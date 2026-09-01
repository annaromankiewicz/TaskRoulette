# Task Roulette

A Spring Boot REST API that manages a personal task backlog and recommends what to do next based on available time and location.

Built as part of the Mobile Computing course at FH Hagenberg.

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 (toolchain) |
| Framework | Spring Boot 4.1 |
| Build tool | Gradle (`build.gradle.kts`) |
| Database (dev) | H2 2.4 (file-based) |
| Database (prod) | PostgreSQL |
| ORM | Spring Data JPA |

---

## How it works

You add tasks to a backlog as they come to mind — "do laundry", "walk the dog", "read chapter 3". Each task has a time weight and a location.

When you have time, the API recommends a random task from your backlog that fits your current constraints (available time, where you are). The recommended task moves to `IN_PROGRESS` and is **reserved** for a limited window: complete it and a reward is drawn, or let the window lapse and it returns to the backlog on its own.

---

## Design decisions

### Task as a one-shot backlog item

`Task` represents a single backlog entry, not a reusable template. Once completed it is marked done and no longer appears in recommendations. This keeps the backlog clean and the recommendation pool relevant.

```
Task (backlog item)
 └── CompletedTask (created on completion, @OneToOne)
      └── Reward (randomly drawn)
```

If tasks were reusable templates, the same "Do laundry" entry would be recommended indefinitely regardless of how often it had been done — not useful for a personal backlog.

### Task lifecycle is one enum, not several booleans

`Task` carries a `State` enum — `BACKLOG` → `IN_PROGRESS` → `DONE` — rather than separate `completed` / `inProgress` flags. Two booleans can express four combinations when only three are legal, and nothing stops `completed=true, inProgress=true` from being written.

The transitions live **on the entity** as methods that refuse illegal moves (a task already `DONE` cannot be completed again), not as public setters. The entity protects its own invariant; the service orchestrates everything that needs a repository.

`@Enumerated(EnumType.STRING)` throughout. The default is `ORDINAL`, which stores the enum's *position* — reordering the constants later would silently change the meaning of every existing row.

### Reservation expiry is derived, not stored

A task is expired when `state == IN_PROGRESS` **and** `activatedAt` is older than the timeout. Nothing stores "expired" and nothing counts down; the answer is computed from two persisted fields.

`activatedAt` is stamped on the transition into `IN_PROGRESS`, not at construction — the clock measures how long the task has been *picked*, not how long it has existed.

`TaskExpiryJob` is a single `@Scheduled` `@Component` holding the repository; it resets whatever has lapsed. Rejected alternative: a per-task timer object held as a field on `Task`. An entity field must map to a column, such an object could only survive as `@Transient`, and it would then exist in memory but not in the table — so a restart, or any task loaded back from the database, would have no timer while still sitting there as `IN_PROGRESS`. Entities are rows, not actors.

The lighter alternative remains open for later: skip the job entirely and put the expiry condition in the pick query, so an expired reservation is simply never considered available.

### Streaks are based on completion history, not task type

The `CompletedTask` table records `completedAt` timestamps and `timeWeight` of each completed task. Streaks and stats are calculated from this history — consecutive days with completions, total weight completed per week, etc. The identity of the task itself is not relevant to streak logic.

### Relationships

```
Task ──────────── CompletedTask ──────── Reward
 1                     0..1        *        1
```

- `Task` → `CompletedTask`: `@OneToOne` — a task is completed at most once
- `CompletedTask` → `Reward`: `@ManyToOne` — many completions can draw the same reward

**`CompletedTask` owns both sides.** It carries `@JoinColumn` for `task_id` and `reward_id`, so it is the only table holding foreign keys; `Task` and `Reward` know nothing about it. Neither inverse side (`mappedBy`) is mapped, because nothing in the app navigates from a task or a reward back to its completions — queries go through `CompletedTaskRepository` instead. Keeping the mapping unidirectional avoids accidental JSON recursion and unnecessary fetches.

Verified in the generated H2 schema: `completed_task` has a `UNIQUE` constraint on `task_id` (enforcing the `@OneToOne` at database level — a second completion of the same task is rejected by the DB, not just by application logic) and a plain, non-unique foreign key on `reward_id`.

### Seeding: reference data only, and idempotent

The reward pool is seeded by a `CommandLineRunner` that returns early if the table is non-empty. Two reasons for a runner over `data.sql`: it runs after the whole context is up, so it can never race Hibernate's schema generation, and it is ordinary Java, so the guard is trivial.

Rewards are *reference data* — part of the app's setup. Backlog tasks are user activity and are only seeded for convenience in development.

---

## API endpoints

### Tasks

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| GET | `/tasks` | Return all backlog tasks | 200 |
| GET | `/tasks/{id}` | Return one task | 200 / 404 |
| POST | `/tasks` | Add task to backlog | 201 |
| PUT | `/tasks/{id}` | Update a task | 200 / 404 |
| DELETE | `/tasks/{id}` | Remove a task | 204 / 404 |
| POST | `/tasks/pick` | Recommend a random task, reserve it | 200 |
| POST | `/tasks/{id}/complete` | Complete a task, draw reward | 200 / 404 / 409 |

`409 Conflict` on completing a task that is already `DONE` — the request is well-formed, it just clashes with current state, which is what 409 means and 400 does not.

---

## Data model

### Task
| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated |
| title | String | e.g. "Do laundry" |
| timeWeight | TimeWeight | enum, stored as STRING |
| location | Location | e.g. `HOME`, `OUTSIDE` |
| state | State | `BACKLOG`, `IN_PROGRESS`, `DONE` |
| activatedAt | Instant | Set when reserved; null in the backlog |

### Reward
| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated |
| description | String | e.g. "15 min gaming" |

### CompletedTask
| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated |
| completedAt | LocalDateTime | Timestamp of completion |
| task | Task | `@OneToOne`, owns `task_id` (unique) |
| reward | Reward | `@ManyToOne`, owns `reward_id` |

Table names are singular throughout: `task`, `reward`, `completed_task`.

**Open:** `TimeWeight` values differ between this document and the code — reconcile before Week 3.

---

## Running locally

### With H2 (dev)

```bash
./gradlew bootRun
```

H2 console available at `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./taskdb;AUTO_SERVER=TRUE`
- Username / password: whatever `spring.datasource.username` / `password` are set to — see the gotcha below

`AUTO_SERVER=TRUE` lets the running app and an external client (H2 console, IntelliJ Database tool) hold the file open at the same time. Without it, a file-based H2 database allows exactly one process and the second connection fails with *"Database may be already in use"*.

### With PostgreSQL

```bash
docker-compose up -d
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

---

## Gotchas hit so far

Notes for future me, and for anyone else on Spring Boot 4.

### The H2 console needs its own dependency

Boot 4 split the monolithic `spring-boot-autoconfigure` into per-technology modules. The H2 console auto-configuration moved out, so `com.h2database:h2` on the classpath is no longer enough — `/h2-console` just 404s, with no error at startup.

```kotlin
implementation("org.springframework.boot:spring-boot-h2console")
```

The IDE's *"cannot resolve configuration property `spring.h2.console.path`"* warning is the same cause: the property metadata ships in that module.

**General rule for Boot 4:** if a feature that is correctly configured simply does not happen, suspect a missing `spring-boot-*` module before suspecting the config.

### `sa` is not the default username for a file-based H2 URL

Spring Boot only supplies the default `sa` / empty-password credentials when it classifies the datasource as *embedded*, and an explicit `jdbc:h2:file:...` URL is not classified that way. With no `spring.datasource.username` set, the app connects with an **empty** username and H2 creates the database file owned by that user — after which logging into the console as `sa` fails with *"Wrong user name or password"*.

Fix: set the credentials explicitly, then delete the database file so it is recreated with the right owner (changing the property does not rename the existing owner).

```properties
spring.datasource.username=sa
spring.datasource.password=
```

### `ddl-auto=update` never drops anything

Renaming a table or field leaves the old one behind and Hibernate creates the new one alongside it. In dev the fix is to stop the app, delete `taskdb.mv.db`, and restart for a clean rebuild.

Values worth knowing: `update` (adds only), `create-drop` (rebuild each run), `validate` (fails startup on mismatch — the right choice for production), `none` (schema managed by Flyway/Liquibase).

### An entity loaded in a transaction saves itself

Hibernate keeps a snapshot of every managed entity as it was at load time. At flush it compares the current fields against that snapshot and issues an `UPDATE` for each difference. So a setter called on an entity returned by a repository **inside** a `@Transactional` method persists with no `save()` call anywhere.

The corollary is the dangerous half: outside a transaction the entity is *detached*, nothing is tracking it, and the same setter changes a Java object that will never be written. No exception, no log line, no row.

Verified deliberately in `TaskExpiryJob`: with `@Transactional`, the log line is followed by an `UPDATE` and appears once. Without it, the log line repeats every scan and no `UPDATE` ever follows — the job keeps finding the same task and keeps throwing the change away.

Two related traps, same root cause:
- `@Transactional` is applied by a proxy, so calling an annotated method **from inside the same class** bypasses it entirely and no transaction is opened. Same for `private` methods.
- Use `org.springframework.transaction.annotation.Transactional`, not the Jakarta one — the Spring annotation supports propagation, isolation, `readOnly` and rollback rules.

The generated `UPDATE` writes *every* column, not just the changed ones: Hibernate builds one static statement per entity at startup and reuses it. `@DynamicUpdate` changes that, at a cost.

### `@SpringBootApplication` is already a `@Configuration` class

It is composed of `@SpringBootConfiguration` (itself meta-annotated `@Configuration`), `@EnableAutoConfiguration` and `@ComponentScan`. So a `@Bean` method placed in the application class runs in **full** mode with `proxyBeanMethods = true` — the CGLIB subclass that makes a direct call to another `@Bean` method return the existing singleton instead of building a second one. Lite mode requires a class that is *not* `@Configuration`, or an explicit `@Configuration(proxyBeanMethods = false)`.

Consequence worth remembering: `@Configuration` classes and their `@Bean` methods cannot be `final`, because the proxy has to override them.

### `data.sql` runs *before* Hibernate creates the schema

`spring.jpa.defer-datasource-initialization` defaults to `false`, so script initialization happens before the JPA `EntityManagerFactory` exists. With the schema generated from entities rather than a `schema.sql`, an `INSERT` in `data.sql` hits tables that do not exist yet and startup fails. Set the property to `true`, or seed from a `CommandLineRunner`, which runs after the context is fully up and sidesteps the ordering completely.

### `Duration.between(start, end)` is directional

`Duration.between(Instant.now(), pastInstant)` is negative and every comparison against a timeout silently fails. Argument order is `(start, end)`; the readable alternative is `activatedAt.isBefore(Instant.now().minus(TIMEOUT))`. Storing the timeout as a `Duration` constant rather than a `long` of unstated units removes a second class of bug.

### `@Scheduled` needs a bean, and `@EnableScheduling`

Scheduling only applies to Spring-managed beans, so a class instantiated with `new` never fires — no proxy, no scheduler, no `@Transactional` either. `fixedRate`/`fixedDelay` are milliseconds unless `timeUnit` is set; `fixedRate` measures start-to-start, `fixedDelay` end-to-start, and the latter is safer when the job's duration grows with the data.

### Useful for inspecting the generated schema

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

```sql
SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'COMPLETED_TASK';
```

Hibernate derives constraint names from a hash of the table and column, so a changed FK name after a rename is confirmation that the constraint was rebuilt against the new target.

---

## Project structure

Target layout (classes are currently flat in `taskroulette/` — to be split before Week 3):

```
src/main/java/at/fhooe/sail/mc/taskroulette/
├── controller/       # HTTP layer — routes and responses
├── service/          # Business logic — pick, complete, validate
├── repository/       # JPA interfaces — database access
├── entity/           # @Entity classes — Task, Reward, CompletedTask
└── dto/              # Request/Response records — never expose entities directly
```

---

## Progress

- [x] W1 — Spring Boot foundations + in-memory CRUD
- [x] W2 · Days 8–9 — JPA + H2 persistence
- [x] W2 · Day 10 — Reward + CompletedTask entities, relationships verified in schema
- [x] W2 · Day 11 — Seeded reward pool, `State` enum + reservation expiry job, dirty checking verified
- [ ] W2 · Day 11 — *outstanding:* persist a completion (`POST /tasks/{id}/complete`), lazy fetch types on `CompletedTask`
- [ ] W2 · Days 12–14 — PostgreSQL + docker-compose + JPQL filter query
- [ ] W3 — Service layer, DTOs, pick + complete logic
- [ ] W4 — Tests, CI, Swagger docs

### Backlog / decided but not built

- `TimeWeight` naming mismatch between README and code
- `location` is not set by the `Task` constructor — seeded tasks have `null`, which will not match any location filter
- `getId()` returns `long` while the field is `Long` — NPE on an unsaved task
- Expiry timeout and scan interval belong in `application.properties`, not as constants
- Two clients can pick the same task concurrently — `@Version` / optimistic locking is the answer when it matters
- Inject a `Clock` instead of calling `Instant.now()` directly, so expiry is testable without waiting
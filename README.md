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

You add tasks to a backlog as they come to mind — "do laundry", "walk the dog", "read chapter 3". Each task has a time weight (`LIGHT`, `MEDIUM`, `HEAVY`) and a location.

When you have time, the API recommends a random task from your backlog that fits your current constraints (available time, where you are). Once completed, a reward is drawn and the completion is recorded with a timestamp.

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
| POST | `/tasks/pick` | Recommend a random task | 200 |
| POST | `/tasks/{id}/complete` | Complete a task, draw reward | 200 |

---

## Data model

### Task
| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated |
| title | String | e.g. "Do laundry" |
| timeWeight | TimeWeight | `LIGHT`, `MEDIUM`, `HEAVY` |
| location | Location | e.g. `HOME`, `OUTSIDE` |
| completed | boolean | False until completed |

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
- [ ] W2 · Day 11 — Seed reward pool, persist a completion, fetch types
- [ ] W2 · Days 12–14 — PostgreSQL + docker-compose + JPQL filter query
- [ ] W3 — Service layer, DTOs, pick + complete logic
- [ ] W4 — Tests, CI, Swagger docs
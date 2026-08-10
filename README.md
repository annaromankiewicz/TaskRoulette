# Task Roulette API

A REST API for managing tasks built with Spring Boot 3 and Java 21.
Part of the FH Hagenberg Mobile Computing course project.

## Tech Stack

- Java 21
- Spring Boot 3
- Maven

## Running the project

1. Clone the repository
2. Open in IntelliJ IDEA
3. Run `TaskRouletteApplication.java`
4. API is available at `http://localhost:8080`

## Endpoints

| Method | Endpoint        | Description            | Success | Error |
|--------|-----------------|------------------------|---------|-------|
| GET    | /tasks          | Get all tasks          | 200     | —     |
| GET    | /tasks/{id}     | Get task by ID         | 200     | 404   |
| POST   | /tasks          | Create a new task      | 201     | 400   |
| PUT    | /tasks/{id}     | Update an existing task| 200     | 404   |
| DELETE | /tasks/{id}     | Delete a task          | 204     | 404   |

## Task model

```json
{
  "id": 1,
  "title": "Study Spring Boot",
  "timeWeight": "SHORT",
  "location": "HOME"
}
```

`timeWeight` values: `SHORT` `MEDIUM` `LONG`  
`location` values: `HOME` `OUTSIDE` `ANYWHERE`

## Testing

Import `requests.http` into IntelliJ IDEA and run requests top to bottom.

## Notes

Data is stored in-memory and resets on restart.
PostgreSQL persistence coming in Week 2.
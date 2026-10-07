# be-interview-prep

Backend interview preparation: five progressively larger Spring Boot exercises (Q1–Q5), each delivered as its own branch, pull request and merge into `main`.

## Tech stack

- Java 21
- Spring Boot 3.5 (Web, Validation)
- Maven (via the wrapper — no global install needed)
- JUnit 5, AssertJ, Mockito
- Spotless with google-java-format

## Getting started

### Database

The app uses a locally installed PostgreSQL (no Docker). Create the database once:

```sql
CREATE DATABASE be_interview_prep;
```

Connection settings come from environment variables:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/be_interview_prep` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | *(empty)* |

Flyway creates the schema on startup. Tests use an in-memory H2 database in PostgreSQL mode, so they need no database.

### Commands

```bash
./mvnw -B test                                   # unit and slice tests
./mvnw -B verify                                 # + integration tests (*IT)
DB_PASSWORD=<your-password> ./mvnw spring-boot:run   # http://localhost:8080
./mvnw -q spotless:apply                         # format code
```

On Windows PowerShell: `$env:DB_PASSWORD="<your-password>"; .\mvnw.cmd spring-boot:run`.

## Task API (Q1)

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/api/tasks` | `201` + `Location` | `400` |
| `GET` | `/api/tasks?status=TODO&page=0&size=20` | `200` (paged) | `400` |
| `GET` | `/api/tasks/{id}` | `200` | `404` |
| `PUT` | `/api/tasks/{id}` | `200` | `400`, `404` |
| `DELETE` | `/api/tasks/{id}` | `204` | `404` |

Statuses: `TODO`, `IN_PROGRESS`, `DONE`. Title is required (max 100 chars); due date is optional and cannot be in the past.

```bash
curl -i -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' \
  -d '{"title":"Write report","description":"Quarterly","dueDate":"2030-01-31"}'
curl 'localhost:8080/api/tasks?status=TODO'
```

All errors use RFC 7807 `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/tasks",
  "errors": [{ "field": "title", "message": "Title is required" }]
}
```

## Project layout

```
src/main/java/com/example/beinterviewprep
├── <feature>/
│   ├── api/           REST controllers + request/response records
│   ├── domain/        entities and domain logic
│   ├── service/       business logic and transactions
│   └── persistence/   Spring Data repositories
├── common/            error handling, config, utilities
└── Application.java
```

## Questions

| # | Branch | Topic | PR |
|---|---|---|---|
| Q1 | `feature/q1-task-api` | Task manager API | — |
| Q2 | `feature/q2-url-shortener` | URL shortener | — |
| Q3 | `feature/q3-auth` | Authentication | — |
| Q4 | `feature/q4-product-catalog` | Product catalog | — |
| Q5 | `feature/q5-order-service` | Order service | — |

## Workflow

1. Branch from the latest `main` for each question.
2. Commit in small, meaningful steps.
3. Open a PR into `main` using the [PR template](.github/pull_request_template.md) and review the diff.
4. Merge, then pull `main` before starting the next question.

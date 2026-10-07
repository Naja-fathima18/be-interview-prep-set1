# be-interview-prep

Backend interview preparation: five progressively larger Spring Boot exercises (Q1–Q5), each delivered as its own branch, pull request and merge into `main`.

## Tech stack

- Java 21
- Spring Boot 3.5 (Web, Validation)
- Maven (via the wrapper — no global install needed)
- JUnit 5, AssertJ, Mockito
- Spotless with google-java-format

## Getting started

```bash
./mvnw -B test                 # unit tests
./mvnw -B verify               # unit + integration tests (*IT)
./mvnw spring-boot:run         # start the app on http://localhost:8080
./mvnw -q spotless:apply       # format code
```

On Windows PowerShell use `mvnw.cmd` instead of `./mvnw`.

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
| Q1 | `feature/q1-task-api` | Task API | — |
| Q2 | `feature/q2-url-shortener` | URL shortener | — |
| Q3 | `feature/q3-auth` | Authentication | — |
| Q4 | `feature/q4-product-catalog` | Product catalog | — |
| Q5 | `feature/q5-order-service` | Order service | — |

## Workflow

1. Branch from the latest `main` for each question.
2. Commit in small, meaningful steps.
3. Open a PR into `main` using the [PR template](.github/pull_request_template.md) and review the diff.
4. Merge, then pull `main` before starting the next question.

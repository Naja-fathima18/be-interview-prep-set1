# be-interview-prep

Backend interview preparation: five progressively larger Spring Boot exercises (Q1–Q5), each delivered as its own branch, pull request and merge into `main`.

## Tech stack

- Java 21
- Spring Boot 3.5 (Web, Validation, Data JPA, Security with OAuth2 Resource Server for JWT, Cache, Actuator)
- PostgreSQL + Flyway; Caffeine for in-process caching
- Docker Compose for a local database, Testcontainers for database tests
- Maven (via the wrapper — no global install needed)
- JUnit 5, AssertJ, Mockito
- Spotless with google-java-format

## Getting started

### Database

The app needs PostgreSQL. The quickest way is Docker:

```bash
docker compose up -d                                   # Postgres 16 on localhost:5433
JWT_SECRET=<32+ byte secret> ./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

The `docker` profile points the app at that container (`postgres`/`postgres`). To use a locally installed PostgreSQL instead, create the database once:

```sql
CREATE DATABASE be_interview_prep;
```

Connection and security settings come from environment variables:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/be_interview_prep` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | *(empty)* |
| `JWT_SECRET` | **required**, at least 32 bytes; the app refuses to start without it |
| `JWT_TTL` | `PT15M` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | *(unset)*; when both are set, an ADMIN with these credentials is created on startup if missing |

Flyway creates the schema on startup. Most tests use an in-memory H2 database in PostgreSQL mode; repository and product query tests run against a real PostgreSQL through Testcontainers, so **Docker must be running** for `./mvnw test`.

### Commands

```bash
./mvnw -B test                                   # unit and slice tests
./mvnw -B verify                                 # + integration tests (*IT)
DB_PASSWORD=<your-password> JWT_SECRET=<32+ byte secret> ./mvnw spring-boot:run   # http://localhost:8080
./mvnw -q spotless:apply                         # format code
```

On Windows PowerShell: `$env:DB_PASSWORD="<your-password>"; $env:JWT_SECRET="<32+ byte secret>"; .\mvnw.cmd spring-boot:run`.

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
curl -i -X POST localhost:8080/api/tasks -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Write report","description":"Quarterly","dueDate":"2030-01-31"}'
curl 'localhost:8080/api/tasks?status=TODO' -H "Authorization: Bearer $TOKEN"
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

## Authentication & roles (Q3)

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/api/auth/register` (public) | `201` | `400`, `409` |
| `POST` | `/api/auth/login` (public) | `200` + token | `400`, `401` |
| `GET` | `/api/users/me` (any logged-in user) | `200` | `401` |
| `GET` | `/api/users?page=0&size=20` (`ADMIN` only) | `200` (paged) | `401`, `403` |

Passwords are stored as BCrypt hashes (8 characters to 72 bytes). Login returns a stateless JWT bearer token (`{"accessToken": "...", "tokenType": "Bearer", "expiresIn": 900}`) that expires after 15 minutes; there are no server-side sessions. Roles are `USER` and `ADMIN`: registration always creates a `USER`, and an `ADMIN` is bootstrapped from `ADMIN_EMAIL`/`ADMIN_PASSWORD`. Every other endpoint, including `/api/tasks` and `/api/short-urls`, needs `Authorization: Bearer <token>`; only the short-link redirect `GET /{code}`, catalog reads (`GET /api/products/**`) and `/actuator/health` stay public. Missing, invalid or expired tokens get `401` and a wrong role gets `403`, both as problem JSON.

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"correct-horse"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"correct-horse"}' | jq -r .accessToken)
curl localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"
```

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required to access this resource",
  "instance": "/api/users/me"
}
```

## Product catalog (Q4)

A product listing API that stays fast as the catalog grows. A product has a name, category, price, stock, rating and creation time; **100 products are seeded on startup** when the table is empty (`catalog.seed.enabled` / `catalog.seed.count`).

| Method | Path | Access | Success | Errors |
|---|---|---|---|---|
| `GET` | `/api/products` | public | `200` (paged) | `400` |
| `GET` | `/api/products/{id}` | public, **cached** | `200` | `404` |
| `POST` | `/api/products` | `ADMIN` | `201` + `Location` | `400`, `401`, `403` |
| `PUT` | `/api/products/{id}` | `ADMIN` | `200` | `400`, `401`, `403`, `404` |
| `DELETE` | `/api/products/{id}` | `ADMIN` | `204` | `401`, `403`, `404` |

**Listing.** Every filter is optional and they combine freely in one request; all of them become a single SQL query (JPA Specifications):

| Parameter | Meaning |
|---|---|
| `category` | exact match: `ELECTRONICS`, `BOOKS`, `CLOTHING`, `HOME`, `SPORTS`, `TOYS`, `GROCERY`, `BEAUTY` |
| `minPrice`, `maxPrice` | inclusive price range (`minPrice` > `maxPrice` → `400`) |
| `inStock=true` | only products with stock > 0 |
| `q` | case-insensitive "name contains"; `%` and `_` are matched literally |
| `page`, `size`, `sort` | `sort=<field>,asc\|desc` on any field (`name`, `category`, `price`, `stock`, `rating`, `createdAt`, `id`); default `createdAt,desc`, size 20, **size capped at 100** |

The response carries `totalElements` and `totalPages`. `id` is appended to every sort as a tie-breaker so pages never overlap or skip rows. Indexes on `(category, price)`, `price`, `created_at`, `rating` and `name` back the filters and sorts.

```bash
curl 'localhost:8080/api/products?category=ELECTRONICS&minPrice=100&maxPrice=800&inStock=true&q=pro&sort=rating,desc&size=5'
```

```json
{"content":[{"id":97,"name":"Pro Headphones Max 97","category":"ELECTRONICS","price":695.79,"stock":351,"rating":4.3,"createdAt":"2026-09-27T21:18:57.539132Z"}],
 "page":0,"size":5,"totalElements":1,"totalPages":1}
```

**Caching single-product lookups.** `GET /api/products/{id}` goes through a Caffeine cache (Spring Cache, `products`, max 10,000 entries, 10-minute expiry as a memory bound only). The cache holds an immutable record, never a JPA entity. Freshness is guaranteed by invalidation, not by the TTL:

- `PUT` and `DELETE` evict the product's entry through a transaction-aware cache manager, so the eviction happens only **after the change commits**; a reader can never re-cache the old row from before the commit and keep it.
- Lookups load with `sync = true`: an eviction for the same id waits for an in-flight load to finish, so a load that read pre-commit data is removed rather than left behind.
- The cache advice runs outside the transaction, so a cache hit opens no transaction and borrows no connection. Missing products (`404`) are not cached.

**How we know repeated lookups skip the database:**

1. `ProductCachingTest` — five `get(7)` calls hit the repository **once** and open **one** transaction; after an update the next read returns the new data, after a delete it returns `404`, and the entry stays cached until the updating transaction commits. Removing the transaction-aware proxy makes that last test fail.
2. Live, via Actuator (admin token required): after five `GET /api/products/42` requests, the SQL log shows a single `select … where p1_0.id=?` and the metrics show 4 hits and 1 miss:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" 'localhost:8080/actuator/metrics/cache.gets?tag=cache:products&tag=result:hit'
curl -H "Authorization: Bearer $ADMIN_TOKEN" 'localhost:8080/actuator/caches'
```

**Trade-offs.** An in-process cache is right for a single instance; with several instances each node would need cross-node invalidation (Redis or a pub/sub eviction). Leading-wildcard name search can't use a B-tree index; a `pg_trgm` GIN index is the next step at larger scale. Offset paging plus a `COUNT` is fine for this catalog size; keyset paging would serve very deep pages better.

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
| Q3 | `feature/q3-auth` | Authentication & roles | — |
| Q4 | `feature/q4-product-catalog` | Product catalog | — |
| Q5 | `feature/q5-order-service` | Order service | — |

## Workflow

1. Branch from the latest `main` for each question.
2. Commit in small, meaningful steps.
3. Open a PR into `main` using the [PR template](.github/pull_request_template.md) and review the diff.
4. Merge, then pull `main` before starting the next question.

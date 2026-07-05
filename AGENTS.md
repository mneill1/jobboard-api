# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Stack

Spring Boot 3.5 / Java 17 · Maven · MySQL (JPA) · MongoDB · Redis · Elasticsearch · Spring-WS (SOAP)

## Commands

```bash
# Build
./mvnw clean package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ApiApplicationTests

# Run a single test method
./mvnw test -Dtest=ApiApplicationTests#contextLoads

# Start all infrastructure (MySQL, Mongo, Redis, Elasticsearch)
docker compose up -d mysql mongo redis elasticsearch

# Run app locally (after infra is up)
./mvnw spring-boot:run
```

## Architecture — Multi-DB Design (Non-obvious)

Three databases serve distinct purposes:
- **MySQL** (JPA/Hibernate): `Job`, `Company`, `JobStatus` — relational entities, source of truth
- **MongoDB** (`applications` collection): `Application` — document store for job applications; `@Id` is `String`, `jobId` is `Long` (cross-store reference)
- **Elasticsearch** (`jobs` index): `JobDocument` — full-text search mirror; only populated when a job transitions to `ACTIVE` status via `updateStatus()`

**Critical:** Jobs are indexed into Elasticsearch only when status changes to `ACTIVE`. Elasticsearch is NOT populated on initial `createJob()`. The `search()` method retrieves `JobDocument`s then re-fetches each full `JobResponse` via `getById()` (which also hits Redis cache).

## Redis Cache

- Cache name: `jobs` (set in `application.yml` under `spring.cache.cache-names`)
- TTL: 5 minutes (configured in `RedisConfig.java`)
- `@Cacheable(value="jobs", key="#id")` on `getById()`
- `@CacheEvict(value="jobs", key="#id")` on `updateStatus()` and `delete()`
- Redis value serialization uses `GenericJackson2JsonRedisSerializer` with `JavaTimeModule` + polymorphic typing — all cached DTOs must be Jackson-serializable with a default constructor

## SOAP Web Service

- WSDL auto-generated from `src/main/resources/company.xsd`
- Served at `/ws/company.wsdl`; endpoint handles `POST /ws`
- JAXB classes generated at build time into `target/generated-sources/jaxb` under package `com.jobboard.hr`
- Namespace: `http://jobboard.com/hr`
- `HrSoapClient` (self-calls `http://localhost:8080/ws`) is wired via `WebServiceConfig` — not auto-wired via `@RequiredArgsConstructor`

## `JobService` Constructor Injection (Non-standard)

`JobService` uses manual constructor injection (not `@RequiredArgsConstructor`) because `JobSearchRepository` requires `@Lazy` to break a Spring circular-dependency cycle between JPA and Elasticsearch repositories.

## Logging

Every HTTP request receives a short UUID prefix (`requestId`, first 8 chars) injected into MDC by `MdcLoggingFilter`. Log format includes `[%X{requestId:-none}]`. All responses include `X-Request-Id` header.

## Error Handling

- `ResourceNotFoundException extends RuntimeException` → caught by `GlobalExceptionHandler`, returns `400` (not 404) with plain string body
- Validation errors (`MethodArgumentNotValidException`) return `ErrorResponse` record `{status, message}` with aggregated field errors

## Code Style Conventions

- Entities use Lombok `@Data` + `@NoArgsConstructor`; DTOs use `@Data` only
- Controllers use Lombok `@RequiredArgsConstructor`; services manually inject when `@Lazy` is needed
- Entity timestamps (`createdAt`, `updatedAt`) initialized inline with `LocalDateTime.now()`
- `JobStatus` stored as `EnumType.STRING` in MySQL; stored as `.name()` String in Elasticsearch `JobDocument`
- Manual `toResponse()` private method in each service (no MapStruct/ModelMapper)
- `ResponseEntity.status(201).body(...)` for creates; `ResponseEntity.noContent().build()` for deletes

## Infrastructure Notes

- `application.yml` hardcodes `root/root` credentials — override via env vars in Docker Compose
- Elasticsearch must have `xpack.security.enabled=false` (set in `docker-compose.yml`)
- `ddl-auto: update` — schema is managed by Hibernate, not migration scripts

## Frontend (Angular)

Location: `frontend/` — Angular 22, TypeScript 6. All commands **must be run from inside `frontend/`**.

```bash
cd frontend
npm install          # first time only
npm start            # ng serve → http://localhost:4200
npm run build        # production build
npm test             # ng test (Vitest, NOT Karma)
```

### Non-obvious frontend gotchas

- **Test runner is Vitest**, not Karma/Jasmine — `tsconfig.spec.json` uses `vitest/globals`; `ng test` invokes `@angular/build:unit-test`
- **Components are module-based** (not standalone), enforced via `angular.json` schematics — new components/directives/pipes must be declared in `AppModule`, never `standalone: true`
- **`@Service()` is used instead of `@Injectable()`** in [`job.ts`](frontend/src/app/service/job.ts) — Angular 22 experimental alias; keep consistent
- **`job.spec.ts` imports `Job` (not `JobService`)**  — this test currently fails; the correct export name is `JobService`
- **API base URL hardcoded** to `http://localhost:8080/api` in [`job.ts`](frontend/src/app/service/job.ts) — no environment file abstraction
- **CORS tied to port 4200** — if the Angular dev port changes, update [`CorsConfig.java`](src/main/java/com/jobboard/api/config/CorsConfig.java) to match
- **Prettier config**: `printWidth: 100`, `singleQuote: true`, Angular HTML parser for `.html` files (see [`frontend/.prettierrc`](frontend/.prettierrc))

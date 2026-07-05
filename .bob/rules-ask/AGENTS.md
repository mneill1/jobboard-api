# Ask Mode Context (Non-Obvious Only)

## Three Databases, One Application
This project intentionally uses MySQL + MongoDB + Elasticsearch + Redis simultaneously:
- MySQL: relational data (jobs, companies)
- MongoDB: applications (document model)
- Elasticsearch: full-text job search (mirror, not source of truth)
- Redis: read-through cache for job lookups

## SOAP Is Self-Referential
`HrSoapClient` calls `http://localhost:8080/ws` — the app calls its own SOAP endpoint. This is not a client to an external HR system; it's a demonstration of the SOAP client pattern within the same process.

## `target/generated-sources/jaxb`
JAXB classes under `com.jobboard.hr` do not exist in `src/`. They appear after `./mvnw compile`. IDE red-lines on these imports are resolved by a build.

## Swagger UI
Available at `http://localhost:8080/swagger-ui.html` after startup.

## Actuator Endpoints
Exposed: `health`, `info`, `metrics`, `loggers` — available at `/actuator/*`.

## Frontend Structure
- Single-page Angular 22 app in `frontend/` — only feature is a job search/list UI (`App` component)
- Only one service: `JobService` in `frontend/src/app/service/job.ts` — covers search, getAll, list by status/location
- **No routing, no lazy loading, no standalone components** — everything in one `AppModule`
- Test runner is **Vitest** (not Karma) — `job.spec.ts` is broken (imports wrong class name `Job` instead of `JobService`)
- `app.spec.ts` has a stale assertion (`'Hello, frontend'`) that doesn't match the actual template heading (`'Job Board'`)

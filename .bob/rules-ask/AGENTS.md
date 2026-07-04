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

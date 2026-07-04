# Agent Coding Rules (Non-Obvious Only)

## Cross-Store ID Types
`Application` (MongoDB) stores `jobId` as `Long` while its own `@Id` is `String`. Never use `Long` for the application primary key.

## Elasticsearch Indexing Gate
Only index `JobDocument` on transition to `ACTIVE`. Do NOT call `jobSearchRepo.save()` in `createJob()` — the pattern is intentional.

## `@Lazy` on `JobSearchRepository`
`JobService` manually injects `JobSearchRepository` with `@Lazy`. If you add a new service that depends on both JPA and ES repositories, replicate this pattern — `@RequiredArgsConstructor` will cause a circular dependency error at startup.

## JAXB Generated Sources
Classes in `com.jobboard.hr` (e.g. `GetCompanyInfoRequest`) are generated from `src/main/resources/company.xsd` at build time. Do not create or edit them by hand. To add new SOAP operations, edit the XSD and rebuild.

## Redis Cache Constraint
All objects stored in Redis (currently `JobResponse`) must be Jackson-serializable with a default (no-arg) constructor. The `ObjectMapper` in `RedisConfig` uses polymorphic typing — adding non-serializable types to `JobResponse` will break cache deserialization at runtime.

## Service-to-Response Mapping
Use a private `toResponse()` method within the service class (see `JobService`, `ApplicationService`). No mapping library is used; keep consistent.

## Error Response Shape
`ResourceNotFoundException` currently returns a plain `String` body (not the `ErrorResponse` record). If you want structured errors for 404s, change `GlobalExceptionHandler.handleRuntime` to return `ErrorResponse` and set status to 404.

# Plan Mode Architecture Rules (Non-Obvious Only)

## Elasticsearch Is an Eventually-Consistent Mirror
`JobDocument` is written only when status becomes `ACTIVE`. Reads via `/api/jobs/search` resolve against Elasticsearch then re-fetch each hit from MySQL (via `getById()`, which may hit Redis). Stale ES data can return cached MySQL results. Any design that adds real-time ES sync must account for this multi-hop read path.

## No Migration System
`ddl-auto: update` means Hibernate manages schema. There is no Flyway/Liquibase. Column renames or drops cannot be expressed safely — plan schema changes as additions only, or introduce a migration tool before making destructive changes.

## MongoDB ↔ MySQL Reference Integrity
`Application.jobId` (Long) references `Job.id` in MySQL — there is no enforced foreign key. Deleting a job does NOT cascade to its MongoDB applications. Any job deletion feature must manually remove or tombstone related applications.

## Circular Dependency Risk Area
The JPA + Elasticsearch repository combination creates a Spring initialization cycle. The `@Lazy` workaround in `JobService` is load-bearing. Adding new services that aggregate both repository types must replicate this pattern or restructure to an event-driven approach.

## SOAP + REST Coexistence
`MessageDispatcherServlet` is registered on `/ws/*` separately from the standard `DispatcherServlet`. SOAP and REST share the same port. Adding Spring Security must account for both servlet paths.

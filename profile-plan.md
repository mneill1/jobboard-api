# Profile Page Plan

## Overview

Add a `/profile` page that shows role-specific information for the logged-in user and allows editing. This requires backend changes (User↔Company link, Application↔User link, new profile endpoints) and frontend changes (ProfileComponent, updated RegisterComponent, new service methods).

**Profile page content by role:**

| Section | APPLICANT | COMPANY |
|---|---|---|
| Account info | Email, role | Email, role |
| Edit account | Update email / password | Update email / password |
| Company info | — | Company name, industry, size, website |
| Edit company | — | Edit company details inline |
| Job listings | — | List of own posted jobs (title, status, application count) |
| Applications | All submitted applications (job title, status, applied date) | — |

**Scope of changes:**
- Backend: 3 schema additions, 3 new endpoints, 1 updated registration flow
- Frontend: updated `RegisterComponent`, new `ProfileComponent`, new service methods, new route

---

## Sub-Tasks

---

### Sub-Task 1 — Backend: Add `companyId` to `User` and `userId` to `Application`

**Intent**
Establish the two missing relationships that the profile page depends on: User↔Company and Application↔User. Without these the backend cannot serve role-filtered data.

**Expected Outcomes**
- `User` entity has a nullable `companyId` (Long) field — populated for `COMPANY` users at registration, null for `APPLICANT` users
- `Application` (MongoDB) has a `userId` (Long) field — populated when an application is submitted by an authenticated user
- `ApplicationRepository` has a `findByUserId(Long userId)` method
- `./mvnw compile` succeeds
- Hibernate applies the `companyId` column addition to `users` table via `ddl-auto: update` on next boot (no migration needed)

**Todo List**
- [x] Add `private Long companyId;` to `User.java` — plain `Long`, no `@ManyToOne` (just a foreign key value, consistent with the `Application.jobId` pattern in this codebase)
- [x] Add `private Long userId;` to `Application.java` — nullable, so existing MongoDB documents without it aren't broken
- [x] Add `List<Application> findByUserId(Long userId)` to `ApplicationRepository`
- [x] Run `./mvnw compile` to confirm clean

**Relevant Context**
- `src/main/java/com/jobboard/api/entity/User.java` — add `companyId` alongside existing fields
- `src/main/java/com/jobboard/api/entity/Application.java` — MongoDB `@Document`, add `userId` as a plain `Long` (nullable, no index needed yet)
- `src/main/java/com/jobboard/api/repository/ApplicationRepository.java` — existing `findByJobId(Long)` pattern to follow
- `ddl-auto: update` means Hibernate adds the column automatically — no Flyway/migration file needed (see AGENTS.md)
- `Application.jobId` is already a `Long` cross-store reference — `userId` follows the same pattern

**Status:** [x] done

---

### Sub-Task 2 — Backend: Update registration to accept company fields and link companyId to User

**Intent**
When a COMPANY user registers, they should supply company details. The backend creates both the `Company` record and the `User` record in the same transaction, linking them via `companyId`.

**Expected Outcomes**
- `RegisterRequest` DTO has optional company fields: `companyName`, `companyIndustry`, `companySize`, `companyWebsite`
- `AuthService.register()` creates a `Company` record when `role == COMPANY`, then sets `user.companyId` to the new company's id before saving
- `APPLICANT` registration is unchanged (company fields ignored)
- `POST /api/auth/register` continues to return `AuthResponse { token, role }`

**Todo List**
- [ ] Add optional fields to `RegisterRequest`: `companyName` (String), `companyIndustry` (String), `companySize` (String), `companyWebsite` (String)
- [ ] Inject `CompanyRepository` into `AuthService`
- [ ] In `AuthService.register()`: if `req.getRole() == UserRole.COMPANY`, create and save a `Company` from the request fields, then set `user.setCompanyId(company.getId())` before saving the user
- [ ] Run `./mvnw compile` to confirm clean

**Relevant Context**
- `src/main/java/com/jobboard/api/dto/RegisterRequest.java` — add new fields; all optional (no `@NotBlank`) since APPLICANT users don't provide them
- `src/main/java/com/jobboard/api/service/AuthService.java` — update `register()` method
- `src/main/java/com/jobboard/api/entity/Company.java` — existing entity; follow the same construction pattern used in `CompanyService.create()`
- `CompanyRepository` is already a `JpaRepository` — inject it alongside `UserRepository`

**Status:** [x] done

---

### Sub-Task 3 — Backend: Update application submission to store userId

**Intent**
When an authenticated user submits a job application, their userId should be stored on the `Application` document so the profile page can retrieve their own applications.

**Expected Outcomes**
- `POST /api/jobs/{id}/apply` sets `application.userId` from the JWT principal
- Existing applications without a `userId` field remain valid (MongoDB schema-less, field is nullable)
- `./mvnw compile` succeeds

**Todo List**
- [ ] Update `ApplicationService.apply()` to accept the authenticated `User` (or just the userId) and set it on the `Application`
- [ ] Update `JobController`'s `POST /{id}/apply` to extract the current user from `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` and pass the userId to the service
- [ ] Run `./mvnw compile`

**Relevant Context**
- `src/main/java/com/jobboard/api/service/ApplicationService.java` — `apply(Long jobId, ApplicationRequest req)` signature needs a third param, or pass the full `User` object
- `src/main/java/com/jobboard/api/controller/JobController.java` — principal is accessible via `@AuthenticationPrincipal User user` on the method parameter (Spring Security injects it automatically)
- The `User` entity implements `UserDetails` so `@AuthenticationPrincipal` resolves to the `User` object directly

**Status:** [x] done

---

### Sub-Task 4 — Backend: `GET /api/users/me` and profile endpoints

**Intent**
Expose the profile data the frontend needs: current user info, their applications (APPLICANT), and their company + job listings with application counts (COMPANY).

**Expected Outcomes**
- `GET /api/users/me` returns a `UserProfileResponse` containing: `id`, `email`, `role`, `companyId` (nullable), and for COMPANY users — the linked company object (`CompanyResponse`)
- `GET /api/users/me/applications` returns `List<ApplicationResponse>` for the authenticated user (APPLICANT use case) — uses `applicationRepository.findByUserId()`
- `GET /api/users/me/jobs` returns `List<JobResponse>` for jobs belonging to the user's company (COMPANY use case) — uses `jobRepository.findByCompanyId()`
- `PUT /api/users/me` allows updating `email` and `password`
- All four endpoints require authentication (`/api/users/**` added to SecurityConfig as authenticated-required)

**Todo List**
- [ ] Create `UserProfileResponse` DTO: `id`, `email`, `role` (String), `companyId` (Long nullable), `company` (CompanyResponse nullable)
- [ ] Add `findByCompanyId(Long companyId): List<Job>` to `JobRepository`
- [ ] Create `UpdateProfileRequest` DTO: `email` (String, nullable), `password` (String, nullable)
- [ ] Create `UserService` with methods: `getProfile(User)`, `getApplications(User)`, `getJobs(User)`, `updateProfile(User, UpdateProfileRequest)`
- [ ] Create `UserController` at `/api/users` with:
  - `GET /me` — returns `UserProfileResponse`, status 200
  - `GET /me/applications` — returns `List<ApplicationResponse>`, status 200
  - `GET /me/jobs` — returns `List<JobResponse>`, status 200
  - `PUT /me` — returns updated `UserProfileResponse`, status 200
- [ ] Update `SecurityConfig` to permit `/api/users/**` for any authenticated user
- [ ] Run `./mvnw test` to confirm passing

**Relevant Context**
- Use `@AuthenticationPrincipal User user` on each controller method to get the current user
- `UserService` should follow the same `@Service @RequiredArgsConstructor toResponse()` pattern as other services
- `CompanyResponse` DTO already exists — reuse it in `UserProfileResponse`
- `updateProfile`: if email is provided, check it isn't already taken by another user; if password is provided, re-encode with `BCryptPasswordEncoder`
- `BCryptPasswordEncoder` is a `@Bean` in `SecurityConfig` — inject it into `UserService`

**Status:** [x] done

---

### Sub-Task 5 — Frontend: Update RegisterComponent for company fields

**Intent**
When a user selects `COMPANY` as their role on the register page, additional fields should appear for company details. These are submitted together to the updated `POST /api/auth/register` endpoint.

**Expected Outcomes**
- Register form shows `companyName`, `companyIndustry`, `companySize`, `companyWebsite` inputs when role = `COMPANY`
- These fields are hidden when role = `APPLICANT`
- `AuthService.register()` passes all fields through to the backend
- `AuthRequest` interface updated to include optional company fields

**Todo List**
- [ ] Add optional fields to `AuthRequest` interface in `auth.ts`: `companyName?`, `companyIndustry?`, `companySize?`, `companyWebsite?`
- [ ] Update `AuthService.register()` signature to accept the new optional fields and pass them in the request body
- [ ] In `register.component.ts`: add state for `companyName`, `companyIndustry`, `companySize`, `companyWebsite`
- [ ] In `register.component.html`: add a `*ngIf="role === 'COMPANY'"` block with four labelled inputs for the company fields, styled consistently with existing form inputs
- [ ] Run `npx tsc --noEmit` from `frontend/`

**Relevant Context**
- `frontend/src/app/register/register.component.ts` and `.html` — add fields below the role select
- `frontend/src/app/service/auth.ts` — update interface and method signature
- Inline styles only — match existing form inputs (same padding, border-radius, font-size as login/register form)

**Status:** [x] done

---

### Sub-Task 6 — Frontend: Add profile service methods and ProfileComponent

**Intent**
Create the `ProfileComponent` at `/profile` with role-aware content: account info + edit form for both roles; company details + job listings for COMPANY; application history for APPLICANT.

**Expected Outcomes**
- `GET /api/users/me`, `GET /api/users/me/applications`, `GET /api/users/me/jobs`, `PUT /api/users/me` wired into service methods
- `ProfileComponent` declared in `AppModule`, route `/profile` added with `AuthGuard`
- APPLICANT view: email, role, joined date, edit email/password form, list of submitted applications (job title, status, applied date)
- COMPANY view: email, role, joined date, edit email/password form, company details section (name, industry, size, website) with inline edit, list of posted jobs (title, status)
- Nav bar has a "Profile" link visible when logged in

**Todo List**
- [ ] Add `UserProfileResponse`, `UpdateProfileRequest` interfaces to a new `frontend/src/app/service/user.ts`
- [ ] Add `UserService` to `user.ts` with methods: `getProfile()`, `getApplications()`, `getJobs()`, `updateProfile(req)` — using `@Service()` + `inject(HttpClient)` pattern
- [ ] Create `frontend/src/app/profile/profile.component.ts` — inject `UserService`, `AuthService`; on `ngOnInit` call `getProfile()` and (conditionally) `getApplications()` or `getJobs()`; edit form state for account + company
- [ ] Create `frontend/src/app/profile/profile.component.html` — shared account section at top; role-specific section below; edit forms toggle on "Edit" button click; inline styles matching existing UI
- [ ] Add `/profile` route (with `AuthGuard`) to `app-routing.module.ts`
- [ ] Declare `ProfileComponent` in `AppModule`
- [ ] Add `Profile` `routerLink` to the nav in `app.html` — visible only when `authService.isLoggedIn()`
- [ ] Run `npx tsc --noEmit` from `frontend/`

**Relevant Context**
- Follow `@Service()` + `inject(HttpClient)` pattern in `service/job.ts`
- Module-based component: `standalone: false`, declared in `AppModule`
- `AuthGuard` already exists at `frontend/src/app/auth.guard.ts` — reuse it on the `/profile` route
- Inline styles only — match existing font-family, border-radius, colour palette
- `UserProfileResponse` should mirror the backend DTO shape: `{ id, email, role, companyId, company: { id, name, industry, size, website } }`

**Status:** [x] done

---

## Implementation Order

Sub-tasks must be completed in order: **1 → 2 → 3 → 4 → 5 → 6**

- Sub-tasks 1–4 are backend. The app remains functional throughout (all additions are additive).
- Sub-tasks 5–6 are frontend and depend on the new backend endpoints from Sub-Task 4.
- Sub-task 5 (register company fields) can be tested independently once Sub-Task 2 is done.

## Key Design Decisions

- `User.companyId` is a plain `Long` (not a `@ManyToOne`) — consistent with `Application.jobId` pattern in this codebase
- `Application.userId` is nullable — existing MongoDB documents without the field remain valid
- `BCryptPasswordEncoder` injected from `SecurityConfig` bean (not instantiated with `new`) to avoid circular dependency

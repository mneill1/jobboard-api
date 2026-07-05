# Auth System Plan

## Overview

Add a JWT-based authentication system with two user roles: `APPLICANT` and `COMPANY`. The backend gets a `User` entity, Spring Security with a JWT filter, and protected endpoints. The Angular frontend gets Angular Router, login/register pages, an `AuthService`, and an HTTP interceptor that attaches the token to every request.

**Access model:**
| Action | APPLICANT | COMPANY | Public (no login) |
|---|---|---|---|
| Browse / search jobs | ✅ | ✅ | ✅ |
| Submit an application | ✅ | ❌ | ❌ |
| Create / edit / delete jobs | ❌ | ✅ | ❌ |
| View applications for a job | ❌ | ✅ (own jobs) | ❌ |
| Create / update / delete companies | ❌ | ✅ | ❌ |
| View company profiles | ✅ | ✅ | ✅ |

**Tech choices:**
- Backend: Spring Security + JJWT library, `User` entity in MySQL, `BCryptPasswordEncoder`
- Frontend: Angular Router, `AuthService` (localStorage), `AuthInterceptor`, `AuthGuard`, login + register components

**Not in scope:**
- Password reset / email verification
- JWT refresh tokens
- Per-company ownership checks (e.g. only the company that owns job X can delete it) — role-level protection only
- Admin role

---

## Sub-Tasks

---

### Sub-Task 1 — Backend: Add dependencies (Spring Security + JJWT)

**Intent**
Spring Security is not on the classpath at all. JJWT is needed to sign and verify JWT tokens. Both must be added before any security code can be written.

**Expected Outcomes**
- `spring-boot-starter-security` is in `pom.xml`
- JJWT API, implementation, and Jackson extension are in `pom.xml`
- `./mvnw compile` succeeds

**Todo List**
- [ ] Add `spring-boot-starter-security` to `pom.xml`
- [ ] Add `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (version `0.12.x`) to `pom.xml`
- [ ] Run `./mvnw compile` to confirm no errors

**Relevant Context**
- `pom.xml` — add inside the existing `<dependencies>` block
- JJWT 0.12.x uses `Jwts.builder()` / `Jwts.parser()` API (different from 0.11.x)
- Adding Spring Security will lock down ALL endpoints by default — `SecurityConfig` in Sub-Task 3 is what re-opens public routes. The app will be broken between Sub-Task 1 and Sub-Task 3.

**Status:** [x] done

---

### Sub-Task 2 — Backend: User entity, repository, and registration/login DTOs

**Intent**
Create the `User` MySQL entity that stores credentials and role, plus the DTOs needed for register and login requests/responses.

**Expected Outcomes**
- `User` JPA entity with fields: `id` (Long), `email` (String, unique), `password` (String, hashed), `role` (enum: `APPLICANT` / `COMPANY`), `createdAt`
- `UserRole` enum: `APPLICANT`, `COMPANY`
- `RegisterRequest` DTO: `email`, `password`, `role`
- `LoginRequest` DTO: `email`, `password`
- `AuthResponse` DTO: `token` (String), `role` (String)
- Hibernate creates the `user` table automatically via `ddl-auto: update`

**Todo List**
- [ ] Create `UserRole` enum in `src/main/java/com/jobboard/api/entity/`
- [ ] Create `User` entity in `src/main/java/com/jobboard/api/entity/` — `@Entity`, `@Data`, `@NoArgsConstructor`, `@Table(name="users")` (avoid reserved word `user`), implement `UserDetails`
- [ ] Create `RegisterRequest`, `LoginRequest`, `AuthResponse` DTOs in `src/main/java/com/jobboard/api/dto/`
- [ ] Create `UserRepository extends JpaRepository<User, Long>` with `findByEmail(String email): Optional<User>` in `src/main/java/com/jobboard/api/repository/`

**Relevant Context**
- Follow existing entity conventions: Lombok `@Data` + `@NoArgsConstructor`, `LocalDateTime.now()` inline for `createdAt`
- `User` must implement `UserDetails` (Spring Security interface) — `getAuthorities()` maps `role` to a `GrantedAuthority`
- Table name must be `users` not `user` — `user` is a reserved word in MySQL
- Follow existing DTO convention: Lombok `@Data` only (no `@NoArgsConstructor`)
- `ddl-auto: update` means no migration needed — Hibernate creates the table on startup

**Status:** [x] done

---

### Sub-Task 3 — Backend: JWT utility, security filter, and SecurityConfig

**Intent**
Implement the JWT signing/verification logic, a filter that extracts and validates the token from the `Authorization` header on every request, and a `SecurityConfig` that defines which endpoints are public and which require a role.

**Expected Outcomes**
- `JwtUtil` service: `generateToken(User)`, `extractEmail(token)`, `isTokenValid(token, userDetails)`
- `JwtAuthFilter extends OncePerRequestFilter`: reads `Authorization: Bearer <token>`, validates it, sets `SecurityContextHolder`
- `SecurityConfig`: permits `GET /api/jobs/**`, `GET /api/companies/**`, `POST /api/auth/**` publicly; requires `COMPANY` for mutating job/company endpoints; requires `APPLICANT` or `COMPANY` for `POST /api/jobs/{id}/apply` and `GET /api/jobs/{id}/applications`
- CORS and SOAP (`/ws/**`) still work after security is applied
- `./mvnw compile` succeeds

**Todo List**
- [x] Create `JwtUtil` in `src/main/java/com/jobboard/api/config/` — use JJWT 0.12.x API, sign with `HS256` and a secret key from `application.yml` (`app.jwt.secret`), set expiry to 24h
- [x] Add `app.jwt.secret` (a long random string) to `application.yml`
- [x] Create `JwtAuthFilter` in `src/main/java/com/jobboard/api/config/` — extends `OncePerRequestFilter`, skip if no `Authorization` header, validate token via `JwtUtil`, load `UserDetails` via `UserRepository`, set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
- [x] Create `SecurityConfig` in `src/main/java/com/jobboard/api/config/` — `@Configuration @EnableWebSecurity`, disable CSRF (stateless JWT), add `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`, define the endpoint rules (see access model table above), preserve existing `CorsConfig` behaviour, permit `/ws/**` for SOAP
- [x] Create `UserDetailsServiceImpl` that loads users by email for Spring Security

**Relevant Context**
- `WebServiceConfig` registers `MessageDispatcherServlet` on `/ws/*` separately — `SecurityConfig` must explicitly permit `/ws/**` or SOAP will break (see AGENTS.md — "SOAP + REST Coexistence")
- Existing `CorsConfig` uses `WebMvcConfigurer` — with Spring Security, CORS must also be enabled via `SecurityConfig.cors()` or the preflight `OPTIONS` requests will be rejected before reaching `CorsConfig`
- `@EnableMethodSecurity` can be added later for ownership checks; for now, role-level rules in the filter chain are sufficient

**Status:** [x] done

---

### Sub-Task 4 — Backend: AuthController (register + login)

**Intent**
Expose `/api/auth/register` and `/api/auth/login` endpoints that create users and issue JWT tokens.

**Expected Outcomes**
- `POST /api/auth/register` accepts `{ email, password, role }`, creates a `User` with BCrypt-hashed password, returns `AuthResponse { token, role }`
- `POST /api/auth/login` accepts `{ email, password }`, verifies credentials, returns `AuthResponse { token, role }`
- Duplicate email on register returns a clear error response (400)
- Wrong password on login returns 401

**Todo List**
- [ ] Create `AuthService` in `src/main/java/com/jobboard/api/service/` with `register(RegisterRequest)` and `login(LoginRequest)` methods
- [ ] `register`: check email not already taken (throw `ResourceNotFoundException` or a new `ConflictException` if it is), encode password with `BCryptPasswordEncoder`, save `User`, generate and return JWT
- [ ] `login`: load user by email, verify password with `BCryptPasswordEncoder.matches()`, throw on mismatch, return JWT
- [ ] Create `AuthController` in `src/main/java/com/jobboard/api/controller/` — `@RestController @RequestMapping("/api/auth")`, `POST /register` returns 201, `POST /login` returns 200
- [ ] Wire `BCryptPasswordEncoder` as a `@Bean` in `SecurityConfig` (avoids circular dependency — don't put it in `AuthService`)

**Relevant Context**
- Follow existing service patterns: `@Service`, `@RequiredArgsConstructor`, `toResponse()` style
- Follow existing controller patterns: `ResponseEntity.status(201).body(...)` for register, `ResponseEntity.ok(...)` for login
- `GlobalExceptionHandler` already handles `RuntimeException` → 400 — login failures should throw a specific exception or return 401 via `ResponseEntity` directly

**Status:** [x] done

---

### Sub-Task 5 — Frontend: AuthService and HTTP interceptor

**Intent**
Create an `AuthService` that manages login/register/logout and persists the JWT to `localStorage`, and an `AuthInterceptor` that attaches `Authorization: Bearer <token>` to every outgoing HTTP request.

**Expected Outcomes**
- `AuthService` in `frontend/src/app/service/auth.ts` with: `login()`, `register()`, `logout()`, `getToken()`, `getRole()`, `isLoggedIn()`, `currentUser$` observable
- JWT stored under key `jobboard_token` in `localStorage`; role stored under `jobboard_role`
- `AuthInterceptor` clones every request and adds the `Authorization` header if a token exists
- `AuthInterceptor` is registered in `AppModule`

**Todo List**
- [ ] Add `AuthRequest { email, password, role? }` and `AuthResponse { token, role }` interfaces to a new `frontend/src/app/service/auth.ts`
- [ ] Implement `AuthService` using `@Service()` + `inject(HttpClient)` pattern — `login()` posts to `POST /api/auth/login`, `register()` posts to `POST /api/auth/register`, `logout()` clears localStorage, `isLoggedIn()` checks for stored token, `getRole()` returns stored role
- [ ] Create `AuthInterceptor` as a class implementing `HttpInterceptor` in `frontend/src/app/`
- [ ] Register `AuthInterceptor` in `AppModule` providers: `{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }`
- [ ] Import `HTTP_INTERCEPTORS` from `@angular/common/http` in `app-module.ts`

**Relevant Context**
- Follow the `@Service()` + `inject(HttpClient)` pattern in `frontend/src/app/service/job.ts` — not `@Injectable()`
- `localStorage` key naming: `jobboard_token`, `jobboard_role` (avoids collisions)
- `HttpInterceptor` is class-based (not functional) to stay consistent with the module-based architecture

**Status:** [x] done

---

### Sub-Task 6 — Frontend: Add Angular Router, Login and Register components

**Intent**
Wire Angular Router into the app and create login and register page components. The job board home (`/`) stays public. `/login` and `/register` are accessible without auth. An `AuthGuard` protects routes that require login (applying to jobs, posting jobs).

**Expected Outcomes**
- `RouterModule` configured with routes: `/` (App / job board), `/login` (LoginComponent), `/register` (RegisterComponent)
- `LoginComponent` and `RegisterComponent` as module-based components with inline-style forms consistent with the existing UI
- `AuthGuard` (`CanActivate`) that redirects to `/login` if the user is not logged in
- `AppModule` imports `RouterModule.forRoot(routes)`
- `app.html` has a `<router-outlet>` replacing the current main view content, OR the existing `App` component is kept as the home route component

**Todo List**
- [ ] Create `frontend/src/app/auth.guard.ts` — `CanActivateFn` (or class-based guard) using `AuthService.isLoggedIn()`, redirects to `/login` via `Router` if not authenticated
- [ ] Create `frontend/src/app/login/login.component.ts` and `login.component.html` — form with email + password fields, calls `authService.login()`, navigates to `/` on success, shows error on failure
- [ ] Create `frontend/src/app/register/register.component.ts` and `register.component.html` — form with email, password, and role dropdown (`APPLICANT` / `COMPANY`), calls `authService.register()`, navigates to `/login` on success
- [ ] Create `frontend/src/app/app-routing.module.ts` — defines routes array, imports `RouterModule.forRoot(routes)`, exports `RouterModule`
- [ ] Import `AppRoutingModule` in `AppModule`
- [ ] Declare `LoginComponent` and `RegisterComponent` in `AppModule`
- [ ] Add `<router-outlet>` to the root `app.html` (the existing job board content moves to the `/` route component — `App` itself stays as the home page component)
- [ ] Add a nav bar to `app.html` with Login / Register links when logged out, and a Logout button when logged in — bound to `authService.isLoggedIn()` and `authService.logout()`

**Relevant Context**
- Existing `App` component becomes the home route (`/`) — no need to create a separate home component
- All new components must be declared in `AppModule` (not standalone) — enforced by `angular.json` schematics
- Use `@Component({ standalone: false })` on new components explicitly to be safe
- Use `RouterModule` (not `provideRouter`) to stay consistent with the module-based architecture
- Inline styles only — match existing font-family, border-radius, colour palette

**Status:** [x] done

---

### Sub-Task 7 — Frontend: Conditionally show/hide UI based on role

**Intent**
The "Post a Job" button and form should only be visible to `COMPANY` users. The "Apply" button (once it exists) should only be visible to `APPLICANT` users. Show a logged-in user indicator in the nav.

**Expected Outcomes**
- "Post a Job" button is hidden from `APPLICANT` users and anonymous users — only `COMPANY` users see it
- Nav bar shows the current user's email and role when logged in
- `AuthService` is injected into `App` component to drive these conditional displays

**Todo List**
- [ ] Inject `AuthService` into `App` component constructor
- [ ] Wrap the "Post a Job" button in `app.html` with `*ngIf="authService.getRole() === 'COMPANY'"`
- [ ] Add logged-in user info display to the nav area (email + role label + Logout button) using `*ngIf="authService.isLoggedIn()"`
- [ ] Add Login / Register links when `!authService.isLoggedIn()`

**Relevant Context**
- `AuthService` must be made `public` in the constructor or accessed via a getter for template binding
- This sub-task depends on Sub-Task 6 (AuthService must exist)

**Status:** [x] done

---

## Implementation Order

Sub-tasks must be completed in order: **1 → 2 → 3 → 4 → 5 → 6 → 7**

- Sub-tasks 1–4 are backend-only; the app will be broken between Sub-Task 1 (security locks everything) and Sub-Task 3 (SecurityConfig re-opens public routes)
- Sub-tasks 5–7 are frontend-only and depend on the backend auth endpoints being in place
- Sub-task 7 depends on Sub-task 6 (AuthService)

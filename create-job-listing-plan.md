# Create Job Listing Feature — Plan

## Overview

Add a "Post a Job" feature to the Angular frontend that allows a user to create a new job listing via an inline toggle form. The form collects all job fields, submits to the existing `POST /api/jobs` backend endpoint, then collapses and refreshes the job list on success.

**Scope:**
- Frontend only: new form UI, two new service methods (`createJob`, `getCompanies`), new `CompanyResponse` interface
- No backend changes required — `POST /api/jobs` already exists and is fully functional
- Newly created jobs will appear in "All jobs" (status: `DRAFT`), but NOT in search results (Elasticsearch only indexes `ACTIVE` jobs)

**Not in scope:**
- Status management (setting a job to ACTIVE)
- Form validation beyond what the browser/API already enforces
- Routing or a separate page

---

## Sub-Tasks

---

### Sub-Task 1 — Add `CompanyResponse` interface and service methods to `job.ts`

**Intent**
The form needs to populate a company dropdown and submit a new job. Both require new HTTP calls that don't exist in the service yet.

**Expected Outcomes**
- `JobService` exposes a `getCompanies()` method that fetches `GET /api/companies`
- `JobService` exposes a `createJob(req)` method that posts to `POST /api/jobs`
- A `CompanyResponse` interface (id, name) is exported from `job.ts` for use in the component
- A `JobRequest` interface (title, description, location, salaryMin, salaryMax, companyId) is exported for typing the form model

**Todo List**
- [ ] Add `CompanyResponse` interface to `frontend/src/app/service/job.ts` with fields: `id: number`, `name: string`
- [ ] Add `JobRequest` interface to `frontend/src/app/service/job.ts` with fields: `title: string`, `description: string`, `location: string`, `salaryMin: number | null`, `salaryMax: number | null`, `companyId: number | null`
- [ ] Add `getCompanies(): Observable<CompanyResponse[]>` method — `GET /api/companies`
- [ ] Add `createJob(req: JobRequest): Observable<JobResponse>` method — `POST /api/jobs` with JSON body

**Relevant Context**
- `frontend/src/app/service/job.ts` — add alongside existing methods; follow the `@Service()` + `inject(HttpClient)` pattern already in use
- Backend endpoint: `POST /api/jobs`, expects `{ title, description, location, salaryMin, salaryMax, companyId }`, returns `JobResponse` with status 201
- Companies endpoint: `GET /api/companies`, returns array of `{ id, name, industry, size, website, createdAt }`

**Status:** [x] done

---

### Sub-Task 2 — Add form state and logic to `App` component (`app.ts`)

**Intent**
The component needs state to track whether the form is open, the form field values, the list of companies for the dropdown, and submission/error state. It also needs methods to open/close the form, load companies, and submit.

**Expected Outcomes**
- Component has a boolean `showForm` flag (false by default)
- Component has a `newJob` object typed as `JobRequest` with all fields initialised to empty/null
- Component has a `companies` array typed as `CompanyResponse[]`
- Component has a `formError` string (null when no error)
- `toggleForm()` method: flips `showForm`; fetches companies (via `getCompanies()`) the first time it opens (only if `companies` is empty)
- `submitJob()` method: calls `jobService.createJob(newJob)`, on success resets the form + calls `loadAll()`, on error sets `formError`

**Todo List**
- [x] Import `CompanyResponse` and `JobRequest` from `./service/job` in `app.ts`
- [x] Add `showForm = false`, `formError: string | null = null`, `companies: CompanyResponse[] = []` properties
- [x] Add `newJob: JobRequest` property initialised with all fields empty/null
- [x] Add `toggleForm()` method — toggles `showForm`; calls `jobService.getCompanies()` and populates `companies` only when `companies.length === 0`
- [x] Add `submitJob()` method — calls `jobService.createJob(this.newJob)`, on success: resets `newJob`, sets `showForm = false`, calls `loadAll()`; on error: sets `formError` to the error message

**Relevant Context**
- `frontend/src/app/app.ts` — follow the same `{ next, error }` subscribe pattern used in `search()` and `loadAll()`
- `loadAll()` already exists and refreshes `this.jobs` — call it after successful creation
- Keep the `loading` flag for the list separate from the form submission state (form doesn't need a spinner)

**Status:** [x] done

---

### Sub-Task 3 — Add the form UI to `app.html`

**Intent**
Add a "Post a Job" toggle button and the inline form to the template, following the existing inline-style CSS conventions of the project.

**Expected Outcomes**
- A "Post a Job" button sits alongside the existing Search and "All jobs" buttons (or in a logical position above/below the search bar)
- Clicking the button shows/hides a form section using `*ngIf="showForm"`
- The form contains: title (text), description (textarea), location (text), salary min (number), salary max (number), company (select dropdown bound to `newJob.companyId`)
- The company `<select>` is populated via `*ngFor` over `companies`
- All fields use `[(ngModel)]` two-way binding to `newJob.*`
- A "Submit" button calls `submitJob()`; a "Cancel" button calls `toggleForm()`
- An inline error message is shown when `formError` is set
- All styling uses inline styles consistent with the rest of the template (no new CSS classes)

**Todo List**
- [ ] Add a "Post a Job" `<button>` near the top of `app.html`; bind `(click)="toggleForm()"` 
- [ ] Add a `<div *ngIf="showForm">` section below the button row
- [ ] Inside the form div, add a labelled `<input [(ngModel)]="newJob.title">` for title
- [ ] Add a `<textarea [(ngModel)]="newJob.description">` for description
- [ ] Add an `<input [(ngModel)]="newJob.location">` for location
- [ ] Add `<input type="number" [(ngModel)]="newJob.salaryMin">` and `salaryMax` inputs
- [ ] Add a `<select [(ngModel)]="newJob.companyId">` with `<option *ngFor="let c of companies" [value]="c.id">{{ c.name }}</option>`
- [ ] Add a submit `<button (click)="submitJob()">Post Job</button>` and a cancel `<button (click)="toggleForm()">Cancel</button>`
- [ ] Add an error display: `<div *ngIf="formError">{{ formError }}</div>` styled in red
- [ ] Match inline style conventions: same font-family, border-radius, padding, and colour palette as existing elements

**Relevant Context**
- `frontend/src/app/app.html` — all existing styling is inline; no CSS file or utility classes used
- `FormsModule` is already imported in `AppModule` — `[(ngModel)]` and `*ngFor` / `*ngIf` will work without any module changes
- `companyId` on the `<select>` must bind as a number — use `[value]="c.id"` (not `value="{{c.id}}"`) to preserve the numeric type for the API call

**Status:** [x] done

---

## Implementation Order

Sub-tasks must be completed in order: **1 → 2 → 3**.  
Sub-task 2 depends on the interfaces and methods from Sub-task 1.  
Sub-task 3 depends on the component properties and methods from Sub-task 2.

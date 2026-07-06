# Location City Dropdown Plan

## Overview

Replace the free-text location input in the "Post a Job" form with a `<select>` dropdown populated from a hardcoded list of the 100 most populous cities in the world. No backend changes are needed — `location` is already stored and displayed as a plain string everywhere.

---

## Sub-Tasks

### 1. Create a cities data constant

**Intent:** Define the list of cities in one place so it can be imported wherever needed.

**Expected Outcomes:** A new file `frontend/src/app/data/cities.ts` exports a `TOP_100_CITIES: string[]` constant with 100 city strings in the format `"City, Country"`, ordered by population.

**Todo List:**
- Create `frontend/src/app/data/cities.ts` with the exported constant
- Include all 100 cities as `"City, Country"` strings ordered by descending population

**Relevant Context:**
- No existing utility file for static data — new file required
- Pattern: simple exported `const` array, consistent with how `JobStatus` enums are kept close to their use

**Status:** `[ ] pending`

---

### 2. Wire the dropdown into the Post a Job form

**Intent:** Replace the location `<input>` with a `<select>` whose options come from `TOP_100_CITIES`, keeping the same `[(ngModel)]` binding so the rest of the form submission logic is untouched.

**Expected Outcomes:**
- The location field in the "Post a Job" form is a styled `<select>` with 100 city options plus a disabled placeholder option
- Styling is consistent with the other `<select>` fields in the same form (company, status)
- Form submission works exactly as before — `newJob.location` is set to the chosen string

**Todo List:**
- Import `TOP_100_CITIES` in `home.component.ts` and expose it as a component property
- In `home.component.html`, replace the location `<input>` with a `<select>` using `*ngFor` over the cities array
- Apply the same Tailwind classes already used on the `companyId` and `status` selects in the same form

**Relevant Context:**
- [`frontend/src/app/home/home.component.html`](frontend/src/app/home/home.component.html) line 94 — current location `<input>` to replace
- [`frontend/src/app/home/home.component.ts`](frontend/src/app/home/home.component.ts) line 20 — `newJob` initialiser uses `location: ''`, no change needed there
- The other selects in the form use: `class="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent transition"`

**Status:** `[ ] pending`

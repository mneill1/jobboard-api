import { Component, computed, signal } from '@angular/core';
import { JobService, JobResponse, CompanyResponse, JobRequest, JobFilter } from '../service/job';
import { AuthService } from '../service/auth';
import { TOP_100_CITIES } from '../data/cities';
import { CITY_COORDINATES } from '../data/city-coordinates';
import type { CityListing } from '../listings-map/listings-map.component';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: false
})
export class HomeComponent {
  readonly cities = TOP_100_CITIES;

  filters = signal<JobFilter>({});
  draftQuery = '';
  draftLocation = '';
  draftMinSalary: number | null = null;
  draftPostedWithinDays: number | null = null;

  jobs = signal<JobResponse[]>([]);
  loading = signal(false);
  searched = signal(false);
  showFilters = signal(false);

  showForm = false;
  formError = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  companies = signal<CompanyResponse[]>([]);
  newJob: JobRequest = { title: '', description: '', location: '', salaryMin: null, salaryMax: null, companyId: null, status: 'DRAFT' };

  allJobs = signal<JobResponse[]>([]);

  activeFilterCount = computed(() => {
    const f = this.filters();
    let n = 0;
    if (f.query) n++;
    if (f.location) n++;
    if (f.minSalary != null) n++;
    if (f.postedWithinDays != null) n++;
    return n;
  });

  mapData = computed<CityListing[]>(() => {
    const counts = new Map<string, number>();
    for (const j of this.allJobs()) {
      if (!j.location) continue;
      counts.set(j.location, (counts.get(j.location) ?? 0) + 1);
    }
    const out: CityListing[] = [];
    for (const [city, count] of counts) {
      const coords = CITY_COORDINATES[city];
      if (coords) out.push({ city, lat: coords.lat, lon: coords.lon, count });
    }
    return out;
  });

  constructor(private jobService: JobService, public authService: AuthService) {
    this.refreshAllJobs();
    this.applyFilters();
  }

  private refreshAllJobs(): void {
    this.jobService.list().subscribe({
      next: (results) => this.allJobs.set(results),
      error: (err) => console.error('Failed to load jobs for map', err)
    });
  }

  private buildFilter(): JobFilter {
    const f: JobFilter = {};
    if (this.draftQuery.trim()) f.query = this.draftQuery.trim();
    if (this.draftLocation) f.location = this.draftLocation;
    if (this.draftMinSalary != null && !isNaN(this.draftMinSalary)) f.minSalary = this.draftMinSalary;
    if (this.draftPostedWithinDays != null) f.postedWithinDays = this.draftPostedWithinDays;
    return f;
  }

  applyFilters(): void {
    const filter = this.buildFilter();
    this.filters.set(filter);
    this.loading.set(true);
    this.jobService.list(filter).subscribe({
      next: (results) => {
        this.jobs.set(results);
        this.loading.set(false);
        this.searched.set(true);
      },
      error: (err) => {
        console.error('Failed to load jobs', err);
        this.loading.set(false);
        this.searched.set(true);
      }
    });
  }

  clearFilters(): void {
    this.draftQuery = '';
    this.draftLocation = '';
    this.draftMinSalary = null;
    this.draftPostedWithinDays = null;
    this.applyFilters();
  }

  toggleFilters(): void {
    this.showFilters.set(!this.showFilters());
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.applyFilters();
  }

  onCitySelected(city: string): void {
    this.draftLocation = city;
    this.applyFilters();
  }

  clearCityFilter(): void {
    this.draftLocation = '';
    this.applyFilters();
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    this.formError.set(null);
    this.successMessage.set(null);
    if (this.showForm && this.companies().length === 0) {
      this.jobService.getCompanies().subscribe({
        next: (results) => { this.companies.set(results); },
        error: (err) => { console.error('Failed to load companies', err); }
      });
    }
  }

  submitJob(): void {
    this.formError.set(null);
    this.successMessage.set(null);
    this.jobService.createJob(this.newJob).subscribe({
      next: () => {
        this.newJob = { title: '', description: '', location: '', salaryMin: null, salaryMax: null, companyId: null, status: 'DRAFT' };
        this.showForm = false;
        this.successMessage.set('Job created successfully!');
        this.refreshAllJobs();
        this.applyFilters();
      },
      error: (err) => {
        this.formError.set(err?.error?.message ?? err?.message ?? 'Failed to create job');
      }
    });
  }
}

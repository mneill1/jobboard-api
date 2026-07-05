import { ChangeDetectorRef, Component } from '@angular/core';
import { JobService, JobResponse, CompanyResponse, JobRequest } from '../service/job';
import { AuthService } from '../service/auth';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: false
})
export class HomeComponent {
  query = '';
  jobs: JobResponse[] = [];
  loading = false;
  searched = false;

  showForm = false;
  formError: string | null = null;
  successMessage: string | null = null;
  companies: CompanyResponse[] = [];
  newJob: JobRequest = { title: '', description: '', location: '', salaryMin: null, salaryMax: null, companyId: null, status: 'DRAFT' };

  constructor(private jobService: JobService, private cdr: ChangeDetectorRef, public authService: AuthService) {}

  search(): void {
    if (!this.query.trim()) return;
    this.loading = true;
    this.searched = false;
    this.jobService.search(this.query).subscribe({
      next: (results) => {
        this.jobs = results;
        this.loading = false;
        this.searched = true;
      },
      error: (err) => {
        console.error('Search failed', err);
        this.loading = false;
        this.searched = true;
      }
    });
    if (this.jobs.length === 0) {
      this.loading = false;
      this.searched = true;
      console.log('empty');
    }
  }

  loadAll(): void {
    this.loading = true;
    this.jobService.getAll().subscribe({
      next: (results) => {
        this.jobs = results;
        this.loading = false;
        this.searched = true;
      },
      error: (err) => {
        console.error('Failed to load jobs', err);
        this.loading = false;
        this.searched = true;
      }
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.search();
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    this.formError = null;
    this.successMessage = null;
    if (this.showForm && this.companies.length === 0) {
      this.jobService.getCompanies().subscribe({
        next: (results) => { this.companies = results; },
        error: (err) => { console.error('Failed to load companies', err); }
      });
    }
  }

  submitJob(): void {
    this.formError = null;
    this.successMessage = null;
    this.jobService.createJob(this.newJob).subscribe({
      next: () => {
        this.newJob = { title: '', description: '', location: '', salaryMin: null, salaryMax: null, companyId: null, status: 'DRAFT' };
        this.showForm = false;
        this.successMessage = 'Job created successfully!';
        this.cdr.detectChanges();
        this.loadAll();
      },
      error: (err) => {
        this.formError = err?.error?.message ?? err?.message ?? 'Failed to create job';
      }
    });
  }
}

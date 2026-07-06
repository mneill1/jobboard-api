import { Component, OnInit, signal } from '@angular/core';
import { UserService, UserProfile } from '../service/user';
import { AuthService } from '../service/auth';
import { JobService } from '../service/job';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  standalone: false,
})
export class ProfileComponent implements OnInit {
  profile = signal<UserProfile | null>(null);
  applications = signal<any[]>([]);
  jobs = signal<any[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  editingAccount = false;
  editingCompany = false;
  editEmail = '';
  editPassword = '';
  editCompanyName = '';
  editCompanyIndustry = '';
  editCompanySize = '';
  editCompanyWebsite = '';

  saveError = signal<string | null>(null);
  saveSuccess = signal<string | null>(null);

  logoUploading = signal(false);
  logoError = signal<string | null>(null);

  constructor(
    public userService: UserService,
    public authService: AuthService,
    private jobService: JobService,
  ) {}

  ngOnInit(): void {
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.editEmail = profile.email;
        if (profile.company) {
          this.editCompanyName = profile.company.name;
          this.editCompanyIndustry = profile.company.industry;
          this.editCompanySize = profile.company.size;
          this.editCompanyWebsite = profile.company.website;
        }
        if (profile.role === 'APPLICANT') {
          this.userService.getApplications().subscribe({
            next: (apps) => {
              this.applications.set(apps);
              this.loading.set(false);
            },
            error: () => {
              this.loading.set(false);
            },
          });
        } else if (profile.role === 'COMPANY') {
          this.userService.getJobs().subscribe({
            next: (jobs) => {
              this.jobs.set(jobs);
              this.loading.set(false);
            },
            error: () => {
              this.loading.set(false);
            },
          });
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.error.set('Failed to load profile.');
        this.loading.set(false);
      },
    });
  }

  toggleEditAccount(): void {
    this.editingAccount = !this.editingAccount;
    this.saveError.set(null);
    this.saveSuccess.set(null);
  }

  toggleEditCompany(): void {
    this.editingCompany = !this.editingCompany;
    this.saveError.set(null);
    this.saveSuccess.set(null);
  }

  saveAccount(): void {
    const req: { email?: string; password?: string } = {};
    if (this.editEmail) req.email = this.editEmail;
    if (this.editPassword) req.password = this.editPassword;
    this.userService.updateProfile(req).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.editPassword = '';
        this.editingAccount = false;
        this.saveSuccess.set('Account updated.');
        this.saveError.set(null);
      },
      error: () => {
        this.saveError.set('Failed to update account.');
        this.saveSuccess.set(null);
      },
    });
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const current = this.profile();
    if (!input.files?.length || !current?.company) return;
    const file = input.files[0];
    this.logoUploading.set(true);
    this.logoError.set(null);
    this.jobService.uploadLogo(current.company.id, file).subscribe({
      next: (res: any) => {
        const p = this.profile();
        if (p?.company) {
          this.profile.set({ ...p, company: { ...p.company, logoUrl: res.logoUrl ?? null } });
        }
        this.logoUploading.set(false);
      },
      error: () => {
        this.logoError.set('Failed to upload logo.');
        this.logoUploading.set(false);
      },
    });
  }

  saveCompany(): void {
    const current = this.profile();
    if (!current?.company) return;
    const body = {
      name: this.editCompanyName,
      industry: this.editCompanyIndustry,
      size: this.editCompanySize,
      website: this.editCompanyWebsite,
    };
    this.userService.updateCompany(current.company.id, body).subscribe({
      next: () => {
        this.userService.getProfile().subscribe({
          next: (updated) => {
            this.profile.set(updated);
            this.editingCompany = false;
            this.saveSuccess.set('Company updated.');
            this.saveError.set(null);
          },
        });
      },
      error: () => {
        this.saveError.set('Failed to update company.');
        this.saveSuccess.set(null);
      },
    });
  }
}

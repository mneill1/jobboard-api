import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../service/auth';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  standalone: false
})
export class RegisterComponent {
  email = '';
  password = '';
  role = 'APPLICANT';
  companyName = '';
  companyIndustry = '';
  companySize = '';
  companyWebsite = '';
  error = signal<string | null>(null);

  constructor(private authService: AuthService, private router: Router) {}

  submit(): void {
    this.error.set(null);
    this.authService.register(
      this.email,
      this.password,
      this.role,
      this.companyName || undefined,
      this.companyIndustry || undefined,
      this.companySize || undefined,
      this.companyWebsite || undefined
    ).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (err) => this.error.set(err?.error?.message ?? 'Registration failed')
    });
  }
}

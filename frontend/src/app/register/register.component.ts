import { Component } from '@angular/core';
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
  error: string | null = null;

  constructor(private authService: AuthService, private router: Router) {}

  submit(): void {
    this.error = null;
    this.authService.register(this.email, this.password, this.role).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (err) => this.error = err?.error?.message ?? 'Registration failed'
    });
  }
}

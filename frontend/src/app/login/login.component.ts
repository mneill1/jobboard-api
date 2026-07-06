import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../service/auth';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  standalone: false
})
export class LoginComponent {
  email = '';
  password = '';
  error = signal<string | null>(null);

  constructor(private authService: AuthService, private router: Router) {}

  submit(): void {
    this.error.set(null);
    this.authService.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => this.error.set(err?.error?.message ?? 'Login failed')
    });
  }
}

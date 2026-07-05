import { Component } from '@angular/core';
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
  error: string | null = null;

  constructor(private authService: AuthService, private router: Router) {}

  submit(): void {
    this.error = null;
    this.authService.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => this.error = err?.error?.message ?? 'Login failed'
    });
  }
}

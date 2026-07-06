import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface AuthRequest {
  email: string;
  password: string;
  role?: string;
  companyName?: string;
  companyIndustry?: string;
  companySize?: string;
  companyWebsite?: string;
}

export interface AuthResponse {
  token: string;
  role: string;
}

@Service()
export class AuthService {

  private apiUrl = 'http://localhost:8080/api';
  private http = inject(HttpClient);

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, { email, password }).pipe(
      tap(res => {
        localStorage.setItem('jobboard_token', res.token);
        localStorage.setItem('jobboard_role', res.role);
      })
    );
  }

  register(
    email: string,
    password: string,
    role: string,
    companyName?: string,
    companyIndustry?: string,
    companySize?: string,
    companyWebsite?: string
  ): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, {
      email,
      password,
      role,
      companyName,
      companyIndustry,
      companySize,
      companyWebsite,
    });
  }

  logout(): void {
    localStorage.removeItem('jobboard_token');
    localStorage.removeItem('jobboard_role');
  }

  getToken(): string | null {
    return localStorage.getItem('jobboard_token');
  }

  getRole(): string | null {
    return localStorage.getItem('jobboard_role');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}

import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserProfile {
  id: number;
  email: string;
  role: string;
  companyId: number | null;
  company: { id: number; name: string; industry: string; size: string; website: string; logoUrl?: string | null } | null;
  createdAt: string;
}

export interface UpdateProfileRequest {
  email?: string;
  password?: string;
}

@Service()
export class UserService {
  private apiUrl = 'http://localhost:8080/api';
  private http = inject(HttpClient);

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/users/me`);
  }

  getApplications(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users/me/applications`);
  }

  getJobs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users/me/jobs`);
  }

  updateProfile(req: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/users/me`, req);
  }

  updateCompany(companyId: number, body: object): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/companies/${companyId}`, body);
  }
}

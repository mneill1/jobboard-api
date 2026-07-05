import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface JobResponse {
  id: number;
  title: string;
  description: string;
  location: string;
  salaryMin: number;
  salaryMax: number;
  status: string;
  companyName: string;
}

export interface CompanyResponse {
  id: number;
  name: string;
}

export interface JobRequest {
  title: string;
  description: string;
  location: string;
  salaryMin: number | null;
  salaryMax: number | null;
  companyId: number | null;
  status: string;
}

@Service()
export class JobService {

    private apiUrl = 'http://localhost:8080/api';
    private http = inject(HttpClient);


    search(query: string): Observable<JobResponse[]>{
        return this.http.get<JobResponse[]>(`${this.apiUrl}/jobs/search`, {
            params: new HttpParams().set('query', query)
        });
    }

    getAll(): Observable<JobResponse[]>{
        return this.http.get<JobResponse[]>(`${this.apiUrl}/jobs`);
    }

    list(status?: string, location?: string): Observable<JobResponse[]> {
        let params = new HttpParams();
        if (status) params = params.set('status', status);
        if (location) params = params.set('location', location);
        return this.http.get<JobResponse[]>(`${this.apiUrl}/jobs`, { params });
    }

    getCompanies(): Observable<CompanyResponse[]> {
        return this.http.get<CompanyResponse[]>(`${this.apiUrl}/companies`);
    }

    createJob(req: JobRequest): Observable<JobResponse> {
        return this.http.post<JobResponse>(`${this.apiUrl}/jobs`, req);
    }

}

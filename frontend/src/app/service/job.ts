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
  companyId: number;
  companyName: string;
  companyLogoUrl: string | null;
}

export interface CompanyResponse {
  id: number;
  name: string;
}

export interface JobFilter {
  query?: string;
  location?: string;
  minSalary?: number;
  status?: string;
  postedWithinDays?: number;
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

    list(filter: JobFilter = {}): Observable<JobResponse[]> {
        let params = new HttpParams();
        if (filter.query) params = params.set('query', filter.query);
        if (filter.location) params = params.set('location', filter.location);
        if (filter.minSalary != null) params = params.set('minSalary', String(filter.minSalary));
        if (filter.status) params = params.set('status', filter.status);
        if (filter.postedWithinDays != null) params = params.set('postedWithinDays', String(filter.postedWithinDays));
        return this.http.get<JobResponse[]>(`${this.apiUrl}/jobs`, { params });
    }

    getCompanies(): Observable<CompanyResponse[]> {
        return this.http.get<CompanyResponse[]>(`${this.apiUrl}/companies`);
    }

    createJob(req: JobRequest): Observable<JobResponse> {
        return this.http.post<JobResponse>(`${this.apiUrl}/jobs`, req);
    }

    uploadLogo(companyId: number, file: File): Observable<any> {
        const form = new FormData();
        form.append('file', file);
        return this.http.post(`${this.apiUrl}/companies/${companyId}/logo`, form);
    }

}

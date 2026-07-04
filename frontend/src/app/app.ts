import { Component, signal } from '@angular/core';
import { JobService, JobResponse } from './service/job';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
  query = '';
  jobs: JobResponse[]=[];
  loading = false;
  searched = false;

  constructor(private jobService: JobService){}

  search(): void {
    if(!this.query.trim()) return
    this.loading = true;
    this.searched = false;
    this.jobService.search(this.query).subscribe({
      next: (results)=>{
        this.jobs = results;
        this.loading = false;
        this.searched = true;
      },
      error: (err)=>{
        console.error('Search failed', err);
        this.loading = false;
        this.searched = true;
      }
    });
    if(this.jobs.length === 0){
      this.loading = false;
      this.searched = true;
      console.log("empty")
    }
  }

  loadAll(): void{
    this.loading = true;
    this.jobService.getAll().subscribe({
      next: (results) =>{
        this.jobs = results;
        this.loading = false;
        this.searched = true;
      },
      error: (err)=>{
        console.error("Failed to load jobs", err);
        this.loading = false;
        this.searched = true;
      }
    });
  }

  onKeydown(event: KeyboardEvent): void{
    if(event.key === 'Enter') this.search
  }
}

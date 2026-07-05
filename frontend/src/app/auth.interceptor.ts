import { inject } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './service/auth';

export class AuthInterceptor implements HttpInterceptor {

  private authService = inject(AuthService);

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();
    if (token) {
      const clonedReq = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
      return next.handle(clonedReq);
    }
    return next.handle(req);
  }
}

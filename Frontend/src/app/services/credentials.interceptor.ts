import { HttpInterceptorFn } from '@angular/common/http';

/** Envía y acepta la cookie HttpOnly JSESSIONID del backend. */
export const credentialsInterceptor: HttpInterceptorFn = (request, next) =>
  next(request.clone({ withCredentials: true }));

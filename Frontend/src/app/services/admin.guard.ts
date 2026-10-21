import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.sesionActual().pipe(map(usuario => {
    if (!usuario) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    return usuario.administrador ? true : router.createUrlTree(['/']);
  }));
};

import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';

export interface SesionUsuario {
  mensaje: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = 'http://localhost:8080/api';
  readonly usuario = signal<SesionUsuario | null>(null);

  login(email: string, contrasenia: string): Observable<SesionUsuario> {
    return this.http.post<SesionUsuario>(`${this.api}/auth/login`, {
      email: email.trim().toLowerCase(), contrasenia,
    }).pipe(tap(usuario => this.usuario.set(usuario)));
  }

  registrar(nombre: string, email: string, contrasenia: string): Observable<unknown> {
    return this.http.post(`${this.api}/usuarios/registro`, {
      nombre: nombre.trim(), email: email.trim().toLowerCase(), contrasenia,
    });
  }

  sesionActual(): Observable<SesionUsuario | null> {
    return this.http.get<SesionUsuario>(`${this.api}/auth/me`).pipe(
      tap(usuario => this.usuario.set(usuario)),
      catchError(() => { this.usuario.set(null); return of(null); }),
    );
  }

  comprobarSesion(): Observable<boolean> {
    return this.sesionActual().pipe(map(usuario => usuario !== null));
  }

  cerrarSesion(): Observable<unknown> {
    return this.http.post(`${this.api}/auth/logout`, {}).pipe(
      tap(() => this.usuario.set(null)),
    );
  }
}

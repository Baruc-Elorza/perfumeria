import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly registrando = signal(false);
  readonly cargando = signal(false);
  readonly error = signal('');
  nombre = '';
  email = '';
  contrasenia = '';

  alternarModo(): void {
    this.registrando.update(valor => !valor);
    this.error.set('');
  }

  enviar(): void {
    if (this.cargando()) return;
    this.error.set('');
    this.cargando.set(true);
    if (this.registrando()) {
      this.auth.registrar(this.nombre, this.email, this.contrasenia).subscribe({
        next: () => this.iniciarSesion(),
        error: err => this.mostrarError(err),
      });
      return;
    }
    this.auth.login(this.email, this.contrasenia).subscribe({
      next: () => this.irADestino(),
      error: err => this.mostrarError(err),
    });
  }

  private iniciarSesion(): void {
    this.auth.login(this.email, this.contrasenia).subscribe({
      next: () => this.irADestino(),
      error: err => this.mostrarError(err),
    });
  }

  private irADestino(): void {
    this.cargando.set(false);
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const rutaInterna = returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//')
      ? returnUrl : '/';
    void this.router.navigateByUrl(rutaInterna);
  }

  private mostrarError(err: any): void {
    this.cargando.set(false);
    const status = err.status;
    this.error.set(status === 401 ? 'Correo o contraseña incorrectos.'
      : status === 409 ? 'Ese correo ya tiene una cuenta.'
      : err.error?.message || err.error || 'No se pudo conectar. Revisa que el servidor esté disponible e inténtalo de nuevo.');
  }
}

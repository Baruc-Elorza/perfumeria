import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-recuperacion',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './recuperacion.component.html',
})
export class RecuperacionComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  readonly restableciendo = this.route.snapshot.data['restablecer'] === true;
  private readonly token = new URLSearchParams(this.route.snapshot.fragment || '').get('token') || '';
  readonly cargando = signal(false);
  readonly verificando = signal(this.restableciendo);
  readonly enlaceValido = signal(false);
  readonly terminado = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  email = '';
  contrasenia = '';
  confirmacion = '';

  constructor() {
    if (!this.restableciendo) return;
    if (!/^[A-Za-z0-9_-]{43}$/.test(this.token)) {
      this.error.set('El enlace es inválido o está incompleto. Solicita uno nuevo.');
      this.verificando.set(false);
      return;
    }
    this.auth.validarEnlaceRecuperacion(this.token).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.enlaceValido.set(true); this.verificando.set(false); },
      error: err => {
        this.error.set(err.error?.message || 'No se pudo validar el enlace. Inténtalo de nuevo más tarde.');
        this.verificando.set(false);
      },
    });
  }

  enviar(formulario: NgForm): void {
    if (formulario.invalid || this.cargando() || this.terminado()) return;
    this.error.set('');
    this.mensaje.set('');
    if (this.restableciendo) {
      if (!this.enlaceValido()) return;
      if (this.contrasenia !== this.confirmacion) {
        this.error.set('Las contraseñas no coinciden.');
        return;
      }
      if (new TextEncoder().encode(this.contrasenia).length > 72 || /[\u0000-\u001f\u007f-\u009f]/.test(this.contrasenia)) {
        this.error.set('La contraseña supera el tamaño permitido o contiene caracteres de control. Usa una frase más corta.');
        return;
      }
    }
    this.cargando.set(true);
    const solicitud = this.restableciendo
      ? this.auth.restablecerContrasenia(this.token, this.contrasenia, this.confirmacion)
      : this.auth.solicitarRecuperacion(this.email);
    solicitud.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: respuesta => {
        this.cargando.set(false);
        this.mensaje.set(respuesta.mensaje);
        this.terminado.set(true);
        this.contrasenia = '';
        this.confirmacion = '';
        if (this.restableciendo) this.auth.usuario.set(null);
      },
      error: err => {
        this.cargando.set(false);
        this.error.set(err.error?.message || 'No se pudo completar la solicitud. Revisa tu conexión e inténtalo de nuevo.');
      },
    });
  }
}

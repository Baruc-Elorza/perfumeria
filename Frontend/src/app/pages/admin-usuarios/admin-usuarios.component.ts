import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { UsuarioAdmin } from '../../models/usuario-admin.model';
import { UsuarioAdminService } from '../../services/usuario-admin.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './admin-usuarios.component.html'
})
export class AdminUsuariosComponent {
  private readonly servicio = inject(UsuarioAdminService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly usuarios = signal<UsuarioAdmin[]>([]);
  readonly busqueda = signal('');
  readonly estado = signal('todos');
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly cargandoDetalle = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly seleccionado = signal<UsuarioAdmin | null>(null);
  readonly pendiente = signal<UsuarioAdmin | null>(null);
  readonly filtrados = computed(() => {
    const texto = this.busqueda().trim().toLocaleLowerCase();
    return this.usuarios().filter(u =>
      (!texto || u.nombre.toLocaleLowerCase().includes(texto) || u.email.toLocaleLowerCase().includes(texto))
      && (this.estado() === 'todos' || (this.estado() === 'activos' ? u.activo : !u.activo)));
  });

  constructor() { this.cargar(); }

  cargar(): void {
    if (this.guardando() || this.cargando() || this.cargandoDetalle()) return;
    this.cargando.set(true);
    this.error.set('');
    this.seleccionado.set(null);
    this.pendiente.set(null);
    this.servicio.listar().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: usuarios => { this.usuarios.set(usuarios); this.cargando.set(false); },
      error: e => { this.cargando.set(false); this.fallo(e, 'No se pudo cargar la lista de usuarios.'); }
    });
  }

  verDetalle(usuario: UsuarioAdmin): void {
    if (this.guardando() || this.cargandoDetalle()) return;
    this.cargandoDetalle.set(true);
    this.error.set('');
    this.seleccionado.set(null);
    this.pendiente.set(null);
    this.servicio.detalle(usuario.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: detalle => { this.seleccionado.set(detalle); this.cargandoDetalle.set(false); },
      error: e => { this.cargandoDetalle.set(false); this.fallo(e, 'No se pudo consultar el usuario. Actualiza la lista.'); }
    });
  }

  solicitarCambio(usuario: UsuarioAdmin): void {
    if (!usuario.puedeCambiarEstado || this.guardando() || this.cargandoDetalle()) return;
    this.error.set('');
    this.mensaje.set('');
    this.pendiente.set(usuario);
  }

  confirmar(): void {
    const usuario = this.pendiente();
    if (!usuario || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.cambiarEstado(usuario.id, !usuario.activo)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: actualizado => {
          this.usuarios.update(lista => lista.map(u => u.id === actualizado.id ? actualizado : u));
          if (this.seleccionado()?.id === actualizado.id) this.seleccionado.set(actualizado);
          this.guardando.set(false);
          this.pendiente.set(null);
          this.mensaje.set(actualizado.activo
            ? 'Usuario desbloqueado. Puede iniciar sesión nuevamente.'
            : 'Usuario bloqueado. Sus sesiones serán rechazadas en la siguiente petición.');
        },
        error: e => { this.guardando.set(false); this.pendiente.set(null);
          this.fallo(e, 'No se pudo cambiar el estado. Actualiza la lista e inténtalo de nuevo.'); }
      });
  }

  private fallo(error: HttpErrorResponse, defecto: string): void {
    if (error.status === 401) {
      this.auth.usuario.set(null);
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/admin/usuarios' } });
      return;
    }
    this.error.set(error.status === 403 ? 'No tienes permiso para administrar usuarios.'
      : error.error?.message || defecto);
  }
}

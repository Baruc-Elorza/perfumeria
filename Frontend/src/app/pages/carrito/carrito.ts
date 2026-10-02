import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { CarritoService } from '../../services/carrito';
import { CarritoRespuesta, LineaCarrito } from '../../models/carrito.model';

@Component({
  imports: [CurrencyPipe, RouterLink],
  selector: 'app-carrito',
  standalone: true,
  styleUrl: './carrito.css',
  templateUrl: './carrito.html',
})
export class Carrito implements OnInit {
  readonly carrito = signal<CarritoRespuesta>({ items: [], subtotal: 0 });
  readonly cargando = signal(true);
  readonly actualizando = signal(false);
  readonly error = signal('');
  private readonly servicio = inject(CarritoService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.solicitar(this.servicio.obtenerCarrito());
  }

  cambiarCantidad(producto: LineaCarrito, diferencia: number): void {
    const cantidad = producto.cantidad + diferencia;
    if (this.actualizando() || cantidad < 1 || cantidad > producto.stock) return;
    this.solicitar(this.servicio.cambiarCantidad(producto.perfumeId, cantidad));
  }

  eliminar(producto: LineaCarrito): void {
    if (!this.actualizando()) this.solicitar(this.servicio.eliminar(producto.perfumeId));
  }

  private solicitar(solicitud: Observable<CarritoRespuesta>): void {
    this.error.set('');
    this.actualizando.set(true);
    solicitud.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (respuesta) => {
        this.carrito.set(respuesta);
        this.cargando.set(false);
        this.actualizando.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo actualizar el carrito. Intenta nuevamente.');
        this.cargando.set(false);
        this.actualizando.set(false);
      }
    });
  }
}

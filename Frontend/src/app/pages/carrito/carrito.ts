import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { CarritoService } from '../../services/carrito.service';
import { CarritoRespuesta, LineaCarrito } from '../../models/carrito.model';
import { CompraService } from '../../services/compra.service';

@Component({
  imports: [CurrencyPipe, RouterLink],
  selector: 'app-carrito',
  standalone: true,
  templateUrl: './carrito.html',
})
export class Carrito implements OnInit {
  readonly carrito = signal<CarritoRespuesta>({
    items: [],
    subtotal: 0,
  });

  readonly cargando = signal(true);
  readonly actualizando = signal(false);
  readonly error = signal('');

  private readonly servicio = inject(CarritoService);
  private readonly compraService = inject(CompraService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.solicitar(this.servicio.obtenerCarrito());
  }

cambiarCantidad(producto: LineaCarrito | any, diferencia: number): void {
  // Extraemos el ID del producto asegurando que no sea undefined
  const perfumeId = producto.perfumeId ?? producto.id ?? producto.productoId;
  const cantidadNueva = (producto.cantidad || 1) + diferencia;

  // Evitamos peticiones innecesarias si baja de 1 o sobrepasa el stock
  if (this.actualizando() || cantidadNueva < 1) return;
  if (producto.stock && cantidadNueva > producto.stock) return;

  if (!perfumeId) {
    console.error('Error: No se encontró el perfumeId en el producto', producto);
    return;
  }

  // Llamamos al servicio con la signature exacta: (perfumeId, cantidad)
  this.solicitar(this.servicio.cambiarCantidad(perfumeId, cantidadNueva));
}

  eliminar(producto: LineaCarrito): void {
    if (!this.actualizando()) {
      this.solicitar(this.servicio.eliminar(producto.perfumeId));
    }
  }

  /**
   * Guarda el estado actual del carrito en localStorage y navega al Checkout
   */
  irAlCheckout(): void {
    const datosCarrito = this.carrito();
    localStorage.setItem('carrito', JSON.stringify(datosCarrito));
    this.router.navigate(['/checkout']);
  }

  procesarPago(pedidoId: string, referenciaPago: string): void {
    this.error.set('');
    this.actualizando.set(true);

    this.compraService.confirmarPago(pedidoId, referenciaPago)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (respuesta: any) => {
          this.actualizando.set(false);
          console.log('Compra procesada:', respuesta);
          alert('¡Compra procesada y registrada exitosamente!');
        },
        error: (err: any) => {
          this.error.set(err.error?.message || err.error || 'No se pudo procesar la compra. Intenta nuevamente.');
          this.actualizando.set(false);
        }
      });
  }

  private solicitar(solicitud: Observable<CarritoRespuesta>): void {
    this.error.set('');
    this.actualizando.set(true);
    solicitud.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (respuesta: CarritoRespuesta) => {
        this.carrito.set(respuesta);
        // Guardar la respuesta actualizada del carrito en localStorage
        localStorage.setItem('carrito', JSON.stringify(respuesta));
        this.cargando.set(false);
        this.actualizando.set(false);
      },
      error: (err: any) => {
        this.error.set(err.error?.message || 'No se pudo actualizar el carrito. Intenta nuevamente.');
        this.cargando.set(false);
        this.actualizando.set(false);
      }
    });
  }
}

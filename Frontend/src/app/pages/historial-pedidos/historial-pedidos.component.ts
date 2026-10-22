
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Pedido, PedidoService } from '../../services/pedido.service';

@Component({
  selector: 'app-historial-pedidos',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './historial-pedidos.html',
})
export class HistorialPedidosComponent implements OnInit {
  private pedidoService = inject(PedidoService);
  private cdr = inject(ChangeDetectorRef);

  pedidos: Pedido[] = [];
  pedidoSeleccionado: Pedido | null = null;
  cargando = true;
  error = '';

  ngOnInit(): void {
    this.cargarHistorial();
  }

  cargarHistorial(): void {
    this.cargando = true;
    this.error = '';

    this.pedidoService.obtenerHistorial().subscribe({
      next: pedidos => {
        console.log('1. Respuesta recibida:', pedidos.length);

        this.pedidos = pedidos;
        this.cargando = false;
        this.cdr.detectChanges();

        console.log('2. Pedidos guardados:', this.pedidos.length);
        console.log('3. Estado de carga:', this.cargando);
      },
      error: err => {
        console.error('Error al cargar el historial:', err);

        this.error =
          err.status === 401 || err.status === 403
            ? 'Inicia sesión para consultar tus pedidos.'
            : 'No se pudo cargar el historial. Inténtalo de nuevo.';

        this.cargando = false;
      }
    });
  }


verDetalle(pedido: Pedido): void {
  // Si ese pedido ya está abierto, cerrarlo inmediatamente.
  if (this.pedidoSeleccionado?.id === pedido.id) {
    this.pedidoSeleccionado = null;
    return;
  }

  // Mostrar el pedido seleccionado inmediatamente.
  this.pedidoSeleccionado = pedido;
  this.cdr.detectChanges();

  // Consultar el detalle actualizado en el backend.
  this.pedidoService.obtenerDetalle(pedido.id).subscribe({
    next: detalle => {
      // Actualizar los datos cuando llegue la respuesta.
      if (this.pedidoSeleccionado?.id === pedido.id) {
        this.pedidoSeleccionado = detalle;
        this.cdr.detectChanges();
      }
    },
    error: err => {
      console.error('Error al cargar el detalle:', err);
      this.error = 'No se pudo cargar el detalle del pedido.';
      this.cdr.detectChanges();
    }
  });
}


  textoEstado(estado: string): string {
    switch (estado) {
      case 'PAGADO':
        return 'Pagado';
      case 'PENDIENTE':
        return 'Pago pendiente';
      case 'RECHAZADO':
        return 'Pago rechazado';
      case 'CANCELADO':
        return 'Cancelado';
      case 'ENVIADO':
        return 'Enviado';
      default:
        return estado;
    }
  }
}

import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Pedido, PedidoService } from '../../services/pedido.service';

@Component({
  selector: 'app-admin-pedidos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './admin-pedidos.component.html',
  styleUrl: './admin-pedidos.component.css',
})
export class AdminPedidosComponent implements OnInit {
  private readonly pedidoService = inject(PedidoService);

  pedidos: Pedido[] = [];
  guias: Record<string, string> = {};
  guardandoId: string | null = null;
  cargando = true;
  error = '';
  mensaje = '';

  ngOnInit(): void {
    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.cargando = true;
    this.error = '';
    this.pedidoService.obtenerPedidosParaEnvio().subscribe({
      next: pedidos => {
        this.pedidos = pedidos;
        this.guias = Object.fromEntries(pedidos.map(pedido => [pedido.id, pedido.numeroGuia ?? '']));
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los pedidos pagados.';
        this.cargando = false;
      },
    });
  }

  guardarGuia(pedido: Pedido): void {
    const numeroGuia = (this.guias[pedido.id] ?? '').trim();
    if (!numeroGuia || this.guardandoId) return;

    this.guardandoId = pedido.id;
    this.error = '';
    this.mensaje = '';
    this.pedidoService.actualizarGuia(pedido.id, numeroGuia).subscribe({
      next: actualizado => {
        this.pedidos = this.pedidos.map(item => item.id === actualizado.id ? actualizado : item);
        this.guias[actualizado.id] = actualizado.numeroGuia ?? '';
        this.guardandoId = null;
        this.mensaje = `El pedido ${actualizado.id} quedó marcado como enviado.`;
      },
      error: err => {
        this.guardandoId = null;
        this.error = err.error?.message || 'No se pudo guardar el número de guía.';
      },
    });
  }
}

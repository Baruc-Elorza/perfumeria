import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ItemPedido {
  productoId: string;
  nombre: string;
  marca: string;
  cantidad: number;
  precioUnitario: number;
}

export interface Pedido {
  id: string;
  estado: string;
  clienteEmail: string;
  fechaCreacion: string;
  clienteNombre: string;
  destinatarioEnvio: string;
  direccionEnvio: string;
  telefono: string;
  subtotal: number;
  envio: number;
  descuento: number;
  total: number;
  productos: ItemPedido[];
}

@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private readonly apiUrl = 'http://localhost:8080/api/pedidos';

  constructor(private http: HttpClient) {}

  obtenerHistorial(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.apiUrl, {
      withCredentials: true
    });
  }

  obtenerDetalle(id: string): Observable<Pedido> {
    return this.http.get<Pedido>(
      `${this.apiUrl}/${encodeURIComponent(id)}`,
      { withCredentials: true }
    );
  }
}
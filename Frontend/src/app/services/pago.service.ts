
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PagoService {

  private apiUrl = 'http://localhost:8080/api/pagos';
  private comprasUrl = 'http://localhost:8080/api/compras';

  constructor(private http: HttpClient) {}

  obtenerConfiguracion(): Observable<{ publicKey: string }> {
    return this.http.get<{ publicKey: string }>(
      `${this.apiUrl}/config`
    );
  }

  crearPago(
    pedidoId: string
  ): Observable<{
    clientSecret: string;
    paymentIntentId: string;
  }> {
    return this.http.post<{
      clientSecret: string;
      paymentIntentId: string;
    }>(
      `${this.apiUrl}/crear`,
      null,
      {
        params: { pedidoId },
        withCredentials: true
      }
    );
  }

  obtenerEstado(
    id: string
  ): Observable<{ estado: string; mensaje: string }> {
    return this.http.get<{ estado: string; mensaje: string }>(
      `${this.apiUrl}/estado/${encodeURIComponent(id)}`,
      { withCredentials: true }
    );
  }

  confirmarCompra(
    pedidoId: string,
    paymentIntentId: string
  ): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(
      `${this.comprasUrl}/confirmar`,
      {
        pedidoId,
        referenciaPagoProveedor: paymentIntentId
      },
      { withCredentials: true }
    );
  }
}

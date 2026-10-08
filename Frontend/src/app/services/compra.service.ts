import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CompraService {
  private apiUrl = 'http://localhost:8080/api/compras';

  constructor(private http: HttpClient) {}

  confirmarPago(pedidoId: string, referenciaPagoProveedor: string): Observable<any> {
    const payload = {
      pedidoId: pedidoId,
      referenciaPagoProveedor: referenciaPagoProveedor
    };
    return this.http.post(`${this.apiUrl}/confirmar`, payload);
  }
}

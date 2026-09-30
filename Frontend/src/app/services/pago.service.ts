import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PagoService {

  private apiUrl = 'http://localhost:8080/api/pagos';

  constructor(private http: HttpClient) {}

  obtenerConfiguracion(): Observable<{ publicKey: string }> {
    return this.http.get<{ publicKey: string }>(
      `${this.apiUrl}/config`
    );
  }

  crearPago(monto: number): Observable<{
    clientSecret: string;
    paymentIntentId: string;
  }> {
    return this.http.post<{
      clientSecret: string;
      paymentIntentId: string;
    }>(
      `${this.apiUrl}/crear?monto=${monto}`,
      {}
    );
  }

  obtenerEstado(id: string): Observable<{
    estado: string;
    mensaje: string;
  }> {
    return this.http.get<{
      estado: string;
      mensaje: string;
    }>(
      `${this.apiUrl}/estado/${id}`
    );
  }
}
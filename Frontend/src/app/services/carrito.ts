import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CarritoRespuesta } from '../models/carrito.model';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {

  private apiUrl = 'http://localhost:8080/api/carrito';

  constructor(private http: HttpClient) {}

  obtenerCarrito(): Observable<CarritoRespuesta> {
    return this.http.get<CarritoRespuesta>(this.apiUrl, { withCredentials: true });
  }

  agregar(perfumeId: number): Observable<CarritoRespuesta> {
    return this.http.post<CarritoRespuesta>(`${this.apiUrl}/items/${perfumeId}`,
      { cantidad: 1 }, { withCredentials: true });
  }

  cambiarCantidad(perfumeId: number, cantidad: number): Observable<CarritoRespuesta> {
    return this.http.patch<CarritoRespuesta>(`${this.apiUrl}/items/${perfumeId}`,
      { cantidad }, { withCredentials: true });
  }

  eliminar(perfumeId: number): Observable<CarritoRespuesta> {
    return this.http.delete<CarritoRespuesta>(`${this.apiUrl}/items/${perfumeId}`, { withCredentials: true });
  }
}

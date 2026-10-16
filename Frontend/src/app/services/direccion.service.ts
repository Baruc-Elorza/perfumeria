import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Direccion } from '../models/direccion.model';

@Injectable({
  providedIn: 'root'
})
export class DireccionService {

  private apiUrl = 'http://localhost:8080/api/direcciones';

  constructor(private http: HttpClient) { }

  obtenerDireccionesPorUsuario(usuarioId: number): Observable<Direccion[]> {
    return this.http.get<Direccion[]>(`${this.apiUrl}/usuario/${usuarioId}`);
  }

  guardarDireccion(direccion: Direccion): Observable<Direccion> {
    return this.http.post<Direccion>(this.apiUrl, direccion);
  }

  eliminarDireccion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

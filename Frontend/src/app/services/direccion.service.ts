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

  obtenerMisDirecciones(): Observable<Direccion[]> {
    return this.http.get<Direccion[]>(this.apiUrl);
  }

  guardarDireccion(direccion: Direccion): Observable<Direccion> {
    return direccion.id == null
      ? this.http.post<Direccion>(this.apiUrl, direccion)
      : this.http.put<Direccion>(`${this.apiUrl}/${direccion.id}`, direccion);
  }

  eliminarDireccion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

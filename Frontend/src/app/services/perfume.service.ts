import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Perfume } from '../models/perfume.model';

@Injectable({
  providedIn: 'root'
})
export class PerfumeService {
  private apiUrl = 'http://localhost:8080/api/perfumes';

  constructor(private http: HttpClient) { }

  getPerfumes(): Observable<Perfume[]> {
    return this.http.get<Perfume[]>(this.apiUrl);
  }

  buscarPorNombre(nombre: string): Observable<Perfume[]> {
    return this.http.get<Perfume[]>(this.apiUrl, {
      params: { nombre: nombre.trim() }
    });
  }

  // H.U.-09: Obtener el detalle de un perfume por su ID
  getPerfumePorId(id: number): Observable<Perfume> {
    return this.http.get<Perfume>(`${this.apiUrl}/${id}`);
  }

  agregarPerfume(perfume: Perfume): Observable<Perfume> {
    return this.http.post<Perfume>(this.apiUrl, perfume);
  }

  eliminarPerfume(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  actualizarPerfume(id: number, perfume: Perfume): Observable<Perfume> {
    return this.http.put<Perfume>(`${this.apiUrl}/${id}`, perfume);
  }
}

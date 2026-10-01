import { Service } from '@angular/core';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {

  private apiUrl = 'http://localhost:8080/api/carrito';

  constructor(private http: HttpClient) {}

  obtenerCarrito() {
    return this.http.get<any>(this.apiUrl);
  }
}
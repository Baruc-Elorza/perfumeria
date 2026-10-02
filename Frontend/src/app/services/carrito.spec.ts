import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CarritoService } from './carrito';

describe('CarritoService', () => {
  it('envía la cantidad con la sesión del navegador al agregar un perfume', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CarritoService);
    const http = TestBed.inject(HttpTestingController);
    service.agregar(7).subscribe();
    const request = http.expectOne('http://localhost:8080/api/carrito/items/7');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ cantidad: 1 });
    expect(request.request.withCredentials).toBe(true);
    request.flush({ items: [], subtotal: 0 });
    http.verify();
  });
});

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Carrito } from './carrito';

describe('Carrito', () => {
  it('muestra cantidades y subtotal del servidor; eliminar restaura el estado vacío', async () => {
    TestBed.configureTestingModule({
      imports: [Carrito],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const fixture = TestBed.createComponent(Carrito);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/carrito').flush({
      items: [{ perfumeId: 1, nombre: 'Sauvage', marca: 'Dior', precio: 10.5, cantidad: 2, stock: 2 }],
      subtotal: 21
    });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Sauvage');
    expect(fixture.nativeElement.textContent).toContain('$21.00');
    expect(fixture.nativeElement.querySelector('[aria-label="Aumentar cantidad de Sauvage"]').disabled).toBe(true);
    fixture.nativeElement.querySelector('.remove').click();
    const eliminar = http.expectOne('http://localhost:8080/api/carrito/items/1');
    expect(eliminar.request.method).toBe('DELETE');
    eliminar.flush({ items: [], subtotal: 0 });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Tu carrito está vacío');
    http.verify();
  });
});

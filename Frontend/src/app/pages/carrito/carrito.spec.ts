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
  it('bloquea continuar al pago si el stock bajó o el perfume se agotó', async () => {
    TestBed.configureTestingModule({ imports: [Carrito],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const fixture = TestBed.createComponent(Carrito);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const producto = { perfumeId: 1, nombre: 'Eros', marca: 'Versace', precio: 100, cantidad: 3, stock: 2 };
    http.expectOne('http://localhost:8080/api/carrito').flush({ items: [producto], subtotal: 300 });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.btn-proceder-pago').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[aria-label="Aumentar cantidad de Eros"]').disabled).toBe(true);
    fixture.componentInstance.carrito.set({ items: [{ ...producto, cantidad: 1, stock: 0 }], subtotal: 100 });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.btn-proceder-pago').disabled).toBe(true);
    fixture.componentInstance.carrito.set({ items: [{ ...producto, cantidad: 2, stock: 2 }], subtotal: 200 });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.btn-proceder-pago').disabled).toBe(false);
    http.verify();
  });

  it('permite reducir de una vez hasta el stock actual cuando las existencias cambiaron', async () => {
    TestBed.configureTestingModule({ imports: [Carrito],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const fixture = TestBed.createComponent(Carrito);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const producto = { perfumeId: 1, nombre: 'Eros', marca: 'Versace', precio: 100, cantidad: 5, stock: 2 };
    http.expectOne('http://localhost:8080/api/carrito').flush({ items: [producto], subtotal: 500 });
    await fixture.whenStable();
    fixture.nativeElement.querySelector('[aria-label="Reducir cantidad de Eros"]').click();
    const cambio = http.expectOne('http://localhost:8080/api/carrito/items/1');
    expect(cambio.request.body.cantidad).toBe(2);
    cambio.flush({ items: [{ ...producto, cantidad: 2 }], subtotal: 200 });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.btn-proceder-pago').disabled).toBe(false);
    http.verify();
  });

});

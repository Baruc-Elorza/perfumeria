import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { routes } from './app.routes';

describe('Navegación', () => {
  it('al abrir carrito muestra únicamente esa página', async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()]
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/carrito');
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('http://localhost:8080/api/carrito').flush({ items: [], subtotal: 0 });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Mi carrito');
    expect(harness.routeNativeElement?.textContent).not.toContain('Catálogo de fragancias');
    http.verify();
  });
});

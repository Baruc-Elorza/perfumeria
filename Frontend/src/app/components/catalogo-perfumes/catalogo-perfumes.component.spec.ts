import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CatalogoPerfumesComponent } from './catalogo-perfumes.component';

const perfumes = [
  { id: 1, nombre: 'Sauvage', marca: 'Dior', precio: 2450, stock: 3 },
  { id: 2, nombre: 'Eros', marca: 'Versace', precio: 2100, stock: 0 }
];

describe('Catálogo y búsqueda', () => {
  function crear() {
    TestBed.configureTestingModule({
      imports: [CatalogoPerfumesComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const fixture = TestBed.createComponent(CatalogoPerfumesComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const consulta = (nombre: string) => vi.waitFor(() => http.expectOne(req =>
      req.url.endsWith('/api/perfumes') && req.params.get('nombre') === nombre));
    return { fixture, http, consulta };
  }

  it('cancela búsquedas antiguas, informa sin coincidencias y restaura el catálogo al borrar', async () => {
    const { fixture, http, consulta } = crear();
    (await consulta('')).flush(perfumes);
    await fixture.whenStable();
    const botones = fixture.nativeElement.querySelectorAll('.checkout');
    expect(botones[0].disabled).toBe(false);
    expect(botones[1].disabled).toBe(true);
    fixture.componentInstance.buscar('sau');
    const anterior = await consulta('sau');
    fixture.componentInstance.buscar('ero');
    expect(anterior.cancelled).toBe(true);
    (await consulta('ero')).flush([perfumes[1]]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.tarjeta').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.tarjeta').textContent).toContain('Eros');
    fixture.componentInstance.buscar('inexistente');
    (await consulta('inexistente')).flush([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron perfumes con ese nombre.');
    fixture.componentInstance.buscar('');
    (await consulta('')).flush(perfumes);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.tarjeta').length).toBe(2);
    http.verify();
  });

  it('permite reintentar después de un error del servidor', async () => {
    const { fixture, http, consulta } = crear();
    (await consulta('')).flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('No fue posible cargar el catálogo.');
    fixture.nativeElement.querySelector('.estado button').click();
    (await consulta('')).flush(perfumes);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.tarjeta').length).toBe(2);
    http.verify();
  });
});

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
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
    http.expectOne('http://localhost:8080/api/auth/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    dialog.showModal = vi.fn(() => dialog.setAttribute('open', ''));
    dialog.close = vi.fn(() => dialog.removeAttribute('open'));
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
  it('abre el detalle desde la imagen, muestra la ficha completa y permite comprar', async () => {
    const { fixture, http, consulta } = crear();
    const producto = { ...perfumes[0], imagenUrl: '/perfume.jpg', descripcion: 'Fragancia fresca.',
      ml: 100, concentracion: 'EDP', notasTop: 'Bergamota', notasMiddle: 'Lavanda', notasBase: 'Cedro' };
    (await consulta('')).flush([producto]);
    await fixture.whenStable();
    fixture.nativeElement.querySelector('.abrir-perfume').click();
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('dialog');
    expect(dialog.open).toBe(true);
    expect(dialog.querySelector('.checkout').disabled).toBe(true);
    http.expectOne('http://localhost:8080/api/perfumes/1').flush(producto);
    await fixture.whenStable();
    for (const texto of ['Sauvage', 'Dior', 'Fragancia fresca.', '100 ml', 'EDP', 'Bergamota', 'Lavanda', 'Cedro']) {
      expect(dialog.textContent).toContain(texto);
    }
    expect(dialog.querySelector('.detalle-imagen img').src).toContain('/perfume.jpg');
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    dialog.querySelector('.checkout').click();
    const compra = http.expectOne('http://localhost:8080/api/carrito/items/1');
    expect(compra.request.method).toBe('POST');
    compra.flush({ items: [], subtotal: 2450 });
    await fixture.whenStable();
    expect(dialog.open).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/carrito']);
    http.verify();
  });

  it('permite reintentar el detalle, impide comprar agotados y cierra con Escape', async () => {
    const { fixture, http, consulta } = crear();
    (await consulta('')).flush(perfumes);
    await fixture.whenStable();
    fixture.nativeElement.querySelectorAll('.nombre-perfume')[1].click();
    http.expectOne('http://localhost:8080/api/perfumes/2').flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const dialog = fixture.nativeElement.querySelector('dialog');
    expect(dialog.textContent).toContain('No se pudieron cargar los detalles');
    expect(dialog.querySelector('.checkout').disabled).toBe(true);
    dialog.querySelector('.estado button').click();
    http.expectOne('http://localhost:8080/api/perfumes/2').flush(perfumes[1]);
    await fixture.whenStable();
    expect(dialog.querySelector('.detalle-ficha')).toBeNull();
    expect(dialog.querySelector('.detalle-descripcion')).toBeNull();
    expect(dialog.textContent).not.toContain('No especificad');
    expect(dialog.querySelector('.checkout').disabled).toBe(true);
    dialog.dispatchEvent(new Event('cancel', { cancelable: true }));
    await fixture.whenStable();
    expect(dialog.open).toBe(false);
    expect(fixture.componentInstance.detalle()).toBeNull();
    http.verify();
  });

  it('cancela la carga al cerrar para no mostrar respuestas antiguas', async () => {
    const { fixture, http, consulta } = crear();
    (await consulta('')).flush(perfumes);
    await fixture.whenStable();
    fixture.nativeElement.querySelector('.abrir-perfume').click();
    const pendiente = http.expectOne('http://localhost:8080/api/perfumes/1');
    fixture.nativeElement.querySelector('.detalle-cerrar').click();
    expect(pendiente.cancelled).toBe(true);
    http.verify();
  });

});

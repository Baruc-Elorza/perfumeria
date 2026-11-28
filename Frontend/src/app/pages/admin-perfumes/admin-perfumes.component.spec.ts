import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminPerfumesComponent } from './admin-perfumes.component';
import { Perfume } from '../../models/perfume.model';

const api = 'http://localhost:8080/api/perfumes';
const producto: Perfume = { id: 1, nombre: 'Eros', marca: 'Versace', precio: 1500, stock: 3, ml: 100, concentracion: 'EDP' };

function crear(productos: Perfume[] = []) {
  TestBed.configureTestingModule({ imports: [AdminPerfumesComponent],
    providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(AdminPerfumesComponent);
  const http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
  http.expectOne(api).flush(productos);
  fixture.detectChanges();
  return { fixture, http };
}

describe('Presentación y concentración en administración', () => {
  it('envía mililitros y una sola concentración al crear y limpia ambos campos', async () => {
    const { fixture, http } = crear();
    fixture.componentInstance.nuevoPerfume = { nombre: 'Eros', marca: 'Versace', precio: 1500, stock: 3, ml: null, concentracion: null };
    fixture.detectChanges(); await fixture.whenStable();
    const ml = fixture.nativeElement.querySelector('input[name="ml"]');
    ml.value = '100'; ml.dispatchEvent(new Event('input'));
    const select = fixture.nativeElement.querySelector('select[name="concentracion"]');
    expect(select.multiple).toBe(false);
    expect(Array.from(select.options).map((o: any) => o.textContent.trim())).toEqual(['Sin especificar', 'EDC', 'EDT', 'EDP', 'PARFUM']);
    select.value = 'EDC'; select.dispatchEvent(new Event('change'));
    fixture.detectChanges(); await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const solicitud = http.expectOne(api);
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body.ml).toBe(100);
    expect(solicitud.request.body.concentracion).toBe('EDC');
    solicitud.flush({ ...producto, concentracion: 'EDC' });
    await fixture.whenStable();
    expect(fixture.componentInstance.nuevoPerfume.ml).toBeNull();
    expect(fixture.componentInstance.nuevoPerfume.concentracion).toBeNull();
    http.verify();
  });

  it('recupera y actualiza ambos valores al editar un perfume', async () => {
    const { fixture, http } = crear([producto]);
    const editar = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b: any) => b.textContent.trim() === 'Editar') as HTMLButtonElement;
    editar.click(); fixture.detectChanges(); await fixture.whenStable();
    const ml = fixture.nativeElement.querySelector('input[name="ml"]');
    const select = fixture.nativeElement.querySelector('select[name="concentracion"]');
    expect(ml.value).toBe('100'); expect(select.value).toBe('EDP');
    ml.value = '50'; ml.dispatchEvent(new Event('input'));
    select.value = 'PARFUM'; select.dispatchEvent(new Event('change'));
    fixture.detectChanges(); await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const solicitud = http.expectOne(api + '/1');
    expect(solicitud.request.method).toBe('PUT');
    expect(solicitud.request.body.ml).toBe(50);
    expect(solicitud.request.body.concentracion).toBe('PARFUM');
    solicitud.flush({ ...producto, ml: 50, concentracion: 'PARFUM' });
    await fixture.whenStable();
    http.verify();
  });
});

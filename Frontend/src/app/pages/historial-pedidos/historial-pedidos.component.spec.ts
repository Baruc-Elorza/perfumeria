import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { HistorialPedidosComponent } from './historial-pedidos.component';
import { PagoComponent } from '../pago/pago.component';

@Component({ selector: 'app-pago', standalone: true, template: '<p>Formulario de pago de prueba</p>' })
class PagoPrueba {
  @Input() pedidoId = '';
  @Input() monto = 0;
  @Output() compraConfirmada = new EventEmitter<void>();
}
const api = 'http://localhost:8080/api/pedidos';
const pendiente = { id: '1', estado: 'PENDIENTE', total: 3300, subtotal: 3300, envio: 0, descuento: 0,
  fechaCreacion: '2026-11-28T07:08:00Z', productos: [{ productoId: '1', nombre: 'Jean Paul Gaultier', cantidad: 2, precioUnitario: 1650 }] };

function crear() {
  TestBed.configureTestingModule({ imports: [HistorialPedidosComponent],
    providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
  TestBed.overrideComponent(HistorialPedidosComponent, { remove: { imports: [PagoComponent] }, add: { imports: [PagoPrueba] } });
  const fixture = TestBed.createComponent(HistorialPedidosComponent);
  const http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
  http.expectOne(api).flush([pendiente, { ...pendiente, id: '2', estado: 'PAGADO' }]);
  fixture.detectChanges();
  const boton = (texto: string) => Array.from(fixture.nativeElement.querySelectorAll('button'))
    .find((b: any) => b.textContent.trim().startsWith(texto)) as HTMLButtonElement;
  return { fixture, http, boton };
}

describe('Acciones de pedidos pendientes', () => {
  it('retoma el mismo pedido y muestra el pago confirmado sin crear otro', async () => {
    const { fixture, http, boton } = crear();
    expect(fixture.nativeElement.querySelectorAll('.acciones-pendientes').length).toBe(1);
    boton('Pagar pedido').click(); fixture.detectChanges();
    const pago = fixture.debugElement.query(By.directive(PagoPrueba)).componentInstance as PagoPrueba;
    expect(pago.pedidoId).toBe('1'); expect(pago.monto).toBe(3300);
    http.expectNone('http://localhost:8080/api/compras/pendiente');
    pago.compraConfirmada.emit(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-pago')).toBeNull();
    expect(fixture.nativeElement.querySelector('.acciones-pendientes')).toBeNull();
    expect(fixture.componentInstance.pedidos[0].estado).toBe('PAGADO');
    http.verify();
  });

  it('pide confirmación antes de cancelar y actualiza el estado', () => {
    const { fixture, http, boton } = crear();
    boton('Cancelar pedido').click(); fixture.detectChanges();
    http.expectNone(api + '/1/cancelar');
    boton('Sí, cancelar pedido').click();
    const cancelar = http.expectOne(api + '/1/cancelar');
    expect(cancelar.request.method).toBe('PATCH');
    cancelar.flush({ id: '1', estado: 'CANCELADO' }); fixture.detectChanges();
    expect(fixture.componentInstance.pedidos[0].estado).toBe('CANCELADO');
    expect(fixture.nativeElement.querySelector('.acciones-pendientes')).toBeNull();
    http.verify();
  });

  it('conserva el pedido ante un rechazo y enlaza los productos al catálogo', () => {
    const { fixture, http, boton } = crear();
    boton('Cancelar pedido').click(); fixture.detectChanges();
    boton('Sí, cancelar pedido').click();
    http.expectOne(api + '/1/cancelar').flush({ message: 'El pago está en proceso.' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(fixture.componentInstance.pedidos[0].estado).toBe('PENDIENTE');
    expect(fixture.nativeElement.textContent).toContain('El pago está en proceso.');
    boton('Ver detalle').click();
    http.expectOne(api + '/1').flush(pendiente); fixture.detectChanges();
    const enlace = fixture.nativeElement.querySelector('.enlace-producto');
    expect(enlace.textContent).toBe('Jean Paul Gaultier');
    expect(enlace.getAttribute('href')).toBe('/#coleccion');
    http.verify();
  });
});

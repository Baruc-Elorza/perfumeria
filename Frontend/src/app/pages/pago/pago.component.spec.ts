import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PagoComponent } from './pago.component';
import { PagoService } from '../../services/pago.service';

const mocks = vi.hoisted(() => ({ loadStripe: vi.fn() }));
vi.mock('@stripe/stripe-js', () => ({ loadStripe: mocks.loadStripe }));

describe('Formulario de pago', () => {
  function crear(falla = false) {
    let alEstarListo: () => void = () => {};
    const confirmar = vi.fn().mockResolvedValue({ paymentIntent: { id: 'pi_prueba', status: 'succeeded' } });
    const elemento = {
      mount: vi.fn(),
      destroy: vi.fn(),
      on: vi.fn((evento: string, callback: () => void) => { if (evento === 'ready') alEstarListo = callback; }),
    };
    mocks.loadStripe.mockResolvedValue({
      elements: () => ({ create: () => elemento }),
      confirmPayment: confirmar,
    });
    const servicio = {
      obtenerConfiguracion: vi.fn(() => of({ publicKey: 'pk_test_ficticia' })),
      crearPago: vi.fn(() => falla ? throwError(() => new Error('Servidor no disponible'))
        : of({ clientSecret: 'ficticio', paymentIntentId: 'pi_prueba' })),
      confirmarCompra: vi.fn(() => of({ mensaje: 'Compra confirmada' })),
    };
    TestBed.configureTestingModule({ imports: [PagoComponent], providers: [{ provide: PagoService, useValue: servicio }] });
    const fixture = TestBed.createComponent(PagoComponent);
    fixture.componentRef.setInput('pedidoId', 'pedido-prueba');
    fixture.detectChanges();
    return { fixture, servicio, confirmar, ready: () => alEstarListo() };
  }

  it('espera al formulario de Stripe antes de permitir pagar y confirma el pedido', async () => {
    const { fixture, servicio, confirmar, ready } = crear();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    ready(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(false);
    await fixture.componentInstance.pagar();
    fixture.detectChanges();
    expect(confirmar).toHaveBeenCalledTimes(1);
    expect(servicio.confirmarCompra).toHaveBeenCalledWith('pedido-prueba', 'pi_prueba');
    expect(fixture.nativeElement.textContent).toContain('Compra confirmada');
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('impide pagar cuando el backend no pudo iniciar el pago', async () => {
    const { fixture, confirmar } = crear(true);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('No se pudo iniciar el pago.');
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    expect(confirmar).not.toHaveBeenCalled();
  });
});

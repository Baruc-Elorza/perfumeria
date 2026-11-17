import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  Input
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  loadStripe,
  Stripe,
  StripeElements,
  StripePaymentElement
} from '@stripe/stripe-js';
import { PagoService } from '../../services/pago.service';

@Component({
  selector: 'app-pago',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stripe-pay-wrapper">
      @if (cargando) {
        <p class="loading-text">Cargando pasarela de pago segura...</p>
      }

      <!-- Contenedor donde Stripe renderiza la caja de tarjeta -->
      <div id="payment-element"></div>

      @if (mensaje) {
        <p class="status-message" [class.success]="pagoExitoso" [class.error]="!pagoExitoso">
          {{ mensaje }}
        </p>
      }

      @if (!cargando && !pagoExitoso) {
        <button class="btn-confirmar-pago" (click)="pagar()">
          Confirmar y Pagar
        </button>
      }
    </div>
  `,})
export class PagoComponent implements AfterViewInit {

  @Input() monto: number = 0;
  @Input() pedidoId = '';

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;
  private paymentIntentId: string | null = null;

  mensaje = '';
  cargando = true;
  pagoExitoso = false;

  constructor(
    private pagoService: PagoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {
    this.pagoService.obtenerConfiguracion().subscribe({
      next: async (config) => {
        this.stripe = await loadStripe(config.publicKey);

        if (!this.stripe) {
          this.mensaje = 'No se pudo cargar Stripe.';
          this.cargando = false;
          this.cdr.detectChanges();
          return;
        }

       if (!this.pedidoId) {
         this.mensaje = 'No se recibió el identificador del pedido.';
         this.cargando = false;
         this.cdr.detectChanges();
         return;
       }

       this.pagoService.crearPago(this.pedidoId).subscribe({
          next: (respuesta) => {
            this.paymentIntentId = respuesta.paymentIntentId;

            this.elements = this.stripe!.elements({
              clientSecret: respuesta.clientSecret
            });

            this.paymentElement = this.elements.create('payment');
            this.paymentElement.mount('#payment-element');

            this.cargando = false;
            this.mensaje = '';
            this.cdr.detectChanges();
          },
          error: (error) => {
            console.error('Error al crear PaymentIntent:', error);
            this.mensaje = 'No se pudo iniciar el pago.';
            this.cargando = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (error) => {
        console.error('Error al obtener configuración de Stripe:', error);
        this.mensaje = 'No se pudo conectar con Stripe.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  async pagar(): Promise<void> {
    if (!this.stripe || !this.elements) {
      this.mensaje = 'El sistema de pago todavía no está listo.';
      this.cdr.detectChanges();
      return;
    }

    this.mensaje = 'Procesando pago...';
    this.cdr.detectChanges();

    const resultado = await this.stripe.confirmPayment({
      elements: this.elements,
      redirect: 'if_required'
    });

    if (resultado.error) {
      console.error('Error en el pago:', resultado.error);
      this.mensaje = resultado.error.message || 'No se pudo procesar el pago. Intente nuevamente.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.paymentIntentId) {
      this.mensaje = 'No se pudo identificar el pago.';
      this.cdr.detectChanges();
      return;
    }

        this.pagoService.confirmarCompra(
      this.pedidoId,
      this.paymentIntentId
    ).subscribe({
      next: (respuesta) => {
        this.mensaje = respuesta.mensaje;
        this.pagoExitoso = true;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al confirmar la compra:', error);

        this.mensaje =
          'Stripe procesó el pago, pero no se pudo confirmar el pedido. ' +
          'No vuelvas a pagar; verifica el pedido antes de intentarlo de nuevo.';

        this.cdr.detectChanges();
      }
    });
  }
}

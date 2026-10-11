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
  `,
  styles: [`
    .stripe-pay-wrapper {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .loading-text {
      font-size: 0.9rem;
      color: #6b7280;
      margin: 0;
    }

    #payment-element {
      min-height: 150px;
    }

    .status-message {
      font-size: 0.88rem;
      padding: 10px 14px;
      border-radius: 6px;
      margin: 0;
    }

    .status-message.error {
      background-color: #fef2f2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }

    .status-message.success {
      background-color: #ecfdf5;
      color: #065f46;
      border: 1px solid #a7f3d0;
    }

    .btn-confirmar-pago {
      width: 100%;
      padding: 12px;
      background-color: #111827;
      color: #ffffff;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: background-color 0.2s ease;
    }

    .btn-confirmar-pago:hover {
      background-color: #1f2937;
    }
  `]
})
export class PagoComponent implements AfterViewInit {

  @Input() monto: number = 0;

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

        const montoACobrar = Math.round(Number(this.monto));

        if (!Number.isFinite(montoACobrar) || montoACobrar <= 0) {
          this.mensaje = 'El monto del carrito no es válido.';
          this.cargando = false;
          this.cdr.detectChanges();
          return;
        }
        this.pagoService.crearPago(montoACobrar).subscribe({
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

    this.pagoService.obtenerEstado(this.paymentIntentId).subscribe({
      next: (respuesta) => {
        this.mensaje = `${respuesta.estado}: ${respuesta.mensaje}`;
        if (respuesta.estado === 'PAGADO') {
          this.pagoExitoso = true;
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al verificar estado del pago:', error);
        this.mensaje = 'El pago fue procesado, pero no se pudo verificar su estado.';
        this.cdr.detectChanges();
      }
    });
  }
}

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  Input, Output, EventEmitter, OnDestroy, DestroyRef, inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
        <button type="button" class="btn-confirmar-pago" [disabled]="!listo || procesando" (click)="pagar()">
          {{ procesando ? 'Procesando...' : 'Confirmar y Pagar' }}
        </button>
      }
    </div>
  `,})
export class PagoComponent implements AfterViewInit, OnDestroy {
  @Output() compraConfirmada = new EventEmitter<void>();
  private readonly destroyRef = inject(DestroyRef);
  private destruido = false;

  @Input() monto: number = 0;
  @Input() pedidoId = '';

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;
  private paymentIntentId: string | null = null;

  mensaje = '';
  cargando = true;
  pagoExitoso = false;
  listo = false;
  procesando = false;

  constructor(
    private pagoService: PagoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {
    this.pagoService.obtenerConfiguracion().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: async (config) => {
        try {
          this.stripe = await loadStripe(config.publicKey);
          if (this.destruido) return;
        } catch {
          if (this.destruido) return;
          this.mensaje = 'No se pudo cargar el formulario de pago. Recarga la página e inténtalo de nuevo.';
          this.cargando = false;
          this.cdr.detectChanges();
          return;
        }

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

       this.pagoService.crearPago(this.pedidoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: (respuesta) => {
            if (respuesta.pagoConfirmado) {
              this.pagoExitoso = true;
              this.cargando = false;
              this.mensaje = 'Este pedido ya está pagado.';
              this.cdr.detectChanges();
              this.compraConfirmada.emit();
              return;
            }
            this.paymentIntentId = respuesta.paymentIntentId;

            this.elements = this.stripe!.elements({
              clientSecret: respuesta.clientSecret
            });

            this.paymentElement = this.elements.create('payment');
            this.paymentElement.on('ready', () => {
              this.listo = true;
              this.cdr.detectChanges();
            });
            this.paymentElement.mount('#payment-element');

            this.cargando = false;
            this.mensaje = '';
            this.cdr.detectChanges();
          },
          error: (error) => {
            console.error('Error al crear PaymentIntent:', error);
            this.mensaje = error.error?.message || 'No se pudo iniciar el pago.';
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

  ngOnDestroy(): void {
    this.destruido = true;
    this.paymentElement?.destroy();
  }

  async pagar(): Promise<void> {
    if (this.procesando || this.pagoExitoso) return;
    if (!this.listo || !this.stripe || !this.elements) {
      this.mensaje = 'El sistema de pago todavía no está listo.';
      this.cdr.detectChanges();
      return;
    }

    this.procesando = true;
    this.mensaje = 'Procesando pago...';
    this.cdr.detectChanges();

    let resultado;
    try {
      resultado = await this.stripe.confirmPayment({
        elements: this.elements,
        redirect: 'if_required'
      });
    } catch {
      if (this.destruido) return;
      this.procesando = false;
      this.listo = false;
      this.mensaje = 'No se pudo verificar el pago. Revisa el pedido antes de volver a intentarlo.';
      this.cdr.detectChanges();
      return;
    }

    if (this.destruido) return;
    if (resultado.error) {
      this.procesando = false;
      console.error('Error en el pago:', resultado.error);
      this.mensaje = resultado.error.message || 'No se pudo procesar el pago. Intente nuevamente.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.paymentIntentId) {
      this.procesando = false;
      this.mensaje = 'No se pudo identificar el pago.';
      this.cdr.detectChanges();
      return;
    }

        this.pagoService.confirmarCompra(
      this.pedidoId,
      this.paymentIntentId
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (respuesta) => {
        this.mensaje = respuesta.mensaje;
        this.pagoExitoso = true;
        this.procesando = false;
        this.cdr.detectChanges();
        this.compraConfirmada.emit();
      },
      error: (error) => {
        console.error('Error al confirmar la compra:', error);
        this.procesando = false;
        this.listo = false;

        this.mensaje =
          'Stripe procesó el pago, pero no se pudo confirmar el pedido. ' +
          'No vuelvas a pagar; verifica el pedido antes de intentarlo de nuevo.';

        this.cdr.detectChanges();
      }
    });
  }
}

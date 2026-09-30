import {
  AfterViewInit,
  ChangeDetectorRef,
  Component
} from '@angular/core';

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
  templateUrl: './pago.component.html'
  
})
export class PagoComponent implements AfterViewInit {

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

          this.mensaje =
            'No se pudo cargar Stripe.';

          this.cargando = false;

          this.cdr.detectChanges();

          return;
        }

        this.pagoService.crearPago(50000).subscribe({

          next: (respuesta) => {

            this.paymentIntentId =
              respuesta.paymentIntentId;

            this.elements =
              this.stripe!.elements({
                clientSecret:
                  respuesta.clientSecret
              });

            this.paymentElement =
              this.elements.create('payment');

            this.paymentElement.mount(
              '#payment-element'
            );

            this.cargando = false;
            this.mensaje = '';

            this.cdr.detectChanges();
          },

          error: (error) => {

            console.error(
              'Error al crear PaymentIntent:',
              error
            );

            this.mensaje =
              'No se pudo iniciar el pago.';

            this.cargando = false;

            this.cdr.detectChanges();
          }
        });
      },

      error: (error) => {

        console.error(
          'Error al obtener configuración de Stripe:',
          error
        );

        this.mensaje =
          'No se pudo conectar con Stripe.';

        this.cargando = false;

        this.cdr.detectChanges();
      }
    });
  }

  async pagar(): Promise<void> {

    if (!this.stripe || !this.elements) {

      this.mensaje =
        'El sistema de pago todavía no está listo.';

      this.cdr.detectChanges();

      return;
    }

    this.mensaje =
      'Procesando pago...';

    this.cdr.detectChanges();

    const resultado =
      await this.stripe.confirmPayment({
        elements: this.elements,
        redirect: 'if_required'
      });

    if (resultado.error) {

      console.error(
        'Error en el pago:',
        resultado.error
      );

      this.mensaje =
        resultado.error.message ||
        'No se pudo procesar el pago. Intente nuevamente.';

      this.cdr.detectChanges();

      return;
    }

    console.log(
      'Pago confirmado:',
      resultado
    );

    if (!this.paymentIntentId) {

      this.mensaje =
        'No se pudo identificar el pago.';

      this.cdr.detectChanges();

      return;
    }

    this.pagoService.obtenerEstado(
      this.paymentIntentId
    ).subscribe({

      next: (respuesta) => {

        console.log(
          'Estado del pago:',
          JSON.stringify(
            respuesta,
            null,
            2
          )
        );

        console.log(
          'MENSAJE ANTES:',
          this.mensaje
        );

        this.mensaje =
          `${respuesta.estado}: ${respuesta.mensaje}`;

        if (respuesta.estado === 'PAGADO') {
          this.pagoExitoso = true;
        }

        console.log(
          'MENSAJE DESPUÉS:',
          this.mensaje
        );

        this.cdr.detectChanges();
      },

      error: (error) => {

        console.error(
          'Error al verificar estado del pago:',
          error
        );

        this.mensaje =
          'El pago fue procesado, pero no se pudo verificar su estado.';

        this.cdr.detectChanges();
      }
    });
  }
}
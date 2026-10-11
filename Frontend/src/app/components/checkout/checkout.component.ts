import {
  Component,
  inject,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CarritoService } from '../../services/carrito.service';
import { PagoComponent } from '../../pages/pago/pago.component';

interface DireccionEnvio {
  calle: string;
  colonia: string;
  ciudad: string;
  codigoPostal: string;
  telefono: string;
}

interface RespuestaPedido {
  pedidoId: string;
  estado: string;
  total: number;
}

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    RouterLink,
    FormsModule,
    PagoComponent
  ],
  template: `
    <div class="checkout-wrapper">
      <header class="checkout-header">
        <h1 class="logo">PERFUMERÍA</h1>

        <a routerLink="/carrito" class="btn-regresar">
          ← Volver al carrito
        </a>
      </header>

      <main class="checkout-grid">
        <div class="left-column">

          <section class="checkout-card form-section">
            <h2 class="card-title">1. Dirección de Envío</h2>

            <form
              #direccionForm="ngForm"
              (ngSubmit)="crearPedido()"
            >
              <div class="form-group">
                <label for="calle">Calle y Número</label>
                <input
                  type="text"
                  id="calle"
                  name="calle"
                  [(ngModel)]="direccion.calle"
                  required
                  placeholder="Ej. Av. Hidalgo #123"
                />
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label for="colonia">Colonia / Barrio</label>
                  <input
                    type="text"
                    id="colonia"
                    name="colonia"
                    [(ngModel)]="direccion.colonia"
                    required
                    placeholder="Ej. Centro"
                  />
                </div>

                <div class="form-group">
                  <label for="codigoPostal">Código Postal</label>
                  <input
                    type="text"
                    id="codigoPostal"
                    name="codigoPostal"
                    [(ngModel)]="direccion.codigoPostal"
                    required
                    placeholder="Ej. 68000"
                  />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label for="ciudad">Ciudad / Municipio</label>
                  <input
                    type="text"
                    id="ciudad"
                    name="ciudad"
                    [(ngModel)]="direccion.ciudad"
                    required
                    placeholder="Ej. Oaxaca"
                  />
                </div>

                <div class="form-group">
                  <label for="telefono">Teléfono de contacto</label>
                  <input
                    type="tel"
                    id="telefono"
                    name="telefono"
                    [(ngModel)]="direccion.telefono"
                    required
                    placeholder="Ej. 951 123 4567"
                  />
                </div>
              </div>

              <button
                type="submit"
                [disabled]="
                  direccionForm.invalid ||
                  creandoPedido ||
                  !!pedidoId
                "
              >
                {{
                  creandoPedido
                    ? 'Guardando pedido...'
                    : pedidoId
                      ? 'Pedido guardado'
                      : 'Guardar pedido y continuar'
                }}
              </button>

              <p
                *ngIf="mensaje"
                class="mensaje"
                role="alert"
              >
                {{ mensaje }}
              </p>
            </form>
          </section>

          <section class="checkout-card payment-section">
            <h2 class="card-title">2. Método de Pago</h2>

            <div class="payment-options">
              <label class="payment-option selected">
                <input
                  type="radio"
                  name="metodoPago"
                  value="TARJETA"
                  checked
                  disabled
                />

                <div class="option-content">
                  <strong>
                    Tarjeta de Crédito / Débito (Stripe)
                  </strong>
                  <span>Pago seguro en línea</span>
                </div>
              </label>
            </div>

            <div class="stripe-container">
              <p *ngIf="!pedidoId">
                Primero completa tu dirección y guarda el pedido.
              </p>

              <p *ngIf="pedidoId && total <= 0">
                El pedido se creó, pero el total no es válido.
              </p>

              <app-pago
                *ngIf="pedidoId && total > 0"
                [monto]="total"
              ></app-pago>
            </div>
          </section>
        </div>

        <section class="checkout-card summary-section">
          <h2 class="card-title">Resumen del Pedido</h2>

          <div class="summary-details">
            <div class="detail-row">
              <span class="label">Número de Pedido</span>
              <span class="value">
                {{ pedidoId || 'Pendiente de crear' }}
              </span>
            </div>

            <div class="detail-row">
              <span class="label">Método</span>
              <span class="value">Tarjeta (Stripe)</span>
            </div>

            <div class="detail-row">
              <span class="label">Envío</span>
              <span class="value free-shipping">Gratis</span>
            </div>

            <div class="detail-row total-row">
              <span class="label-total">Total a Pagar</span>
              <span class="value-total">
                {{ total | currency:'MXN':'symbol':'1.2-2' }}
              </span>
            </div>
          </div>
        </section>
      </main>
    </div>
  `,
  styles: [`
    .checkout-wrapper {
      max-width: 1100px;
      margin: 0 auto;
      padding: 40px 24px;
      font-family: system-ui, -apple-system, sans-serif;
      color: #111827;
    }

    .checkout-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 36px;
    }

    .logo {
      font-size: 1.25rem;
      font-weight: 800;
      letter-spacing: 2.5px;
      margin: 0;
    }

    .btn-regresar {
      color: #4b5563;
      text-decoration: none;
      font-size: 0.95rem;
      font-weight: 500;
    }

    .checkout-grid {
      display: grid;
      grid-template-columns: 1fr 380px;
      gap: 32px;
      align-items: start;
    }

    @media (max-width: 850px) {
      .checkout-grid {
        grid-template-columns: 1fr;
      }
    }

    .left-column {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .checkout-card {
      background: #ffffff;
      border-radius: 12px;
      padding: 28px;
      border: 1px solid #e5e7eb;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
    }

    .card-title {
      margin: 0 0 20px 0;
      font-size: 1.2rem;
      font-weight: 700;
      color: #111827;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
      margin-bottom: 16px;
    }

    .form-group label {
      font-size: 0.88rem;
      font-weight: 600;
      color: #374151;
    }

    .form-group input {
      padding: 10px 14px;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 0.95rem;
      outline: none;
      box-sizing: border-box;
      width: 100%;
    }

    .form-group input:focus {
      border-color: #111827;
      box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.1);
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    @media (max-width: 500px) {
      .form-row {
        grid-template-columns: 1fr;
        gap: 0;
      }
    }

    .payment-options {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .payment-option {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 16px;
      border: 1px solid #111827;
      background-color: #f9fafb;
      border-radius: 8px;
    }

    .payment-option input {
      margin-top: 3px;
    }

    .option-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .option-content strong {
      font-size: 0.95rem;
      color: #111827;
    }

    .option-content span {
      font-size: 0.82rem;
      color: #6b7280;
    }

    .stripe-container {
      margin-top: 20px;
      padding: 16px;
      background-color: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
    }

    .summary-details {
      border-top: 1px solid #f3f4f6;
      border-bottom: 1px solid #f3f4f6;
      padding: 16px 0;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      font-size: 0.92rem;
    }

    .label {
      color: #6b7280;
    }

    .value {
      font-weight: 600;
      color: #111827;
      overflow-wrap: anywhere;
      text-align: right;
    }

    .free-shipping {
      color: #059669;
      font-weight: 700;
    }

    .detail-row.total-row {
      margin-bottom: 0;
      padding-top: 12px;
      border-top: 1px dashed #e5e7eb;
    }

    .label-total {
      font-weight: 600;
      font-size: 1rem;
    }

    .value-total {
      font-size: 1.25rem;
      font-weight: 800;
      color: #111827;
      text-align: right;
    }

    button[type="submit"] {
      padding: 12px 18px;
      border: none;
      border-radius: 8px;
      background: #111827;
      color: #ffffff;
      font-weight: 600;
      cursor: pointer;
    }

    button[type="submit"]:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .mensaje {
      margin-top: 12px;
      overflow-wrap: anywhere;
    }
  `]
})
export class CheckoutComponent implements OnInit {
  pedidoId = '';
  total = 0;
  creandoPedido = false;
  mensaje = '';

  direccion: DireccionEnvio = {
    calle: 'Avenida Ferrocarril',
    colonia: 'Vicente Suarez',
    ciudad: 'Oaxaca',
    codigoPostal: '38033',
    telefono: '9514646455'
  };

  private carritoService = inject(CarritoService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.obtenerTotalCarrito();
  }

  obtenerTotalCarrito(): void {
    this.total = 0;
    this.mensaje = '';

    this.carritoService.obtenerCarrito().subscribe({
      next: (carrito) => {
        this.total = Number(carrito.subtotal) || 0;

        if (this.total <= 0) {
          this.mensaje =
            'El carrito está vacío o su subtotal es cero.';
        }

        this.cdr.detectChanges();
      },

      error: (error) => {
        console.error('Error al obtener el carrito:', error);

        this.total = 0;
        this.mensaje = 'No se pudo cargar el carrito.';

        this.cdr.detectChanges();
      }
    });
  }

  crearPedido(): void {
    if (this.creandoPedido || this.pedidoId) {
      return;
    }

    if (
      !this.direccion.calle.trim() ||
      !this.direccion.colonia.trim() ||
      !this.direccion.ciudad.trim() ||
      !this.direccion.codigoPostal.trim() ||
      !this.direccion.telefono.trim()
    ) {
      this.mensaje = 'Completa todos los datos de envío.';
      return;
    }

    if (this.total <= 0) {
      this.mensaje =
        'Agrega productos al carrito antes de continuar.';
      return;
    }

    this.creandoPedido = true;
    this.mensaje = '';
    this.cdr.detectChanges();

    this.http
      .post<RespuestaPedido>(
        'http://localhost:8080/api/compras/pendiente',
        this.direccion,
        { withCredentials: true }
      )
      .subscribe({
        next: (respuesta) => {
          console.log('Respuesta del servidor:', respuesta);

          if (!respuesta?.pedidoId) {
            this.mensaje =
              'El servidor no devolvió el identificador del pedido.';
            this.creandoPedido = false;
            this.cdr.detectChanges();
            return;
          }

          this.pedidoId = respuesta.pedidoId;
          this.total = Number(respuesta.total) || 0;
          this.creandoPedido = false;

          if (this.total <= 0) {
            this.mensaje =
              'El pedido se creó, pero el total recibido no es válido.';
            this.cdr.detectChanges();
            return;
          }

          this.mensaje = 'Pedido guardado correctamente.';
          this.cdr.detectChanges();
        },

        error: (error) => {
          console.error('Error al crear el pedido:', error);

          this.mensaje =
            error.error?.error ||
            'No se pudo guardar el pedido. Verifica tu sesión y que el backend esté activo.';

          this.creandoPedido = false;
          this.cdr.detectChanges();
        },

        complete: () => {
          this.creandoPedido = false;
          this.cdr.detectChanges();
        }
      });
  }
}

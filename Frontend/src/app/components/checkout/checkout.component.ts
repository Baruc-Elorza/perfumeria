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
import { Direccion } from '../../models/direccion.model';
import { DireccionService } from '../../services/direccion.service';

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
        <a routerLink="/carrito" class="btn-regresar">← Volver al carrito</a>
      </header>

      <main class="checkout-grid">
        <div class="left-column">

          <section class="checkout-card form-section">
            <div class="section-title-bar">
              <h2 class="card-title">1. Dirección de Envío</h2>
<a routerLink="/direcciones" class="btn-admin-dir">+ Administrar Direcciones</a>
            </div>

            <!-- Selector de Direcciones Guardadas -->
            <div class="saved-addresses-container" *ngIf="direcciones.length > 0">
              <label class="sub-label">Selecciona una de tus direcciones guardadas:</label>
              <div class="addresses-grid">
                <div *ngFor="let dir of direcciones"
                     (click)="seleccionarDireccion(dir)"
                     [class.selected]="direccionSeleccionadaId === dir.id"
                     class="address-card-item">
                  <input type="radio" name="dirSeleccionada" [checked]="direccionSeleccionadaId === dir.id">
                  <div class="address-info">
                    <strong>{{ dir.calle }} #{{ dir.numeroExterior || 'S/N' }} <span *ngIf="dir.numeroInterior">Int. {{ dir.numeroInterior }}</span></strong>
                    <span>Col. {{ dir.colonia }}, {{ dir.ciudad }}, {{ dir.estado }} - C.P. {{ dir.codigoPostal }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="no-addresses-notice" *ngIf="direcciones.length === 0">
              <p>No tienes direcciones guardadas. Puedes escribirlas abajo o <a routerLink="/direcciones" class="link-inline">registrarlas aquí</a>.</p>
            </div>

            <form #direccionForm="ngForm" (ngSubmit)="crearPedido()">
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
                [disabled]="direccionForm.invalid || creandoPedido || !!pedidoId"
              >
                {{
                  creandoPedido
                    ? 'Guardando pedido...'
                    : pedidoId
                      ? 'Pedido guardado'
                      : 'Guardar pedido y continuar'
                }}
              </button>

              <p *ngIf="mensaje" class="mensaje" role="alert">
                {{ mensaje }}
              </p>
            </form>
          </section>

          <section class="checkout-card payment-section">
            <h2 class="card-title">2. Método de Pago</h2>

            <div class="payment-options">
              <label class="payment-option selected">
                <input type="radio" name="metodoPago" value="TARJETA" checked disabled />
                <div class="option-content">
                  <strong>Tarjeta de Crédito / Débito (Stripe)</strong>
                  <span>Pago seguro en línea</span>
                </div>
              </label>
            </div>

            <div class="stripe-container">
              <p *ngIf="!pedidoId" class="notice-text">
                Primero completa tu dirección y guarda el pedido.
              </p>
              <p *ngIf="pedidoId && total <= 0" class="notice-text">
                El pedido se creó, pero el total no es válido.
              </p>
              <app-pago *ngIf="pedidoId && total > 0" [monto]="total"></app-pago>
            </div>
          </section>
        </div>

        <section class="checkout-card summary-section">
          <h2 class="card-title">Resumen del Pedido</h2>

          <div class="summary-details">
            <div class="detail-row">
              <span class="label">Número de Pedido</span>
              <span class="value">{{ pedidoId || 'Pendiente de crear' }}</span>
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
      background-color: #fcfcfd;
      min-height: 100vh;
    }

    .checkout-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 36px;
      border-bottom: 1px solid #e5e7eb;
      padding-bottom: 16px;
    }

    .logo {
      font-size: 1.25rem;
      font-weight: 800;
      letter-spacing: 2.5px;
      margin: 0;
      color: #111827;
    }

    .btn-regresar {
      color: #4b5563;
      text-decoration: none;
      font-size: 0.95rem;
      font-weight: 500;
      transition: color 0.2s;
    }
    .btn-regresar:hover {
      color: #111827;
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
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
    }

    .section-title-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }

    .card-title {
      margin: 0;
      font-size: 1.2rem;
      font-weight: 700;
      color: #111827;
    }

    .btn-admin-dir {
      font-size: 0.82rem;
      color: #2563eb;
      text-decoration: none;
      font-weight: 600;
    }
    .btn-admin-dir:hover {
      text-decoration: underline;
    }

    .sub-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: #374151;
      display: block;
      margin-bottom: 8px;
    }

    .saved-addresses-container {
      margin-bottom: 20px;
      background: #f9fafb;
      padding: 14px;
      border-radius: 8px;
      border: 1px solid #f3f4f6;
    }

    .addresses-grid {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 200px;
      overflow-y: auto;
    }

    .address-card-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 10px 12px;
      background: #ffffff;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .address-card-item:hover {
      border-color: #9ca3af;
    }

    .address-card-item.selected {
      border-color: #111827;
      background-color: #f8fafc;
      box-shadow: 0 0 0 1px #111827;
    }

    .address-info {
      display: flex;
      flex-direction: column;
      font-size: 0.85rem;
      color: #374151;
    }
    .address-info strong {
      color: #111827;
      font-weight: 600;
    }

    .no-addresses-notice {
      font-size: 0.88rem;
      color: #6b7280;
      margin-bottom: 16px;
      background: #fdf2f2;
      padding: 10px 12px;
      border-radius: 6px;
      border: 1px solid #fde8e8;
    }
    .link-inline {
      color: #d97706;
      font-weight: 600;
      text-decoration: none;
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
      transition: border-color 0.2s, box-shadow 0.2s;
    }

    .form-group input:focus {
      border-color: #111827;
      box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.08);
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

    .notice-text {
      font-size: 0.9rem;
      color: #6b7280;
      margin: 0;
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
      width: 100%;
      margin-top: 8px;
      padding: 12px 18px;
      border: none;
      border-radius: 8px;
      background: #111827;
      color: #ffffff;
      font-weight: 600;
      cursor: pointer;
      transition: background-color 0.2s;
    }

    button[type="submit"]:hover:not(:disabled) {
      background: #1f2937;
    }

    button[type="submit"]:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .mensaje {
      margin-top: 12px;
      font-size: 0.9rem;
      color: #dc2626;
      overflow-wrap: anywhere;
    }
  `]
})
export class CheckoutComponent implements OnInit {
  pedidoId = '';
  total = 0;
  creandoPedido = false;
  mensaje = '';

  usuarioId = 1; // ID de usuario por defecto para pruebas
  direcciones: Direccion[] = [];
  direccionSeleccionadaId?: number;

  direccion = {
    calle: '',
    colonia: '',
    ciudad: '',
    codigoPostal: '',
    telefono: ''
  };

  private carritoService = inject(CarritoService);
  private direccionService = inject(DireccionService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.obtenerTotalCarrito();
    this.cargarDirecciones();
  }

  cargarDirecciones(): void {
    this.direccionService.obtenerDireccionesPorUsuario(this.usuarioId).subscribe({
      next: (data) => {
        this.direcciones = data;
        if (this.direcciones.length > 0) {
          this.seleccionarDireccion(this.direcciones[0]);
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al cargar direcciones en checkout:', err);
      }
    });
  }

seleccionarDireccion(dir: Direccion): void {
    this.direccionSeleccionadaId = dir.id;
    this.direccion = {
      calle: `${dir.calle}${dir.numeroExterior ? ' #' + dir.numeroExterior : ''}`,
      colonia: dir.colonia,
      ciudad: dir.ciudad,
      codigoPostal: dir.codigoPostal,
      telefono: dir.telefono || '' // <-- Se auto-rellena con el teléfono guardado
    };
    this.cdr.detectChanges();
  }

  obtenerTotalCarrito(): void {
    this.total = 0;
    this.mensaje = '';

    this.carritoService.obtenerCarrito().subscribe({
      next: (carrito) => {
        this.total = Number(carrito.subtotal) || 0;

        if (this.total <= 0) {
          this.mensaje = 'El carrito está vacío o su subtotal es cero.';
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
      this.mensaje = 'Agrega productos al carrito antes de continuar.';
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
            this.mensaje = 'El servidor no devolvió el identificador del pedido.';
            this.creandoPedido = false;
            this.cdr.detectChanges();
            return;
          }

          this.pedidoId = respuesta.pedidoId;
          this.total = Number(respuesta.total) || 0;
          this.creandoPedido = false;

          if (this.total <= 0) {
            this.mensaje = 'El pedido se creó, pero el total recibido no es válido.';
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

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
              <app-pago
                *ngIf="pedidoId && total > 0"
                [monto]="total"
                [pedidoId]="pedidoId">
              </app-pago>
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
  `,})
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

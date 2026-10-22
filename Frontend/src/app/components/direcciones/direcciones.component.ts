import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Direccion } from '../../models/direccion.model';
import { DireccionService } from '../../services/direccion.service';

@Component({
  selector: 'app-direcciones',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="dir-wrapper">
      <header class="dir-header">
        <h1 class="logo">PERFUMERÍA</h1>
        <a routerLink="/checkout" class="btn-regresar">← Volver a procesar pago</a>
      </header>

      <main class="dir-main-grid">
        <!-- Formulario dinámico (Crear / Editar) -->
        <section class="dir-card">
          <div class="form-header-title">
            <h2 class="card-title">{{ modoEdicion ? 'Editar Dirección' : 'Agregar Nueva Dirección' }}</h2>
            <button *ngIf="modoEdicion" type="button" class="btn-cancelar-edicion" (click)="cancelarEdicion(dirForm)">Cancelar</button>
          </div>

          <form (ngSubmit)="guardarDireccion(dirForm)" #dirForm="ngForm" class="dir-form">

            <!-- Calle -->
            <div class="form-group">
              <label for="calle">Calle y Número / Referencia *</label>
              <input
                type="text"
                id="calle"
                name="calle"
                [ngModel]="nuevaDireccion.calle"
                (ngModelChange)="nuevaDireccion.calle = capitalizarPalabras($event)"
                required
                minlength="3"
                #calle="ngModel"
                placeholder="Ej. Av. Juárez #123">
              <div *ngIf="intentoEnvio && calle.invalid" class="error-msg">
                <small *ngIf="calle.errors?.['required']">La calle es obligatoria.</small>
                <small *ngIf="calle.errors?.['minlength']">Debe tener al menos 3 caracteres.</small>
              </div>
            </div>

            <!-- Números Exterior e Interior -->
            <div class="form-row">
              <div class="form-group">
                <label for="numExt">Nº Exterior</label>
                <input
                  type="text"
                  id="numExt"
                  name="numeroExterior"
                  [ngModel]="nuevaDireccion.numeroExterior"
                  (ngModelChange)="nuevaDireccion.numeroExterior = capitalizarPalabras($event)"
                  placeholder="Ej. 123 o S/N">
              </div>
              <div class="form-group">
                <label for="numInt">Nº Interior</label>
                <input
                  type="text"
                  id="numInt"
                  name="numeroInterior"
                  [ngModel]="nuevaDireccion.numeroInterior"
                  (ngModelChange)="nuevaDireccion.numeroInterior = capitalizarPalabras($event)"
                  placeholder="Ej. A-4">
              </div>
            </div>

            <!-- Colonia -->
            <div class="form-group">
              <label for="colonia">Colonia / Barrio *</label>
              <input
                type="text"
                id="colonia"
                name="colonia"
                [ngModel]="nuevaDireccion.colonia"
                (ngModelChange)="nuevaDireccion.colonia = capitalizarPalabras($event)"
                required
                minlength="3"
                #colonia="ngModel"
                placeholder="Ej. Centro">
              <div *ngIf="intentoEnvio && colonia.invalid" class="error-msg">
                <small *ngIf="colonia.errors?.['required']">La colonia es obligatoria.</small>
                <small *ngIf="colonia.errors?.['minlength']">Debe tener al menos 3 caracteres.</small>
              </div>
            </div>

            <!-- Ciudad y Estado -->
            <div class="form-row">
              <div class="form-group">
                <label for="ciudad">Ciudad *</label>
                <input
                  type="text"
                  id="ciudad"
                  name="ciudad"
                  [ngModel]="nuevaDireccion.ciudad"
                  (ngModelChange)="nuevaDireccion.ciudad = capitalizarPalabras($event)"
                  required
                  minlength="3"
                  #ciudad="ngModel"
                  placeholder="Ej. Oaxaca">
                <div *ngIf="intentoEnvio && ciudad.invalid" class="error-msg">
                  <small *ngIf="ciudad.errors?.['required']">La ciudad es obligatoria.</small>
                </div>
              </div>
              <div class="form-group">
                <label for="estado">Estado *</label>
                <input
                  type="text"
                  id="estado"
                  name="estado"
                  [ngModel]="nuevaDireccion.estado"
                  (ngModelChange)="nuevaDireccion.estado = capitalizarPalabras($event)"
                  required
                  minlength="3"
                  #estado="ngModel"
                  placeholder="Ej. Oaxaca">
                <div *ngIf="intentoEnvio && estado.invalid" class="error-msg">
                  <small *ngIf="estado.errors?.['required']">El estado es obligatorio.</small>
                </div>
              </div>
            </div>

            <!-- Código Postal y Teléfono -->
            <div class="form-row">
              <div class="form-group">
                <label for="cp">Código Postal *</label>
                <input
                  type="text"
                  id="cp"
                  name="codigoPostal"
                  [(ngModel)]="nuevaDireccion.codigoPostal"
                  required
                  pattern="^[0-9]{5}$"
                  #cp="ngModel"
                  placeholder="Ej. 68000">
                <div *ngIf="intentoEnvio && cp.invalid" class="error-msg">
                  <small *ngIf="cp.errors?.['required']">El C.P. es obligatorio.</small>
                  <small *ngIf="cp.errors?.['pattern']">Debe ser de 5 dígitos.</small>
                </div>
              </div>

              <div class="form-group">
                <label for="tel">Teléfono de contacto *</label>
                <input
                  type="tel"
                  id="tel"
                  name="telefono"
                  [(ngModel)]="nuevaDireccion.telefono"
                  required
                  pattern="^[0-9]{10}$"
                  #tel="ngModel"
                  placeholder="Ej. 9511234567">
                <div *ngIf="intentoEnvio && tel.invalid" class="error-msg">
                  <small *ngIf="tel.errors?.['required']">El teléfono es obligatorio.</small>
                  <small *ngIf="tel.errors?.['pattern']">Debe ser de 10 dígitos.</small>
                </div>
              </div>
            </div>

            <!-- Referencias -->
            <div class="form-group">
              <label for="ref">Referencias opcionales</label>
              <textarea
                id="ref"
                name="referencias"
                [ngModel]="nuevaDireccion.referencias"
                (ngModelChange)="nuevaDireccion.referencias = capitalizarPalabras($event)"
                rows="2"
                placeholder="Color de fachada, entre calles...">
              </textarea>
            </div>

            <button type="submit" class="btn-submit">
              {{ modoEdicion ? 'Actualizar Dirección' : 'Guardar Dirección' }}
            </button>

            <p *ngIf="mensajeExito" class="success-msg">{{ mensajeExito }}</p>
          </form>
        </section>

        <!-- Listado -->
        <section class="dir-card">
          <h2 class="card-title">Tus Direcciones Registradas</h2>

          <div *ngIf="direcciones.length === 0" class="empty-notice">
            No tienes direcciones registradas todavía. Agrega una llenando el formulario.
          </div>

          <div class="addresses-list" *ngIf="direcciones.length > 0">
            <div *ngFor="let dir of direcciones" class="address-item">
              <div class="address-details">
                <strong>{{ dir.calle }} #{{ dir.numeroExterior || 'S/N' }} <span *ngIf="dir.numeroInterior">Int. {{ dir.numeroInterior }}</span></strong>
                <span>Col. {{ dir.colonia }}, {{ dir.ciudad }}, {{ dir.estado }}</span>
                <span class="cp-text">C.P. {{ dir.codigoPostal }} | Tel: {{ dir.telefono || 'No registrado' }}</span>
                <small class="ref-text" *ngIf="dir.referencias">Ref: {{ dir.referencias }}</small>
              </div>
              <div class="action-buttons">
                <button class="btn-edit" (click)="cargarParaEditar(dir)">Editar</button>
                <button class="btn-delete" (click)="eliminarDireccion(dir.id)">Eliminar</button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  `,})
export class DireccionesComponent implements OnInit {
  direcciones: Direccion[] = [];
  usuarioId: number = 1;
  intentoEnvio = false;
  mensajeExito = '';
  modoEdicion = false;
  direccionIdEnEdicion?: number;

  nuevaDireccion: Direccion = {
    usuarioId: this.usuarioId,
    calle: '',
    colonia: '',
    ciudad: '',
    estado: '',
    codigoPostal: '',
    telefono: ''
  };

  private direccionService = inject(DireccionService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.cargarDirecciones();
  }

  // Función para capitalizar la primera letra de cada palabra
  capitalizarPalabras(valor: string): string {
    if (!valor) return '';
    return valor
      .toLowerCase()
      .split(' ')
      .map(palabra => palabra.charAt(0).toUpperCase() + palabra.slice(1))
      .join(' ');
  }

  cargarDirecciones(): void {
    this.direccionService.obtenerDireccionesPorUsuario(this.usuarioId).subscribe({
      next: (data) => {
        this.direcciones = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al cargar las direcciones:', err);
      }
    });
  }

  cargarParaEditar(dir: Direccion): void {
    this.modoEdicion = true;
    this.direccionIdEnEdicion = dir.id;
    this.intentoEnvio = false;
    this.nuevaDireccion = { ...dir };
    this.cdr.detectChanges();
  }

  cancelarEdicion(form: NgForm): void {
    this.modoEdicion = false;
    this.direccionIdEnEdicion = undefined;
    this.resetearFormulario(form);
  }

  guardarDireccion(form: NgForm): void {
    this.intentoEnvio = true;

    const regexCalle = /^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\s.,#-]+$/;
    const regexTexto = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]+$/;

    const calleValida = this.nuevaDireccion.calle && regexCalle.test(this.nuevaDireccion.calle.trim()) && this.nuevaDireccion.calle.trim().length >= 3;
    const coloniaValida = this.nuevaDireccion.colonia && regexTexto.test(this.nuevaDireccion.colonia.trim()) && this.nuevaDireccion.colonia.trim().length >= 3;
    const ciudadValida = this.nuevaDireccion.ciudad && regexTexto.test(this.nuevaDireccion.ciudad.trim()) && this.nuevaDireccion.ciudad.trim().length >= 3;
    const estadoValido = this.nuevaDireccion.estado && regexTexto.test(this.nuevaDireccion.estado.trim()) && this.nuevaDireccion.estado.trim().length >= 3;
    const cpValido = this.nuevaDireccion.codigoPostal && /^[0-9]{5}$/.test(this.nuevaDireccion.codigoPostal.trim());
    const telValido = this.nuevaDireccion.telefono && /^[0-9]{10}$/.test(this.nuevaDireccion.telefono.trim());

    if (!calleValida || !coloniaValida || !ciudadValida || !estadoValido || !cpValido || !telValido) {
      return;
    }

    this.nuevaDireccion.usuarioId = this.usuarioId;

    if (this.modoEdicion && this.direccionIdEnEdicion) {
      this.nuevaDireccion.id = this.direccionIdEnEdicion;
      this.direccionService.guardarDireccion(this.nuevaDireccion).subscribe({
        next: () => {
          this.cargarDirecciones();
          this.cancelarEdicion(form);
          this.mensajeExito = '¡Dirección actualizada correctamente!';
          setTimeout(() => {
            this.mensajeExito = '';
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          console.error('Error al actualizar la dirección:', err);
        }
      });
    } else {
      this.direccionService.guardarDireccion(this.nuevaDireccion).subscribe({
        next: () => {
          this.cargarDirecciones();
          this.resetearFormulario(form);
          this.mensajeExito = '¡Dirección guardada correctamente!';
          setTimeout(() => {
            this.mensajeExito = '';
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          console.error('Error al guardar la dirección:', err);
        }
      });
    }
  }

  eliminarDireccion(id?: number): void {
    if (id) {
      this.direccionService.eliminarDireccion(id).subscribe({
        next: () => {
          this.cargarDirecciones();
        },
        error: (err) => {
          console.error('Error al eliminar la dirección:', err);
        }
      });
    }
  }

  resetearFormulario(form: NgForm): void {
    this.intentoEnvio = false;
    form.resetForm();
    this.nuevaDireccion = {
      usuarioId: this.usuarioId,
      calle: '',
      numeroExterior: '',
      numeroInterior: '',
      colonia: '',
      ciudad: '',
      estado: '',
      codigoPostal: '',
      telefono: '',
      referencias: '',
      esPrincipal: false
    };
    this.cdr.detectChanges();
  }
}

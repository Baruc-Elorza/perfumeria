import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PerfumeService } from '../../services/perfume.service';
import { Perfume } from '../../models/perfume.model';

@Component({
  selector: 'app-admin-perfumes',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './admin-perfumes.component.html',
  styleUrl: './admin-perfumes.component.css'
})
export class AdminPerfumesComponent implements OnInit {
  private readonly servicio = inject(PerfumeService);
  private readonly destroyRef = inject(DestroyRef);
  readonly perfumes = signal<Perfume[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly pendienteEliminar = signal<Perfume | null>(null);
  readonly detalle = signal<Perfume | null>(null);
  editandoId: number | null = null;
  nuevoPerfume: Perfume = this.vacio();

  ngOnInit(): void { this.cargarPerfumes(); }

  private vacio(): Perfume {
    return { nombre: '', marca: '', precio: 0, stock: 0, descripcion: '',
      imagenUrl: '', notasTop: '', notasMiddle: '', notasBase: '' };
  }

  cargarPerfumes(): void {
    this.cargando.set(true);
    this.error.set('');
    this.servicio.getPerfumes().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: productos => { this.perfumes.set(productos); this.cargando.set(false); },
      error: () => { this.error.set('No se pudo cargar el inventario. Intenta actualizar la lista.'); this.cargando.set(false); }
    });
  }

  editar(perfume: Perfume, formulario: NgForm): void {
    if (this.guardando() || perfume.id == null) return;
    this.editandoId = perfume.id;
    this.nuevoPerfume = { ...perfume };
    formulario.resetForm(this.nuevoPerfume);
    this.pendienteEliminar.set(null);
    this.error.set('');
    this.mensaje.set('');
  }

  cancelar(formulario: NgForm): void {
    this.editandoId = null;
    this.nuevoPerfume = this.vacio();
    formulario.resetForm(this.nuevoPerfume);
  }

  guardarPerfume(formulario: NgForm): void {
    if (this.guardando() || this.cargando()) return;
    const datos = { ...this.nuevoPerfume, nombre: this.nuevoPerfume.nombre.trim(),
      marca: this.nuevoPerfume.marca.trim() };
    if (formulario.invalid || !datos.nombre || !datos.marca || datos.precio == null
      || !Number.isFinite(datos.precio) || datos.precio < 0 || datos.stock == null
      || !Number.isInteger(datos.stock) || datos.stock < 0) {
      this.error.set('Completa nombre y marca, un precio no negativo y existencias enteras no negativas.');
      return;
    }
    const editando = this.editandoId !== null;
    const solicitud = this.editandoId === null ? this.servicio.agregarPerfume(datos)
      : this.servicio.actualizarPerfume(this.editandoId, datos);
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');
    solicitud.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: guardado => {
        this.perfumes.update(lista => editando ? lista.map(p => p.id === guardado.id ? guardado : p) : [...lista, guardado]);
        if (this.detalle()?.id === guardado.id) this.detalle.set(guardado);
        this.guardando.set(false);
        this.cancelar(formulario);
        this.mensaje.set(editando ? 'Perfume actualizado.' : 'Perfume agregado al catálogo.');
      },
      error: err => { this.guardando.set(false); this.error.set(err.error?.message || 'No se pudo guardar el perfume. Inténtalo de nuevo.'); }
    });
  }

  eliminarPerfume(formulario: NgForm): void {
    const perfume = this.pendienteEliminar();
    if (this.guardando() || this.cargando() || perfume?.id == null) return;
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');
    this.servicio.eliminarPerfume(perfume.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.perfumes.update(lista => lista.filter(p => p.id !== perfume.id));
        if (this.editandoId === perfume.id) this.cancelar(formulario);
        if (this.detalle()?.id === perfume.id) this.detalle.set(null);
        this.pendienteEliminar.set(null);
        this.guardando.set(false);
        this.mensaje.set('Perfume eliminado.');
      },
      error: err => { this.guardando.set(false); this.pendienteEliminar.set(null);
        this.error.set(err.error?.message || 'No se pudo eliminar el perfume. Inténtalo de nuevo.'); }
    });
  }
}

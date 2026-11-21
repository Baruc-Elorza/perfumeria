import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, map, of, startWith, switchMap, timer } from 'rxjs';
import { Perfume } from '../../models/perfume.model';
import { PerfumeService } from '../../services/perfume.service';
import { CarritoService } from '../../services/carrito.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-catalogo-perfumes',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './catalogo-perfumes.component.html',
})
export class CatalogoPerfumesComponent implements OnInit {
  readonly perfumesFiltrados = signal<Perfume[]>([]);
  readonly cargando = signal(true);
  readonly errorCarga = signal(false);
  readonly agregando = signal<number | null>(null);
  readonly errorCompra = signal('');
  readonly detalle = signal<Perfume | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly errorDetalle = signal('');
  @ViewChild('ventanaDetalle', { static: true }) ventanaDetalle!: ElementRef<HTMLDialogElement>;
  private readonly seleccion = new Subject<Perfume | null>();
  textoBusqueda = '';
  private readonly cambios = new Subject<string>();
  private readonly perfumes = inject(PerfumeService);
  private readonly carrito = inject(CarritoService);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.seleccion.pipe(
      switchMap(perfume => perfume?.id == null ? EMPTY : this.perfumes.getPerfumePorId(perfume.id).pipe(
        map(producto => ({ producto, error: false })),
        catchError(() => of({ producto: perfume, error: true }))
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(({ producto, error }) => {
      this.detalle.set(producto);
      this.cargandoDetalle.set(false);
      this.errorDetalle.set(error ? 'No se pudieron cargar los detalles del perfume.' : '');
    });
    this.auth.sesionActual().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    this.cambios.pipe(
      startWith(''),
      switchMap(nombre => {
        this.cargando.set(true);
        this.errorCarga.set(false);
        // Cancelar respuestas anteriores evita resultados desactualizados al escribir rápido.
        return timer(nombre ? 250 : 0).pipe(
          switchMap(() => this.perfumes.buscarPorNombre(nombre)),
          map(perfumes => ({ perfumes, error: false })),
          catchError(() => of({ perfumes: [] as Perfume[], error: true }))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(resultado => {
      this.perfumesFiltrados.set(resultado.perfumes);
      this.errorCarga.set(resultado.error);
      this.cargando.set(false);
    });
  }

  abrirDetalle(perfume: Perfume): void {
    this.detalle.set(perfume);
    this.errorDetalle.set('');
    this.errorCompra.set('');
    this.cargandoDetalle.set(true);
    if (!this.ventanaDetalle.nativeElement.open) this.ventanaDetalle.nativeElement.showModal();
    this.seleccion.next(perfume);
  }

  cerrarDetalle(): void {
    this.seleccion.next(null);
    this.ventanaDetalle.nativeElement.close();
    this.detalle.set(null);
    this.cargandoDetalle.set(false);
    this.errorDetalle.set('');
  }

  cerrarAlFondo(evento: MouseEvent): void {
    const ventana = this.ventanaDetalle.nativeElement;
    const limites = ventana.getBoundingClientRect();
    if (evento.target === ventana && (evento.clientX < limites.left || evento.clientX > limites.right
        || evento.clientY < limites.top || evento.clientY > limites.bottom)) this.cerrarDetalle();
  }

  moverZoom(evento: MouseEvent): void {
    const marco = evento.currentTarget as HTMLElement;
    const limites = marco.getBoundingClientRect();
    marco.style.setProperty('--zoom-x', `${(evento.clientX - limites.left) / limites.width * 100}%`);
    marco.style.setProperty('--zoom-y', `${(evento.clientY - limites.top) / limites.height * 100}%`);
  }

  cerrarSesion(): void {
    this.auth.cerrarSesion().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => void this.router.navigate(['/']),
      error: () => this.auth.usuario.set(null),
    });
  }

  buscar(texto: string): void {
    this.textoBusqueda = texto;
    this.cambios.next(texto.trim());
  }

  comprar(perfume: Perfume): void {
    if (perfume.id == null || !(perfume.stock > 0) || this.agregando() !== null) return;
    this.agregando.set(perfume.id);
    this.errorCompra.set('');
    this.carrito.agregar(perfume.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.agregando.set(null);
        this.cerrarDetalle();
        void this.router.navigate(['/carrito']);
      },
      error: err => {
        this.agregando.set(null);
        this.errorCompra.set(err.error?.message || 'No se pudo agregar el perfume. Intenta nuevamente.');
        this.buscar(this.textoBusqueda);
      }
    });
  }
}

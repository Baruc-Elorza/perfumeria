import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, map, of, startWith, switchMap, timer } from 'rxjs';
import { Perfume } from '../../models/perfume.model';
import { PerfumeService } from '../../services/perfume.service';
import { CarritoService } from '../../services/carrito';

@Component({
  selector: 'app-catalogo-perfumes',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './catalogo-perfumes.component.html',
  styleUrl: './catalogo-perfumes.component.css'
})
export class CatalogoPerfumesComponent implements OnInit {
  readonly perfumesFiltrados = signal<Perfume[]>([]);
  readonly cargando = signal(true);
  readonly errorCarga = signal(false);
  readonly agregando = signal<number | null>(null);
  readonly errorCompra = signal('');
  textoBusqueda = '';
  private readonly cambios = new Subject<string>();
  private readonly perfumes = inject(PerfumeService);
  private readonly carrito = inject(CarritoService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
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

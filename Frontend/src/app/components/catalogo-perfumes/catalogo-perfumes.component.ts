import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Perfume } from '../../models/perfume.model';
import { PerfumeService } from '../../services/perfume.service';

@Component({
  selector: 'app-catalogo-perfumes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo-perfumes.component.html',
  styleUrl: './catalogo-perfumes.component.css'
})
export class CatalogoPerfumesComponent implements OnInit {
  perfumes: Perfume[] = [];
  perfumesFiltrados: Perfume[] = [];
  textoBusqueda = '';
  cargando = true;
  errorCarga = false;

  constructor(private perfumeService: PerfumeService) {}

  ngOnInit(): void {
    this.perfumeService.getPerfumes().subscribe({
      next: (perfumes) => {
        this.perfumes = perfumes;
        this.perfumesFiltrados = perfumes;
        this.cargando = false;
      },
      error: () => {
        this.errorCarga = true;
        this.cargando = false;
      }
    });
  }

  buscar(): void {
    const nombre = this.textoBusqueda.trim().toLocaleLowerCase('es');

    this.perfumesFiltrados = nombre
      ? this.perfumes.filter((perfume) =>
          perfume.nombre.toLocaleLowerCase('es').includes(nombre)
        )
      : this.perfumes;
  }
}

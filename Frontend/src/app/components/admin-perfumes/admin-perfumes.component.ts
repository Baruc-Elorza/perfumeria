import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PerfumeService } from '../../services/perfume.service';
import { Perfume } from '../../models/perfume.model';

@Component({
  selector: 'app-admin-perfumes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-perfumes.component.html'
})
export class AdminPerfumesComponent implements OnInit {
  perfumes: Perfume[] = [];
  nuevoPerfume: Perfume = {
    nombre: '',
    marca: '',
    precio: 0,
    stock: 0,
    imagenUrl: ''
  };

  constructor(private perfumeService: PerfumeService) {}

  ngOnInit(): void {
    this.cargarPerfumes();
  }

  cargarPerfumes(): void {
    this.perfumeService.getPerfumes().subscribe(data => {
      this.perfumes = data;
    });
  }

  guardarPerfume(): void {
    if (!this.nuevoPerfume.nombre || !this.nuevoPerfume.marca) return;

    this.perfumeService.agregarPerfume(this.nuevoPerfume).subscribe(() => {
      this.cargarPerfumes();
      this.limpiarFormulario();
    });
  }

  eliminarPerfume(id?: number): void {
    if (id) {
      this.perfumeService.eliminarPerfume(id).subscribe(() => {
        this.cargarPerfumes();
      });
    }
  }

  limpiarFormulario(): void {
    this.nuevoPerfume = { nombre: '', marca: '', precio: 0, stock: 0, imagenUrl: '' };
  }
}

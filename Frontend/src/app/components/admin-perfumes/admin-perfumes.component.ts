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

  // H.U.-09: Variable para almacenar el detalle del perfume seleccionado
  perfumeSeleccionado: Perfume | null = null;

  nuevoPerfume: Perfume = {
    nombre: '',
    marca: '',
    descripcion: '',
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

  // H.U.-09: Método para consultar el detalle de un perfume por su ID
  verDetalle(id?: number): void {
    if (id) {
      this.perfumeService.getPerfumePorId(id).subscribe({
        next: (data) => {
          this.perfumeSeleccionado = data;
        },
        error: (err) => {
          console.error('Error al obtener el detalle del perfume', err);
        }
      });
    }
  }

  // H.U.-09: Método para cerrar la tarjeta o modal de detalle
  cerrarDetalle(): void {
    this.perfumeSeleccionado = null;
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
        if (this.perfumeSeleccionado?.id === id) {
          this.cerrarDetalle();
        }
      });
    }
  }

  limpiarFormulario(): void {
    this.nuevoPerfume = {
      nombre: '',
      marca: '',
      descripcion: '',
      precio: 0,
      stock: 0,
      imagenUrl: ''
    };
  }
}

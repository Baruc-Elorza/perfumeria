import { Component, OnInit } from '@angular/core';
import { CarritoService } from '../../services/carrito';

@Component({
  imports: [],
  selector: 'app-carrito',
  standalone: true,
  styleUrl: './carrito.css',
  templateUrl: './carrito.html',
})
export class Carrito implements OnInit {
  carrito: any[] = [];
  constructor(private carritoService: CarritoService){}

  ngOnInit(): void {
    this.carritoService.obtenerCarrito().subscribe({
      next: (respuesta) => {
        console.log(respuesta);
        this.carrito = respuesta;
      },

      error:(error) =>{
        console.error('Error al obtenner carrito', error);
      }
  });
  }

  getTotal(): number {

    return this.carrito.reduce(
        (total, producto) =>
            total + (producto.precio * producto.cantidad),
        0
    );

}
}
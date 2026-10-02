export interface LineaCarrito {
  perfumeId: number;
  nombre: string;
  marca: string;
  imagenUrl?: string;
  precio: number;
  stock: number;
  cantidad: number;
}

export interface CarritoRespuesta {
  items: LineaCarrito[];
  subtotal: number;
}

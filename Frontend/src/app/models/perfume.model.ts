export interface Perfume {
  id?: number;
  nombre: string;
  marca: string;
  descripcion?: string;
  notasTop?: string;
  notasMiddle?: string;
  notasBase?: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
}

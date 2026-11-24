export interface Perfume {
  id?: number;
  nombre: string;
  marca: string;
  descripcion?: string;
  ml?: number | null;
  concentracion?: 'EDF' | 'EDT' | 'EDP' | 'PARFUM' | null;
  notasTop?: string;
  notasMiddle?: string;
  notasBase?: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
}

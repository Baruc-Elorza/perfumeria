export interface Direccion {
  id?: number;
  calle: string;
  numeroExterior?: string;
  numeroInterior?: string;
  colonia: string;
  ciudad: string;
  estado: string;
  codigoPostal: string;
  telefono?: string;
  referencias?: string;
  esPrincipal?: boolean;
}

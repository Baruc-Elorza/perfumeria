export interface UsuarioAdmin {
  id: number;
  nombre: string;
  email: string;
  activo: boolean;
  administrador: boolean;
  puedeCambiarEstado: boolean;
}

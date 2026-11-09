import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UsuarioAdmin } from '../models/usuario-admin.model';

@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {
  private readonly http = inject(HttpClient);
  private readonly api = 'http://localhost:8080/api/admin/usuarios';

  listar() { return this.http.get<UsuarioAdmin[]>(this.api, { withCredentials: true }); }
  detalle(id: number) { return this.http.get<UsuarioAdmin>(`${this.api}/${id}`, { withCredentials: true }); }
  cambiarEstado(id: number, activo: boolean) {
    return this.http.patch<UsuarioAdmin>(`${this.api}/${id}/estado`, { activo }, { withCredentials: true });
  }
}

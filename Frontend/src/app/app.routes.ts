import { Routes } from '@angular/router';

import { AdminPerfumesComponent } from './components/admin-perfumes/admin-perfumes.component';
import { CatalogoPerfumesComponent } from './components/catalogo-perfumes/catalogo-perfumes.component';
import { Carrito } from './pages/carrito/carrito';
import { PagoComponent } from './pages/pago/pago.component';

export const routes: Routes = [
  {
    path: '',
    component: CatalogoPerfumesComponent,
    pathMatch: 'full'
  },
  {
    path: 'carrito',
    component: Carrito
  },
  {
    path: 'admin/perfumes',
    component: AdminPerfumesComponent
  },
  {
    path: 'pago',
    component: PagoComponent
  },
  {
    path: '**',
    redirectTo: ''
  }
];
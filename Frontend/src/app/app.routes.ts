import { Routes } from '@angular/router';

import { PagoComponent } from './pages/pago/pago.component';

import { Carrito } from './pages/carrito/carrito';

export const routes: Routes = [
  {
    path: 'pago',
    component: PagoComponent
  },
  {
    path: 'carrito',
    component: Carrito
  }
];
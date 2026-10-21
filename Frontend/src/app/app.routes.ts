import { Routes } from '@angular/router';
import { AdminPerfumesComponent } from './pages/admin-perfumes/admin-perfumes.component';
import { Carrito } from './pages/carrito/carrito';
import { CatalogoPerfumesComponent } from './pages/catalogo-perfumes/catalogo-perfumes.component';
import { CheckoutComponent } from './components/checkout/checkout.component'; // 1. Importar el componente
import { LoginComponent } from './pages/login/login.component';
import { authGuard } from './services/auth.guard';
import { HistorialPedidosComponent } from './pages/historial-pedidos/historial-pedidos.component';
import { DireccionesComponent } from './components/direcciones/direcciones.component';
import { AdminPedidosComponent } from './pages/admin-pedidos/admin-pedidos.component';
import { adminGuard } from './services/admin.guard';

export const routes: Routes = [
    { path: '', component: CatalogoPerfumesComponent, pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    { path: 'carrito', component: Carrito, canActivate: [authGuard] },
    { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard] },
    { path: 'direcciones', component: DireccionesComponent, canActivate: [authGuard] },
    { path: 'admin/perfumes', component: AdminPerfumesComponent, canActivate: [adminGuard] },
    { path: 'admin/pedidos', component: AdminPedidosComponent, canActivate: [adminGuard] },
    { path: 'mis-pedidos', component: HistorialPedidosComponent, canActivate: [authGuard] },
    { path: '**', redirectTo: '' } // Siempre debe ir al final
];

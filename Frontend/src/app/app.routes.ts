import { Routes } from '@angular/router';
import { AdminPerfumesComponent } from './pages/admin-perfumes/admin-perfumes.component';
import { Carrito } from './pages/carrito/carrito';
import { CatalogoPerfumesComponent } from './pages/catalogo-perfumes/catalogo-perfumes.component';
import { CheckoutComponent } from './components/checkout/checkout.component'; // 1. Importar el componente
import { LoginComponent } from './pages/login/login.component';
import { authGuard } from './services/auth.guard';
import { HistorialPedidosComponent } from './pages/historial-pedidos/historial-pedidos.component';

export const routes: Routes = [
    { path: '', component: CatalogoPerfumesComponent, pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    { path: 'carrito', component: Carrito, canActivate: [authGuard] },
    { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard] },
    { path: 'admin/perfumes', component: AdminPerfumesComponent, canActivate: [authGuard] },
    { path: 'mis-pedidos', component: HistorialPedidosComponent, canActivate: [authGuard] },
    { path: '**', redirectTo: '' } // Siempre debe ir al final
];

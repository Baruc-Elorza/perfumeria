import { Routes } from '@angular/router';
import { AdminPerfumesComponent } from './pages/admin-perfumes/admin-perfumes.component';
import { Carrito } from './pages/carrito/carrito';
import { CatalogoPerfumesComponent } from './pages/catalogo-perfumes/catalogo-perfumes.component';
import { CheckoutComponent } from './components/checkout/checkout.component'; // 1. Importar el componente

export const routes: Routes = [
    { path: '', component: CatalogoPerfumesComponent, pathMatch: 'full' },
    { path: 'carrito', component: Carrito },
    { path: 'checkout', component: CheckoutComponent }, // 2. Agregar la ruta para la HU-16
    { path: 'admin/perfumes', component: AdminPerfumesComponent },
    { path: '**', redirectTo: '' } // Siempre debe ir al final
];

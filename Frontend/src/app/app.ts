import { Component } from '@angular/core';
import { CatalogoPerfumesComponent } from './components/catalogo-perfumes/catalogo-perfumes.component';

@Component({
  imports: [CatalogoPerfumesComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
}

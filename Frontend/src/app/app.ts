import { Component } from '@angular/core';
import { CatalogoPerfumesComponent } from './components/catalogo-perfumes/catalogo-perfumes.component';
import { RouterOutlet } from '@angular/router';
@Component({
  imports: [CatalogoPerfumesComponent, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
}

import { Component } from '@angular/core';
import { AdminPerfumesComponent } from './components/admin-perfumes/admin-perfumes.component';

@Component({
  imports: [AdminPerfumesComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
}

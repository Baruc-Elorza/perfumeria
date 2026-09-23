import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  mensaje = '';
  constructor(private http: HttpClient){
    this.http
    .get('http://localhost:8080/api/hola', {
      responseType: 'text'
    })
    .subscribe(respuesta =>{
      this.mensaje = respuesta;
    });
  }
}

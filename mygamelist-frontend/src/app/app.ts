import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
// 1. IMPORTANTE: Importe o arquivo do Navbar aqui
import { Navbar } from './components/navbar/navbar';

@Component({
  selector: 'app-root',
  standalone: true,
  // 2. IMPORTANTE: Adicione o NavbarComponent nesta lista de imports
  imports: [RouterOutlet, Navbar], 
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'mygamelist-frontend';
}
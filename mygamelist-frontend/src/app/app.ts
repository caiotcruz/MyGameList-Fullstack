import { FeedbackHost } from './components/feedback-host';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './components/navbar/navbar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Navbar, FeedbackHost], 
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'mygamelist-frontend';
}
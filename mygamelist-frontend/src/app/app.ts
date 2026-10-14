import { FeedbackHost } from './components/feedback-host';
import { ThemeService } from './services/theme';
import { Component, inject } from '@angular/core';
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
  private theme = inject(ThemeService);
  title = 'mygamelist-frontend';
}

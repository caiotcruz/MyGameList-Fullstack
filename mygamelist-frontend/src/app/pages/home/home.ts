import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Feed } from '../../components/feed/feed'; 
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, Feed],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {

  authService = inject(AuthService);
  cdr = inject(ChangeDetectorRef); 
  
  userName = 'Gamer'; 
  myId!: number;

  ngOnInit(): void {
    this.updateUserName();
    this.myId = Number(localStorage.getItem('userId'));
  }

  updateUserName() {
    const storedName = localStorage.getItem('userName');
    if (storedName) {
      this.userName = storedName;
    }
    
    this.cdr.detectChanges();
  }
  
}
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Api } from '../../api/api';
import { getMe } from '../../api/fn/user/get-me';
import { UserDto } from '../../api/models/user-dto';
import { from } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private api = inject(Api);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  user: UserDto | null = null;

  ngOnInit() {
    this.loadUser();
  }

  loadUser() {
    console.log('DashboardComponent.loadUser called');
    from(this.api.invoke(getMe)).subscribe({
      next: (data) => {
        console.log('User data loaded:', data);
        this.user = data || null;
        console.log('User set to:', this.user);
        this.cdr.detectChanges(); // Manually trigger change detection
      },
      error: (err: any) => {
        console.error('Error loading user profile', err);
        if (err.status === 401 || err.status === 403) {
          localStorage.removeItem('token');
          this.router.navigate(['/login']);
        }
        this.cdr.detectChanges();
      }
    });
  }
}

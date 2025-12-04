import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Api } from '../../api/api';
import { getMe } from '../../api/fn/user/get-me';
import { UserDto } from '../../api/models/user-dto';
import { StatisticsResponse } from '../../api/models/statistics-response';
import { getStatistics } from '../../api/fn/statistics-controller/get-statistics';
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
  statistics: StatisticsResponse | null = null;

  // Displayed values for count-up animation
  displayedStats = {
    totalOffers: 0,
    freeOffers: 0,
    exchangeOffers: 0,
    paidOffers: 0,
    totalSearches: 0
  };

  ngOnInit() {
    this.loadUser();
    this.loadStatistics();
  }

  loadUser() {
    from(this.api.invoke(getMe)).subscribe({
      next: (data) => {
        this.user = data || null;
        this.cdr.detectChanges();
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

  loadStatistics() {
    from(this.api.invoke(getStatistics)).subscribe({
      next: (data) => {
        this.statistics = data;
        this.animateCountUp();
      },
      error: (err) => {
        console.error('Error loading statistics', err);
      }
    });
  }

  private animateCountUp() {
    if (!this.statistics?.cardOffers || !this.statistics?.cardSearches) return;

    const duration = 1500; // 1.5 seconds
    const frameRate = 60; // 60 FPS
    const totalFrames = (duration / 1000) * frameRate;
    let currentFrame = 0;

    const targets = {
      totalOffers: this.statistics.cardOffers.total ?? 0,
      freeOffers: this.statistics.cardOffers.free ?? 0,
      exchangeOffers: this.statistics.cardOffers.exchange ?? 0,
      paidOffers: this.statistics.cardOffers.paid ?? 0,
      totalSearches: this.statistics.cardSearches.total ?? 0
    };

    const animate = () => {
      currentFrame++;
      const progress = currentFrame / totalFrames;

      // Easing function for smooth animation (easeOutCubic)
      const easeProgress = 1 - Math.pow(1 - progress, 3);

      this.displayedStats.totalOffers = Math.floor(targets.totalOffers * easeProgress);
      this.displayedStats.freeOffers = Math.floor(targets.freeOffers * easeProgress);
      this.displayedStats.exchangeOffers = Math.floor(targets.exchangeOffers * easeProgress);
      this.displayedStats.paidOffers = Math.floor(targets.paidOffers * easeProgress);
      this.displayedStats.totalSearches = Math.floor(targets.totalSearches * easeProgress);

      this.cdr.detectChanges();

      if (currentFrame < totalFrames) {
        requestAnimationFrame(animate);
      } else {
        // Ensure final values are exact
        this.displayedStats = { ...targets };
        this.cdr.detectChanges();
      }
    };

    requestAnimationFrame(animate);
  }
}

import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Api } from '../../../api/api';
import { getExchangeMatches } from '../../../api/fn/matches/get-exchange-matches';
import { getFreebieMatches } from '../../../api/fn/matches/get-freebie-matches';
import { getPayedMatches } from '../../../api/fn/matches/get-payed-matches';
import { createExchangeRequest } from '../../../api/fn/exchanges/create-exchange-request';
import { MatchResponse } from '../../../api/models/match-response';
import { Pageable } from '../../../api/models/pageable';
import { CreateExchangeRequestDto } from '../../../api/models/create-exchange-request-dto';
import { BehaviorSubject, Observable, from, of, forkJoin } from 'rxjs';
import { switchMap, tap, catchError, map, finalize } from 'rxjs/operators';
import { ExchangeRequestModalComponent } from '../exchange-request-modal/exchange-request-modal.component';

type MatchTab = 'EXCHANGE' | 'FREEBIE' | 'PAYED';

@Component({
  selector: 'app-match-list',
  standalone: true,
  imports: [CommonModule, TranslateModule, ExchangeRequestModalComponent],
  templateUrl: './match-list.html',
  styleUrl: './match-list.css',
})
export class MatchListComponent implements OnInit {
  private api = inject(Api);
  private cdr = inject(ChangeDetectorRef);

  matches: MatchResponse[] = [];
  loading = true;

  // State management with Subjects
  private currentTabSubject = new BehaviorSubject<MatchTab>('EXCHANGE');
  private currentPageSubject = new BehaviorSubject<number>(0);

  // Pagination state
  pageSize: number = 20;
  totalPages: number = 0;
  totalElements: number = 0;

  // Modal State
  isModalVisible = false;
  selectedMatch: MatchResponse | null = null;

  // Expose Math to template
  Math = Math;

  ngOnInit() {
    // Combine tab and page changes to trigger data loading
    this.currentTabSubject.pipe(
      switchMap(tab => {
        return this.currentPageSubject.pipe(
          tap(() => {
            this.loading = true;
            this.matches = []; // Clear current matches to avoid showing stale data
            this.cdr.detectChanges();
          }),
          switchMap(page => this.fetchMatches(tab, page))
        );
      })
    ).subscribe({
      next: (data) => {
        console.log('Matches loaded:', data);
        this.matches = data.content || [];
        this.totalPages = data.totalPages || 0;
        this.totalElements = data.totalElements || 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error in match subscription', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private fetchMatches(tab: MatchTab, page: number): Observable<any> {
    const pageable: Pageable = { page, size: this.pageSize };
    let apiCall;

    switch (tab) {
      case 'FREEBIE':
        apiCall = this.api.invoke(getFreebieMatches, { pageable });
        break;
      case 'PAYED':
        apiCall = this.api.invoke(getPayedMatches, { pageable });
        break;
      case 'EXCHANGE':
      default:
        apiCall = this.api.invoke(getExchangeMatches, { pageable });
        break;
    }

    return from(apiCall).pipe(
      catchError(err => {
        console.error('Error fetching matches', err);
        return of({ content: [], totalPages: 0, totalElements: 0 });
      })
    );
  }

  // Modal Logic
  openRequestModal(match: MatchResponse) {
    this.selectedMatch = match;
    this.isModalVisible = true;
    this.cdr.detectChanges();
  }

  closeRequestModal() {
    this.isModalVisible = false;
    this.selectedMatch = null;
    this.cdr.detectChanges();
  }

  handleRequestSubmit(requests: CreateExchangeRequestDto[]) {
    if (requests.length === 0) {
      this.closeRequestModal();
      return;
    }

    this.loading = true;
    const observables = requests.map(req =>
      this.api.invoke(createExchangeRequest, { body: req })
    );

    // Use concat to execute requests sequentially
    import('rxjs').then(({ concat }) => {
      concat(...observables).pipe(
        finalize(() => {
          this.loading = false;
          this.closeRequestModal();
          this.cdr.detectChanges();
        })
      ).subscribe({
        next: () => {
          console.log('Exchange request created successfully');
        },
        error: (err) => {
          console.error('Error creating exchange requests', err);
          if (err.status === 400) {
            alert('Some requests could not be created because they already exist or are invalid.');
          } else {
            alert('An error occurred while creating the exchange requests. Please try again.');
          }
        },
        complete: () => {
          console.log('All exchange requests processed');
          // Refresh list or show success message
        }
      });
    });
  }

  // Public getters for template
  get currentTab(): MatchTab {
    return this.currentTabSubject.value;
  }

  get currentPage(): number {
    return this.currentPageSubject.value;
  }

  setTab(tab: MatchTab) {
    if (this.currentTabSubject.value !== tab) {
      this.currentPageSubject.next(0); // Reset to first page
      this.currentTabSubject.next(tab);
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPageSubject.next(this.currentPage + 1);
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPageSubject.next(this.currentPage - 1);
    }
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPageSubject.next(page);
    }
  }

  get hasNextPage(): boolean {
    return this.currentPage < this.totalPages - 1;
  }

  get hasPreviousPage(): boolean {
    return this.currentPage > 0;
  }
}

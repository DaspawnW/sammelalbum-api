import { Component, OnInit, inject, ChangeDetectorRef, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Api } from '../../../api/api';
import { getReceivedOffers } from '../../../api/fn/exchanges/get-received-offers';
import { getSentRequests } from '../../../api/fn/exchanges/get-sent-requests';
import { acceptExchangeRequest } from '../../../api/fn/exchanges/accept-exchange-request';
import { declineExchangeRequest } from '../../../api/fn/exchanges/decline-exchange-request';
import { closeExchangeRequest } from '../../../api/fn/exchanges/close-exchange-request';
import { ExchangeRequestDto } from '../../../api/models/exchange-request-dto';
import { from, forkJoin } from 'rxjs';

@Component({
  selector: 'app-exchange-list',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './exchange-list.html',
  styleUrl: './exchange-list.css',
})
export class ExchangeListComponent implements OnInit {
  private api = inject(Api);
  private cdr = inject(ChangeDetectorRef);

  // Raw data
  private allReceivedOffers: ExchangeRequestDto[] = [];
  private allSentRequests: ExchangeRequestDto[] = [];

  // Display data
  receivedOffers: ExchangeRequestDto[] = [];
  sentRequests: ExchangeRequestDto[] = [];

  loading = true;

  // Filter
  readonly allStatuses: Array<ExchangeRequestDto['status']> = ['INITIAL', 'MAIL_SEND', 'EXCHANGE_INTERREST', 'EXCHANGE_COMPLETED', 'EXCHANGE_CANCELED'];
  selectedStatuses: Set<ExchangeRequestDto['status']> = new Set(['INITIAL', 'MAIL_SEND', 'EXCHANGE_INTERREST']);
  isFilterDropdownOpen = false;

  // Sort
  sortColumn: string = 'partnerId';
  sortDirection: 'asc' | 'desc' = 'asc';

  // Modal
  selectedExchange: ExchangeRequestDto | null = null;
  userRole: 'requester' | 'offerer' | null = null;
  showCloseWarning: boolean = false;

  constructor(private elementRef: ElementRef) { }

  ngOnInit() {
    this.loadExchanges();
  }

  loadExchanges() {
    console.log('ExchangeListComponent.loadExchanges called');
    this.loading = true;
    forkJoin({
      received: from(this.api.invoke(getReceivedOffers)),
      sent: from(this.api.invoke(getSentRequests))
    }).subscribe({
      next: (data) => {
        console.log('Exchanges loaded, raw data:', JSON.stringify(data));
        this.allReceivedOffers = data.received || [];
        this.allSentRequests = data.sent || [];

        this.applyFiltersAndSort();

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading exchanges', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (this.isFilterDropdownOpen) {
      const clickedInside = this.elementRef.nativeElement.querySelector('.relative').contains(event.target);
      if (!clickedInside) {
        this.isFilterDropdownOpen = false;
      }
    }
  }

  toggleStatus(status: ExchangeRequestDto['status']) {
    if (this.selectedStatuses.has(status)) {
      this.selectedStatuses.delete(status);
    } else {
      this.selectedStatuses.add(status);
    }
    this.applyFiltersAndSort();
  }

  toggleFilterDropdown() {
    this.isFilterDropdownOpen = !this.isFilterDropdownOpen;
  }

  sort(column: string) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFiltersAndSort();
  }

  // Modal Actions
  openModal(exchange: ExchangeRequestDto, role: 'requester' | 'offerer') {
    this.selectedExchange = exchange;
    this.userRole = role;
    this.showCloseWarning = false;
  }

  closeModal() {
    this.selectedExchange = null;
    this.userRole = null;
    this.showCloseWarning = false;
  }

  acceptExchange() {
    if (!this.selectedExchange?.id) return;
    this.api.invoke(acceptExchangeRequest, { id: this.selectedExchange.id }).then(() => {
      this.closeModal();
      this.loadExchanges();
    }).catch(err => console.error('Error accepting exchange', err));
  }

  declineExchange() {
    if (!this.selectedExchange?.id) return;
    this.api.invoke(declineExchangeRequest, { id: this.selectedExchange.id }).then(() => {
      this.closeModal();
      this.loadExchanges();
    }).catch(err => console.error('Error declining exchange', err));
  }

  initiateCloseExchange() {
    this.showCloseWarning = true;
  }

  confirmCloseExchange() {
    if (!this.selectedExchange?.id) return;
    this.api.invoke(closeExchangeRequest, { id: this.selectedExchange.id }).then(() => {
      this.closeModal();
      this.loadExchanges();
    }).catch(err => console.error('Error closing exchange', err));
  }

  cancelCloseWarning() {
    this.showCloseWarning = false;
  }

  private applyFiltersAndSort() {
    this.receivedOffers = this.filterAndSort(this.allReceivedOffers, 'received');
    this.sentRequests = this.filterAndSort(this.allSentRequests, 'sent');
    this.cdr.detectChanges();
  }

  private filterAndSort(data: ExchangeRequestDto[], type: 'received' | 'sent'): ExchangeRequestDto[] {
    // 1. Filter
    console.log(`Filtering ${type} data. Total: ${data.length}. Selected statuses: ${Array.from(this.selectedStatuses).join(', ')}`);
    let result = data.filter(item => {
      const match = item.status && this.selectedStatuses.has(item.status);
      console.log(`Item status: ${item.status}, Match: ${match}`);
      return match;
    });

    // 2. Sort
    return result.sort((a, b) => {
      const valueA = this.getSortValue(a, this.sortColumn, type);
      const valueB = this.getSortValue(b, this.sortColumn, type);

      if (valueA < valueB) return this.sortDirection === 'asc' ? -1 : 1;
      if (valueA > valueB) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  private getSortValue(item: ExchangeRequestDto, column: string, type: 'received' | 'sent'): any {
    switch (column) {
      case 'partnerId': return type === 'received' ? (item.requesterId || 0) : (item.offererId || 0);
      case 'type': return item.exchangeType;
      case 'status': return item.status;
      case 'partner': return (item.partnerFirstname || '') + (item.partnerLastname || '');
      case 'requested': return item.requestedStickerName || '';
      case 'offered': return item.offeredStickerName || '';
      default: return '';
    }
  }

  getSortIcon(column: string): string {
    if (this.sortColumn !== column) {
      return '↕'; // Neutral icon for non-sorted columns
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  getStatusTranslationKey(exchange: ExchangeRequestDto, role: 'requester' | 'offerer' | null): string {
    if (!role) {
      return 'EXCHANGES.TABLE.STATUSES.' + exchange.status;
    }
    if (exchange.status === 'EXCHANGE_INTERREST') {
      // Priority 1: My action
      if (role === 'requester' && exchange.requesterClosed) {
        return 'EXCHANGES.TABLE.STATUSES.STATUS_RECEIVED';
      }
      if (role === 'offerer' && exchange.offererClosed) {
        return 'EXCHANGES.TABLE.STATUSES.STATUS_GIVEN';
      }

      // Priority 2: Partner action
      if (role === 'requester' && exchange.offererClosed) {
        return 'EXCHANGES.TABLE.STATUSES.STATUS_RECEIVED';
      }
      if (role === 'offerer' && exchange.requesterClosed) {
        return 'EXCHANGES.TABLE.STATUSES.STATUS_GIVEN';
      }
    }
    return 'EXCHANGES.TABLE.STATUSES.' + exchange.status;
  }

  isDeclineDisabled(): boolean {
    if (!this.selectedExchange || !this.userRole) {
      return false;
    }
    if (this.userRole === 'requester' && this.selectedExchange.requesterClosed) {
      return true;
    }
    if (this.userRole === 'offerer' && this.selectedExchange.offererClosed) {
      return true;
    }
    return false;
  }
}

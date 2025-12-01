import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../../../api/api';
import { getOffers } from '../../../api/fn/card-offers/get-offers';
import { addBulkOffers } from '../../../api/fn/card-offers/add-bulk-offers';
import { removeBulkOffers } from '../../../api/fn/card-offers/remove-bulk-offers';
import { CardOfferResponse } from '../../../api/models/card-offer-response';
import { from, forkJoin } from 'rxjs';

@Component({
  selector: 'app-offer-list',
  standalone: true,
  imports: [CommonModule, TranslateModule, FormsModule],
  templateUrl: './offer-list.html',
  styleUrl: './offer-list.css',
})
export class OfferListComponent implements OnInit {
  private api = inject(Api);
  private cdr = inject(ChangeDetectorRef);
  offers: CardOfferResponse[] = [];
  loading = true;

  // Pagination state
  currentPage: number = 0;
  pageSize: number = 20;
  totalPages: number = 0;
  totalElements: number = 0;

  // Expose Math to template
  Math = Math;

  // Sorting state
  sortColumn: string = 'stickerId';
  sortDirection: 'asc' | 'desc' = 'asc';

  // Selection state
  selectedOfferIds: Set<number> = new Set();

  // Delete modal state
  showDeleteModal: boolean = false;
  deleting: boolean = false;
  deleteError: string = '';

  // Modal state
  showCreateModal: boolean = false;
  stickerIdsInput: string = '';
  offerPayed: boolean = false;
  offerExchange: boolean = false;
  offerFreebie: boolean = false;
  createError: string = '';
  creating: boolean = false;

  ngOnInit() {
    this.loadOffers();
  }

  loadOffers() {

    this.loading = true;
    from(this.api.invoke(getOffers)).subscribe({
      next: (data) => {


        this.offers = data || [];

        this.sortOffers(); // Apply default sort
        this.loading = false;
        this.cdr.detectChanges(); // Manually trigger change detection
      },
      error: (err) => {
        console.error('Error loading offers', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  openCreateModal() {
    this.showCreateModal = true;
    this.stickerIdsInput = '';
    this.offerPayed = false;
    this.offerExchange = false;
    this.offerFreebie = false;
    this.createError = '';
  }

  closeCreateModal() {
    this.showCreateModal = false;
  }

  createOffers() {
    this.createError = '';

    // Validate at least one offer type is selected
    if (!this.offerPayed && !this.offerExchange && !this.offerFreebie) {
      this.createError = 'OFFERS.CREATE_MODAL.ERROR_NO_TYPE';
      return;
    }

    // Parse sticker IDs from textarea (one per line)
    const stickerIds = this.stickerIdsInput
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(line => parseInt(line, 10))
      .filter(id => !isNaN(id));

    if (stickerIds.length === 0) {
      this.createError = 'OFFERS.CREATE_MODAL.ERROR_NO_IDS';
      return;
    }

    this.creating = true;
    from(this.api.invoke(addBulkOffers, {
      body: {
        stickerIds,
        offerPayed: this.offerPayed,
        offerExchange: this.offerExchange,
        offerFreebie: this.offerFreebie
      }
    })).subscribe({
      next: (response) => {

        this.creating = false;
        this.closeCreateModal();
        this.loadOffers(); // Refresh the list
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error creating bulk offers:', err);
        this.createError = 'OFFERS.CREATE_MODAL.ERROR_GENERAL';
        this.creating = false;
        this.cdr.detectChanges();
      }
    });
  }

  sortBy(column: string) {
    if (this.sortColumn === column) {
      // Toggle direction if same column
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      // New column, default to ascending
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.sortOffers();
  }

  sortOffers() {
    this.offers.sort((a, b) => {
      let comparison = 0;

      switch (this.sortColumn) {
        case 'stickerId':
          comparison = (a.stickerId || 0) - (b.stickerId || 0);
          break;
        case 'stickerName':
          const nameA = (a.stickerName || '').toLowerCase();
          const nameB = (b.stickerName || '').toLowerCase();
          comparison = nameA.localeCompare(nameB);
          break;
        case 'type':
          // Sort by offer type priority: Payed > Exchange > Freebie
          const getPriority = (offer: CardOfferResponse) => {
            if (offer.offerPayed) return 3;
            if (offer.offerExchange) return 2;
            if (offer.offerFreebie) return 1;
            return 0;
          };
          comparison = getPriority(a) - getPriority(b);
          break;
      }

      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  // Selection methods
  toggleOfferSelection(offerId: number) {
    if (this.selectedOfferIds.has(offerId)) {
      this.selectedOfferIds.delete(offerId);
    } else {
      this.selectedOfferIds.add(offerId);
    }
  }

  isOfferSelected(offerId: number): boolean {
    return this.selectedOfferIds.has(offerId);
  }

  toggleSelectAll() {
    if (this.isAllSelected()) {
      this.selectedOfferIds.clear();
    } else {
      this.offers.forEach(offer => {
        if (offer.id) {
          this.selectedOfferIds.add(offer.id);
        }
      });
    }
  }

  isAllSelected(): boolean {
    return this.offers.length > 0 && this.offers.every(offer => offer.id && this.selectedOfferIds.has(offer.id));
  }

  get hasSelectedOffers(): boolean {
    return this.selectedOfferIds.size > 0;
  }

  // Delete methods
  openDeleteModal() {
    this.showDeleteModal = true;
    this.deleteError = '';
  }

  openDeleteModalForOffer(offerId: number) {
    this.selectedOfferIds.clear();
    this.selectedOfferIds.add(offerId);
    this.openDeleteModal();
  }

  closeDeleteModal() {
    this.showDeleteModal = false;
    this.deleteError = '';
  }

  deleteSelectedOffers() {
    if (this.selectedOfferIds.size === 0) {
      return;
    }

    this.deleting = true;
    this.deleteError = '';

    // Map selected offer IDs to sticker IDs
    const stickerIds = this.offers
      .filter(offer => offer.id && this.selectedOfferIds.has(offer.id))
      .map(offer => offer.stickerId)
      .filter((id): id is number => id !== undefined);

    if (stickerIds.length === 0) {
      this.deleting = false;
      return;
    }

    from(this.api.invoke(removeBulkOffers, {
      body: { stickerIds }
    })).subscribe({
      next: () => {
        this.selectedOfferIds.clear();
        this.closeDeleteModal();
        this.loadOffers();
      },
      error: (err) => {
        console.error('Error deleting offers:', err);
        this.deleteError = 'OFFERS.DELETE_MODAL.ERROR_GENERAL';
        this.deleting = false;
      },
      complete: () => {
        this.deleting = false;
      }
    });
  }

  getSortIcon(column: string): string {
    if (this.sortColumn !== column) {
      return '↕'; // Neutral icon for non-sorted columns
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }
}

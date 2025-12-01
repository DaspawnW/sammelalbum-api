import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../../../api/api';
import { getSearches } from '../../../api/fn/card-search/get-searches';
import { addBulkSearches } from '../../../api/fn/card-search/add-bulk-searches';
import { removeBulkSearches } from '../../../api/fn/card-search/remove-bulk-searches';
import { CardSearchResponse } from '../../../api/models/card-search-response';
import { from } from 'rxjs';

@Component({
  selector: 'app-search-list',
  standalone: true,
  imports: [CommonModule, TranslateModule, FormsModule],
  templateUrl: './search-list.html',
  styleUrl: './search-list.css',
})
export class SearchListComponent implements OnInit {
  private api = inject(Api);
  private cdr = inject(ChangeDetectorRef);
  searches: CardSearchResponse[] = [];
  loading = true;

  // Sorting state
  sortColumn: string = 'stickerId';
  sortDirection: 'asc' | 'desc' = 'asc';

  // Selection state
  selectedSearchIds: Set<number> = new Set();

  // Delete modal state
  showDeleteModal: boolean = false;
  deleting: boolean = false;
  deleteError: string = '';

  // Modal state
  showCreateModal: boolean = false;
  stickerIdsInput: string = '';
  createError: string = '';
  creating: boolean = false;

  ngOnInit() {
    this.loadSearches();
  }

  loadSearches() {

    this.loading = true;
    from(this.api.invoke(getSearches)).subscribe({
      next: (data) => {

        this.searches = data || [];

        this.sortSearches(); // Apply default sort
        this.loading = false;
        this.cdr.detectChanges(); // Manually trigger change detection
      },
      error: (err) => {
        console.error('Error loading searches', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  openCreateModal() {
    this.showCreateModal = true;
    this.stickerIdsInput = '';
    this.createError = '';
  }

  closeCreateModal() {
    this.showCreateModal = false;
  }

  createSearches() {
    this.createError = '';

    // Parse sticker IDs from textarea (one per line)
    const stickerIds = this.stickerIdsInput
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(line => parseInt(line, 10))
      .filter(id => !isNaN(id));

    if (stickerIds.length === 0) {
      this.createError = 'SEARCHES.CREATE_MODAL.ERROR_NO_IDS';
      return;
    }

    this.creating = true;
    from(this.api.invoke(addBulkSearches, {
      body: { stickerIds }
    })).subscribe({
      next: (response) => {

        this.creating = false;
        this.closeCreateModal();
        this.loadSearches(); // Refresh the list
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error creating bulk searches:', err);
        this.createError = 'SEARCHES.CREATE_MODAL.ERROR_GENERAL';
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
    this.sortSearches();
  }

  sortSearches() {
    this.searches.sort((a, b) => {
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
        case 'status':
          // Sort by reserved status: reserved (true) before open (false)
          comparison = (a.isReserved === b.isReserved) ? 0 : (a.isReserved ? -1 : 1);
          break;
      }

      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  // Selection methods
  toggleSearchSelection(searchId: number) {
    if (this.selectedSearchIds.has(searchId)) {
      this.selectedSearchIds.delete(searchId);
    } else {
      this.selectedSearchIds.add(searchId);
    }
  }

  isSearchSelected(searchId: number): boolean {
    return this.selectedSearchIds.has(searchId);
  }

  toggleSelectAll() {
    if (this.isAllSelected()) {
      this.selectedSearchIds.clear();
    } else {
      this.searches.forEach(search => {
        if (search.id) {
          this.selectedSearchIds.add(search.id);
        }
      });
    }
  }

  isAllSelected(): boolean {
    return this.searches.length > 0 && this.searches.every(search => search.id && this.selectedSearchIds.has(search.id));
  }

  get hasSelectedSearches(): boolean {
    return this.selectedSearchIds.size > 0;
  }

  // Delete methods
  openDeleteModal() {
    this.showDeleteModal = true;
    this.deleteError = '';
  }

  openDeleteModalForSearch(searchId: number) {
    this.selectedSearchIds.clear();
    this.selectedSearchIds.add(searchId);
    this.openDeleteModal();
  }

  closeDeleteModal() {
    this.showDeleteModal = false;
    this.deleteError = '';
  }

  deleteSelectedSearches() {
    if (this.selectedSearchIds.size === 0) {
      return;
    }

    this.deleting = true;
    this.deleteError = '';

    // Map selected search IDs to sticker IDs
    const stickerIds = this.searches
      .filter(search => search.id && this.selectedSearchIds.has(search.id))
      .map(search => search.stickerId)
      .filter((id): id is number => id !== undefined);

    if (stickerIds.length === 0) {
      this.deleting = false;
      return;
    }

    from(this.api.invoke(removeBulkSearches, {
      body: { stickerIds }
    })).subscribe({
      next: () => {
        this.selectedSearchIds.clear();
        this.closeDeleteModal();
        this.loadSearches();
      },
      error: (err) => {
        console.error('Error deleting searches:', err);
        this.deleteError = 'SEARCHES.DELETE_MODAL.ERROR_GENERAL';
        this.deleting = false;
      },
      complete: () => {
        this.deleting = false;
      }
    });
  }

  getSortIcon(column: string): string {
    if (this.sortColumn !== column) {
      return '↕';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }
}

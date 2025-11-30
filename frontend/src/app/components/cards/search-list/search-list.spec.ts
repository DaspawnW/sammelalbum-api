import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SearchListComponent } from './search-list';
import { Api } from '../../../api/api';
import { TranslateModule } from '@ngx-translate/core';
import { CardSearchResponse } from '../../../api/models/card-search-response';
import { createSpyObj } from '../../../test-utils';

describe('SearchListComponent', () => {
  let component: SearchListComponent;
  let fixture: ComponentFixture<SearchListComponent>;
  let apiSpy: any;

  beforeEach(async () => {
    apiSpy = createSpyObj('Api', ['invoke']);

    await TestBed.configureTestingModule({
      imports: [SearchListComponent, TranslateModule.forRoot()],
      providers: [
        { provide: Api, useValue: apiSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch searches on init', () => {
    const mockSearches: CardSearchResponse[] = [{ stickerId: 1, isReserved: false }];
    apiSpy.invoke.and.returnValue(Promise.resolve(mockSearches));

    fixture.detectChanges();

    expect(apiSpy.invoke).toHaveBeenCalled();
  });
});

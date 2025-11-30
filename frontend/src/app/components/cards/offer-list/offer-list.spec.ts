import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OfferListComponent } from './offer-list';
import { Api } from '../../../api/api';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';
import { CardOfferResponse } from '../../../api/models/card-offer-response';
import { createSpyObj } from '../../../test-utils';

describe('OfferListComponent', () => {
  let component: OfferListComponent;
  let fixture: ComponentFixture<OfferListComponent>;
  let apiSpy: any;

  beforeEach(async () => {
    apiSpy = createSpyObj('Api', ['invoke']);

    await TestBed.configureTestingModule({
      imports: [OfferListComponent, TranslateModule.forRoot()],
      providers: [
        { provide: Api, useValue: apiSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OfferListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch offers on init', () => {
    const mockOffers: CardOfferResponse[] = [{ stickerId: 1, offerPayed: true }];
    apiSpy.invoke.and.returnValue(Promise.resolve(mockOffers));

    fixture.detectChanges();

    expect(apiSpy.invoke).toHaveBeenCalled();
  });
});

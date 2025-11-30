import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExchangeListComponent } from './exchange-list';
import { Api } from '../../../api/api';
import { TranslateModule } from '@ngx-translate/core';
import { ExchangeRequestDto } from '../../../api/models/exchange-request-dto';
import { createSpyObj } from '../../../test-utils';

describe('ExchangeListComponent', () => {
  let component: ExchangeListComponent;
  let fixture: ComponentFixture<ExchangeListComponent>;
  let apiSpy: any;

  beforeEach(async () => {
    apiSpy = createSpyObj('Api', ['invoke']);

    await TestBed.configureTestingModule({
      imports: [ExchangeListComponent, TranslateModule.forRoot()],
      providers: [
        { provide: Api, useValue: apiSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ExchangeListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch exchanges on init', () => {
    const mockExchanges: ExchangeRequestDto[] = [{ id: 1 }];
    apiSpy.invoke.and.returnValue(Promise.resolve(mockExchanges));

    fixture.detectChanges();

    expect(apiSpy.invoke).toHaveBeenCalledTimes(2); // Once for received, once for sent
  });
});

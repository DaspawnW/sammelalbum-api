import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatchListComponent } from './match-list';
import { Api } from '../../../api/api';
import { TranslateModule } from '@ngx-translate/core';
import { PageMatchResponse } from '../../../api/models/page-match-response';
import { createSpyObj } from '../../../test-utils';

describe('MatchListComponent', () => {
  let component: MatchListComponent;
  let fixture: ComponentFixture<MatchListComponent>;
  let apiSpy: any;

  beforeEach(async () => {
    apiSpy = createSpyObj('Api', ['invoke']);

    await TestBed.configureTestingModule({
      imports: [MatchListComponent, TranslateModule.forRoot()],
      providers: [
        { provide: Api, useValue: apiSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MatchListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch matches on init', () => {
    const mockMatches: PageMatchResponse = { content: [] };
    apiSpy.invoke.and.returnValue(Promise.resolve(mockMatches));

    fixture.detectChanges();

    expect(apiSpy.invoke).toHaveBeenCalled();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterComponent } from './register';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { Api } from '../../api/api';
import { createSpyObj } from '../../test-utils';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let apiSpy: any;

  beforeEach(async () => {
    apiSpy = createSpyObj('Api', ['invoke']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, TranslateModule.forRoot()],
      providers: [
        { provide: ActivatedRoute, useValue: { params: of({}) } },
        { provide: Api, useValue: apiSpy }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

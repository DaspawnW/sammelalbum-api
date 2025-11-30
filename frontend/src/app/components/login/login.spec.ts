import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { Api } from '../../api/api';
import { createSpyObj } from '../../test-utils';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let apiSpy: any;

  beforeEach(async () => {
    apiSpy = createSpyObj('Api', ['invoke']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, TranslateModule.forRoot()],
      providers: [
        { provide: ActivatedRoute, useValue: { params: of({}) } },
        { provide: Api, useValue: apiSpy }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

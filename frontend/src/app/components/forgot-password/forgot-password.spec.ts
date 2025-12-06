import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ForgotPasswordComponent } from './forgot-password';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { Api } from '../../api/api';
import { createSpyObj } from '../../test-utils';

describe('ForgotPasswordComponent', () => {
    let component: ForgotPasswordComponent;
    let fixture: ComponentFixture<ForgotPasswordComponent>;
    let apiSpy: any;

    beforeEach(async () => {
        apiSpy = createSpyObj('Api', ['invoke']);

        await TestBed.configureTestingModule({
            imports: [ForgotPasswordComponent, TranslateModule.forRoot()],
            providers: [
                { provide: ActivatedRoute, useValue: { params: of({}) } },
                { provide: Api, useValue: apiSpy }
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ForgotPasswordComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have invalid form when identifier is empty', () => {
        expect(component.forgotPasswordForm.valid).toBeFalsy();
    });

    it('should have valid form when identifier is provided', () => {
        component.forgotPasswordForm.controls['identifier'].setValue('testuser');
        expect(component.forgotPasswordForm.valid).toBeTruthy();
    });

    it('should call API and show success message on successful submission', async () => {
        apiSpy.invoke.and.returnValue(Promise.resolve(null));

        component.forgotPasswordForm.controls['identifier'].setValue('testuser');
        await component.onSubmit();

        expect(apiSpy.invoke).toHaveBeenCalled();
        expect(component.success).toBeTruthy();
        expect(component.error).toBe('');
        expect(component.isSubmitting).toBeFalsy();
    });

    it('should show error message on API failure', async () => {
        apiSpy.invoke.and.returnValue(Promise.reject(new Error('API Error')));

        component.forgotPasswordForm.controls['identifier'].setValue('testuser');
        component.onSubmit();

        // Wait for the promise to reject and change detection to run
        await new Promise(resolve => setTimeout(resolve, 10));

        expect(apiSpy.invoke).toHaveBeenCalled();
        expect(component.success).toBeFalsy();
        expect(component.error).toBe('AUTH.FORGOT_PASSWORD.ERROR');
        expect(component.isSubmitting).toBeFalsy();
    });

    it('should not submit when form is invalid', async () => {
        await component.onSubmit();
        expect(apiSpy.invoke).not.toHaveBeenCalled();
    });

    it('should not submit when already submitting', async () => {
        component.forgotPasswordForm.controls['identifier'].setValue('testuser');
        component.isSubmitting = true;

        await component.onSubmit();
        expect(apiSpy.invoke).not.toHaveBeenCalled();
    });

    it('should reset form after successful submission', async () => {
        apiSpy.invoke.and.returnValue(Promise.resolve(null));

        component.forgotPasswordForm.controls['identifier'].setValue('testuser');
        await component.onSubmit();

        expect(component.forgotPasswordForm.value.identifier).toBeNull();
    });
});

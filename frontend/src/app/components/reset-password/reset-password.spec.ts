import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResetPasswordComponent } from './reset-password';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { Api } from '../../api/api';
import { createSpyObj } from '../../test-utils';

describe('ResetPasswordComponent', () => {
    let component: ResetPasswordComponent;
    let fixture: ComponentFixture<ResetPasswordComponent>;
    let apiSpy: any;
    let activatedRoute: any;

    beforeEach(async () => {
        apiSpy = createSpyObj('Api', ['invoke']);
        activatedRoute = {
            queryParams: of({ token: 'test-token-123' })
        };

        await TestBed.configureTestingModule({
            imports: [ResetPasswordComponent, TranslateModule.forRoot()],
            providers: [
                { provide: ActivatedRoute, useValue: activatedRoute },
                { provide: Api, useValue: apiSpy }
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ResetPasswordComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should extract token from URL query parameters', () => {
        expect(component.token).toBe('test-token-123');
        expect(component.invalidToken).toBeFalsy();
    });

    it('should show invalid token error when no token in URL', () => {
        activatedRoute.queryParams = of({});
        const newFixture = TestBed.createComponent(ResetPasswordComponent);
        const newComponent = newFixture.componentInstance;
        newFixture.detectChanges();

        expect(newComponent.token).toBeUndefined();
        expect(newComponent.invalidToken).toBeTruthy();
        expect(newComponent.error).toBe('AUTH.RESET_PASSWORD.INVALID_TOKEN');
    });

    it('should have invalid form when fields are empty', () => {
        expect(component.resetPasswordForm.valid).toBeFalsy();
    });

    it('should have invalid form when passwords do not match', () => {
        component.resetPasswordForm.controls['password'].setValue('password123');
        component.resetPasswordForm.controls['confirmPassword'].setValue('different123');
        expect(component.resetPasswordForm.valid).toBeFalsy();
        expect(component.resetPasswordForm.hasError('passwordMismatch')).toBeTruthy();
    });

    it('should have invalid form when password is too short', () => {
        component.resetPasswordForm.controls['password'].setValue('short');
        component.resetPasswordForm.controls['confirmPassword'].setValue('short');
        expect(component.resetPasswordForm.valid).toBeFalsy();
        expect(component.resetPasswordForm.get('password')?.hasError('minlength')).toBeTruthy();
    });

    it('should have valid form when passwords match and meet requirements', () => {
        component.resetPasswordForm.controls['password'].setValue('password123');
        component.resetPasswordForm.controls['confirmPassword'].setValue('password123');
        expect(component.resetPasswordForm.valid).toBeTruthy();
    });

    it('should call API and show success message on successful password reset', async () => {
        apiSpy.invoke.and.returnValue(Promise.resolve(null));

        component.resetPasswordForm.controls['password'].setValue('newpassword123');
        component.resetPasswordForm.controls['confirmPassword'].setValue('newpassword123');
        await component.onSubmit();

        expect(apiSpy.invoke).toHaveBeenCalled();
        expect(component.success).toBeTruthy();
        expect(component.error).toBe('');
        expect(component.isSubmitting).toBeFalsy();
    });

    it('should show error message on API failure', async () => {
        apiSpy.invoke.and.returnValue(Promise.reject(new Error('API Error')));

        component.resetPasswordForm.controls['password'].setValue('newpassword123');
        component.resetPasswordForm.controls['confirmPassword'].setValue('newpassword123');
        component.onSubmit();

        // Wait for the promise to reject
        await new Promise(resolve => setTimeout(resolve, 10));

        expect(apiSpy.invoke).toHaveBeenCalled();
        expect(component.success).toBeFalsy();
        expect(component.error).toBe('AUTH.RESET_PASSWORD.ERROR');
        expect(component.isSubmitting).toBeFalsy();
    });

    it('should not submit when form is invalid', async () => {
        await component.onSubmit();
        expect(apiSpy.invoke).not.toHaveBeenCalled();
    });

    it('should not submit when already submitting', async () => {
        component.resetPasswordForm.controls['password'].setValue('newpassword123');
        component.resetPasswordForm.controls['confirmPassword'].setValue('newpassword123');
        component.isSubmitting = true;

        await component.onSubmit();
        expect(apiSpy.invoke).not.toHaveBeenCalled();
    });

    it('should reset form after successful submission', async () => {
        apiSpy.invoke.and.returnValue(Promise.resolve(null));

        component.resetPasswordForm.controls['password'].setValue('newpassword123');
        component.resetPasswordForm.controls['confirmPassword'].setValue('newpassword123');
        await component.onSubmit();

        expect(component.resetPasswordForm.value.password).toBeNull();
        expect(component.resetPasswordForm.value.confirmPassword).toBeNull();
    });
});

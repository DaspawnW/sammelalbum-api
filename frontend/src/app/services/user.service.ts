import { Injectable, inject } from '@angular/core';
import { Observable, from } from 'rxjs';
import { Api } from '../api/api';
import { getMe } from '../api/fn/user/get-me';
import { updateProfile } from '../api/fn/user/update-profile';
import { changePassword } from '../api/fn/user/change-password';
import { UserDto } from '../api/models/user-dto';
import { UpdateProfileRequest } from '../api/models/update-profile-request';
import { ChangePasswordRequest } from '../api/models/change-password-request';

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private api = inject(Api);

    getProfile(): Observable<UserDto> {
        return from(this.api.invoke(getMe));
    }

    updateProfile(request: UpdateProfileRequest): Observable<void> {
        return from(this.api.invoke(updateProfile, { body: request }));
    }

    changePassword(request: ChangePasswordRequest): Observable<void> {
        return from(this.api.invoke(changePassword, { body: request }));
    }
}

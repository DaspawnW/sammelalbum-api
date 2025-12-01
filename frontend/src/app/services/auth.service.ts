import { Injectable, signal } from '@angular/core';
import { Api } from '../api/api';
import { login } from '../api/fn/authentication/login';
import { register } from '../api/fn/authentication/register';
import { LoginRequest } from '../api/models/login-request';
import { RegisterRequest } from '../api/models/register-request';
import { AuthResponse } from '../api/models/auth-response';
import { tap, switchMap } from 'rxjs/operators';
import { Observable, from } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_ID_KEY = 'user_id';

  isAuthenticated = signal<boolean>(!!this.getToken());

  constructor(private api: Api) { }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return from(this.api.invoke(login, { body: credentials })).pipe(
      switchMap(async (response: any) => {
        // Handle blob response from generated API client
        if (response instanceof Blob) {
          const text = await response.text();
          return JSON.parse(text) as AuthResponse;
        }
        return response as AuthResponse;
      }),
      tap((response) => {

        this.setSession(response);
      })
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return from(this.api.invoke(register, { body: data })).pipe(
      switchMap(async (response: any) => {
        // Handle blob response from generated API client
        if (response instanceof Blob) {
          const text = await response.text();
          return JSON.parse(text) as AuthResponse;
        }
        return response as AuthResponse;
      }),
      tap((response) => {

        this.setSession(response);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_ID_KEY);
    this.isAuthenticated.set(false);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private setSession(response: AuthResponse): void {

    if (response.token) {

      localStorage.setItem(this.TOKEN_KEY, response.token);
      this.isAuthenticated.set(true);
    } else {

    }
    if (response.userId) {
      localStorage.setItem(this.USER_ID_KEY, response.userId.toString());
    }
  }
}

export interface AuthUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  avatarUrl: string | null;
  roles: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface VerifyResetTokenRequest {
  token: string;
}

export interface VerifyResetTokenResponse {
  token: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface MessageResponse {
  message?: string;
  error?: string;
}

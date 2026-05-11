import request from './request'
import type {
  ApiResponse,
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '../types/auth'

export function login(data: LoginRequest) {
  return request.post<ApiResponse<LoginResponse>, ApiResponse<LoginResponse>>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<ApiResponse<RegisterResponse>, ApiResponse<RegisterResponse>>('/auth/register', data)
}

export function getCurrentUser() {
  return request.get<ApiResponse<CurrentUser>, ApiResponse<CurrentUser>>('/auth/me')
}

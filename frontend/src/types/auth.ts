export type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  token: string
  userId: number
  username: string
}

export type RegisterRequest = {
  username: string
  email?: string
  password: string
  confirmPassword: string
}

export type RegisterResponse = number

export type CurrentUser = {
  userId: number
  username: string
  nickname?: string
  role?: string
  status?: number
  createdAt?: string
  updatedAt?: string
}

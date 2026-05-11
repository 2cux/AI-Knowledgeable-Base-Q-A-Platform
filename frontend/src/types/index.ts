export type ApiResult<T> = {
  code: number
  message: string
  data: T
}

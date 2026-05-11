const TOKEN_KEY = 'aikb_token'

export function saveToken(token: string) {
  const normalizedToken = token.trim()

  if (!normalizedToken) {
    removeToken()
    return
  }

  localStorage.setItem(TOKEN_KEY, normalizedToken)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

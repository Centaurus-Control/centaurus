const accessTokenKey: string = "centaurus.accessToken";
const expiresAtKey: string = "centaurus.accessTokenExpiresAt";

export interface StoredAccessToken {
  accessToken: string;
  expiresAt: string;
}

export function readStoredAccessToken(): StoredAccessToken | null {
  const accessToken: string | null = window.sessionStorage.getItem(accessTokenKey);
  const expiresAt: string | null = window.sessionStorage.getItem(expiresAtKey);

  if (accessToken === null || expiresAt === null) {
    return null;
  }

  return {
    accessToken,
    expiresAt
  };
}

export function writeStoredAccessToken(token: StoredAccessToken): void {
  window.sessionStorage.setItem(accessTokenKey, token.accessToken);
  window.sessionStorage.setItem(expiresAtKey, token.expiresAt);
}

export function clearStoredAccessToken(): void {
  window.sessionStorage.removeItem(accessTokenKey);
  window.sessionStorage.removeItem(expiresAtKey);
}

/** Compare parsed origins and path boundaries: similar hostnames must never receive the token. */
export function isApiRequest(requestUrl: string, apiUrl: string, pageOrigin: string): boolean {
  try {
    const target = new URL(requestUrl, pageOrigin);
    const api = new URL(apiUrl, pageOrigin);
    const path = api.pathname.replace(/\/+$/, '');
    return target.origin === api.origin &&
      (path === '' || target.pathname === path || target.pathname.startsWith(path + '/'));
  } catch {
    return false;
  }
}


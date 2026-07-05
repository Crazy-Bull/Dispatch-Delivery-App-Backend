const API_BASE = '/api';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function parseBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function errorMessage(body: unknown, status: number): string {
  if (typeof body === 'string' && body.trim()) return body;
  if (body && typeof body === 'object') {
    if ('message' in body && body.message) return String(body.message);
    if ('error' in body && body.error) return String(body.error);
  }
  if (status === 401) return 'Unauthorized — log in again';
  if (status === 403) return 'Forbidden — check request parameters or permissions';
  if (status === 404) return 'Not found';
  return `Request failed (HTTP ${status})`;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const body = await parseBody(response);

  if (!response.ok) {
    throw new ApiError(errorMessage(body, response.status), response.status);
  }

  return body as T;
}

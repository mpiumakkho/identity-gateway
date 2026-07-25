export type ApiResponse<T> = {
  status: "success" | "error";
  code: string;
  message: string;
  data: T | null;
  timestamp: string;
};

type RequestOptions = {
  accessToken?: string;
  payload?: unknown;
};

type HttpMethod = "GET" | "POST" | "PUT";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

export async function getJson<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  return requestJson<T>(path, "GET", options);
}

export async function postJson<T>(path: string, payload?: unknown, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  return requestJson<T>(path, "POST", { ...options, payload });
}

export async function putJson<T>(path: string, payload?: unknown, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  return requestJson<T>(path, "PUT", { ...options, payload });
}

async function requestJson<T>(path: string, method: HttpMethod, options: RequestOptions): Promise<ApiResponse<T>> {
  const headers = new Headers();

  if (options.payload !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (options.accessToken) {
    headers.set("Authorization", `Bearer ${options.accessToken}`);
  }

  const response = await fetch(path, {
    method,
    headers,
    body: options.payload === undefined ? undefined : JSON.stringify(options.payload)
  });

  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok) {
    throw new ApiError(response.status, body.code, body.message || `Request failed with status ${response.status}`);
  }

  return body;
}

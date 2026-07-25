export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiResponse<T> = {
  status: "success" | "error";
  code: string;
  message: string;
  data: T | null;
  errors: ApiFieldError[] | null;
  timestamp: string;
};

type RequestOptions = {
  accessToken?: string;
  payload?: unknown;
};

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly errors: ApiFieldError[];

  constructor(status: number, code: string, message: string, errors: ApiFieldError[] = []) {
    super(formatApiErrorMessage(message, errors));
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.errors = errors;
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

export async function deleteJson<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  return requestJson<T>(path, "DELETE", options);
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
    throw new ApiError(
      response.status,
      body.code,
      body.message || `Request failed with status ${response.status}`,
      normalizeFieldErrors(body.errors)
    );
  }

  return body;
}

function normalizeFieldErrors(errors: ApiResponse<unknown>["errors"] | unknown): ApiFieldError[] {
  if (!Array.isArray(errors)) {
    return [];
  }

  return errors.flatMap((error) => {
    if (!isApiFieldError(error)) {
      return [];
    }

    return [{ field: error.field, message: error.message }];
  });
}

function isApiFieldError(error: unknown): error is ApiFieldError {
  return Boolean(
    error
    && typeof error === "object"
    && "field" in error
    && "message" in error
    && typeof error.field === "string"
    && typeof error.message === "string"
  );
}

function formatApiErrorMessage(message: string, errors: ApiFieldError[]) {
  if (errors.length === 0) {
    return message;
  }

  return errors
    .map((error) => `${formatFieldName(error.field)}: ${error.message}`)
    .join("; ");
}

function formatFieldName(field: string) {
  return field
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .replace(/^./, (value) => value.toUpperCase());
}
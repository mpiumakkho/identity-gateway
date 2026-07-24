export type ApiResponse<T> = {
  status: "success" | "error";
  code: string;
  message: string;
  data: T | null;
  timestamp: string;
};

export async function postJson<T>(path: string, payload: unknown): Promise<ApiResponse<T>> {
  const response = await fetch(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok) {
    throw new Error(body.message || `Request failed with status ${response.status}`);
  }

  return body;
}
const API_BASE = (import.meta.env.VITE_API_BASE || "").trim().replace(/\/+$/, "");
const TOKEN_KEY = "pm_react_token";

const jsonHeaders = {
  "Content-Type": "application/json"
};

export async function request(path, options = {}, token = "") {
  const headers = {
    ...jsonHeaders,
    ...(options.headers || {})
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const fetchOptions = {
    ...options,
    headers
  };

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, fetchOptions);
  } catch (error) {
    const shouldFallbackToSameOrigin = Boolean(API_BASE) && /^https?:\/\//i.test(API_BASE);
    if (!shouldFallbackToSameOrigin) {
      throw error;
    }
    response = await fetch(path, fetchOptions);
  }

  if (!response.ok) {
    const body = await response.text();
    const error = new Error(body || `Request failed (${response.status})`);
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) return null;

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }

  return null;
}

export function getSavedToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function saveToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function extractRole(token) {
  if (!token) return "";

  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    const json = JSON.parse(atob(padded));
    return (json.role || "").toUpperCase();
  } catch {
    return "";
  }
}

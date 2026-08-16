const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const TOKEN_KEY = 'docket_token';

/**
 * Stores the JWT token in localStorage.
 */
export function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

/**
 * Retrieves the JWT token from localStorage.
 * @returns {string|null}
 */
export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

/**
 * Removes the JWT token, effectively logging the user out.
 */
export function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
}

/**
 * Checks if a token exists in storage.
 * @returns {boolean}
 */
export function isAuthenticated() {
    return !!getToken();
}

/**
 * Thin fetch wrapper that auto-attaches the Authorization header
 * and handles JSON parsing. Throws on non-OK responses with the
 * server's error message if available.
 *
 * @param {string} path  — API path (e.g., "/api/auth/login")
 * @param {object} options — fetch options (method, body, etc.)
 * @returns {Promise<any>} parsed JSON response
 */
export async function apiFetch(path, options = {}) {
    const token = getToken();

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers,
    });

    if (response.status === 401 || response.status === 403) {
        clearToken();
        window.location.href = '/login';
        throw new Error(`Authentication failed (${response.status}). Please log in again.`);
    }

    // Handle no-content responses
    if (response.status === 204) {
        return null;
    }

    let data = null;
    const text = await response.text();
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (err) {
            data = { error: { message: "Invalid JSON response from server" } };
        }
    }

    if (!response.ok) {
        const errorMessage = data?.error?.message || data?.message || `Request failed with status ${response.status}`;
        const error = new Error(errorMessage);
        error.status = response.status;
        throw error;
    }

    return data;
}

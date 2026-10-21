const API_BASE_URL = "http://localhost:8080/api";

const TOKEN_KEY = "algoviz-token";
const REFRESH_TOKEN_KEY = "algoviz-refresh-token";
const USER_KEY = "algoviz-user";

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);

export const setAuthData = (token, user, refreshToken) => {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  localStorage.setItem("algoviz-auth", "true");
  if (user?.username || user?.fullName) {
    localStorage.setItem("algoviz-user-name", user.fullName || user.username);
  }
};

export const clearAuthData = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.setItem("algoviz-auth", "false");
  localStorage.removeItem("algoviz-user-name");
};

export const getUserData = () => {
  const data = localStorage.getItem(USER_KEY);
  try {
    return data ? JSON.parse(data) : null;
  } catch {
    return null;
  }
};

let refreshPromise = null;

async function doRefreshToken() {
  const currentRefreshToken = getRefreshToken();
  if (!currentRefreshToken) {
    clearAuthData();
    window.dispatchEvent(new Event("algoviz:logout"));
    throw new Error("No refresh token available");
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: currentRefreshToken }),
  });

  if (!response.ok) {
    clearAuthData();
    window.dispatchEvent(new Event("algoviz:logout"));
    throw new Error("Refresh token expired or invalid");
  }

  const data = await response.json();
  if (data?.token) {
    setAuthData(data.token, data.user, data.refreshToken);
    return data.token;
  }
  throw new Error("Failed to parse refresh token response");
}

async function request(endpoint, options = {}, isRetry = false) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  // Auto-refresh token if 401 Unauthorized and not already refreshing/retrying
  if (response.status === 401 && !isRetry && !endpoint.startsWith("/auth/")) {
    if (token?.startsWith("guest-session-") || token?.startsWith("offline-session-")) {
      throw new Error("Local/guest session cannot authenticate with live backend");
    }

    if (!refreshPromise) {
      refreshPromise = doRefreshToken().finally(() => {
        refreshPromise = null;
      });
    }

    try {
      await refreshPromise;
      return await request(endpoint, options, true);
    } catch {
      throw new Error("Session expired. Please sign in again.");
    }
  }

  if (!response.ok) {
    let errorMessage = `HTTP Error ${response.status}`;
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorData.error || errorMessage;
    } catch {
      // ignore json parse error
    }
    throw new Error(errorMessage);
  }

  // Check if response has content before parsing JSON
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return await response.json();
  }
  return null;
}

export const api = {
  // Auth API
  login: async (usernameOrEmail, password) => {
    try {
      const data = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify({ usernameOrEmail, password }),
      });
      if (data && data.token) {
        setAuthData(data.token, data.user, data.refreshToken);
        return data;
      }
      return data;
    } catch (err) {
      const isNetworkError =
        err.name === "TypeError" ||
        err.message.includes("Failed to fetch") ||
        err.message.includes("NetworkError") ||
        err.message.includes("Load failed");

      if (isNetworkError) {
        console.info("Backend offline: creating local developer session for", usernameOrEmail);
        const namePart = (usernameOrEmail || "developer").split("@")[0].replace(/[^a-zA-Z0-9_]/g, "");
        const user = {
          id: 1,
          username: namePart || "developer",
          fullName: namePart || "Developer",
          email: usernameOrEmail.includes("@") ? usernameOrEmail : `${usernameOrEmail}@example.com`,
        };
        const mockToken = "offline-session-" + Date.now();
        setAuthData(mockToken, user, "offline-refresh-" + Date.now());
        return { token: mockToken, user, offline: true };
      }
      throw err;
    }
  },

  register: async (username, email, password, fullName) => {
    try {
      const data = await request("/auth/register", {
        method: "POST",
        body: JSON.stringify({ username, email, password, fullName: fullName || username }),
      });
      if (data && data.token) {
        setAuthData(data.token, data.user, data.refreshToken);
        return data;
      }
      return data;
    } catch (err) {
      const isNetworkError =
        err.name === "TypeError" ||
        err.message.includes("Failed to fetch") ||
        err.message.includes("NetworkError") ||
        err.message.includes("Load failed");

      if (isNetworkError) {
        console.info("Backend offline: creating local registered session for", username);
        const cleanUser = username || (email ? email.split("@")[0] : "user");
        const user = {
          id: Date.now(),
          username: cleanUser,
          fullName: fullName || cleanUser,
          email,
        };
        const mockToken = "offline-session-" + Date.now();
        setAuthData(mockToken, user, "offline-refresh-" + Date.now());
        return { token: mockToken, user, offline: true };
      }
      throw err;
    }
  },

  logout: async () => {
    try {
      const refreshToken = getRefreshToken();
      await request("/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      });
    } catch (err) {
      console.warn("Logout notification error (client cleared regardless):", err);
    } finally {
      clearAuthData();
      window.dispatchEvent(new Event("algoviz:logout"));
    }
  },

  // Problems API
  getProblems: (difficulty = "", category = "") => {
    const params = new URLSearchParams();
    if (difficulty) params.append("difficulty", difficulty);
    if (category) params.append("category", category);
    const queryString = params.toString() ? `?${params.toString()}` : "";
    return request(`/problems${queryString}`);
  },

  getProblemById: (id) => {
    return request(`/problems/${id}`);
  },

  // Submissions API
  runCode: (language, code, customInput = "") => {
    return request("/submissions/run", {
      method: "POST",
      body: JSON.stringify({ language, code, customInput }),
    });
  },

  submitCode: (problemId, language, code) => {
    return request("/submissions/submit", {
      method: "POST",
      body: JSON.stringify({ problemId, language, code }),
    });
  },

  getSubmissionHistory: () => {
    return request("/submissions/history");
  },

  getProblemSubmissions: (problemId) => {
    return request(`/submissions/problem/${problemId}`);
  },

  // AI Analysis API
  analyzeCode: (code, language = "python", problemContext = "") => {
    return request("/ai/analyze", {
      method: "POST",
      body: JSON.stringify({ code, language, problemContext }),
    });
  },

  // User Profile API
  getProfile: () => {
    return request("/users/me");
  },
};

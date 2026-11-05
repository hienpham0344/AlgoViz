const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
const TOKEN_KEY = "algoviz-token";
const REFRESH_TOKEN_KEY = "algoviz-refresh-token";
const USER_KEY = "algoviz-user";

export const getToken = () => {
  const token = localStorage.getItem(TOKEN_KEY);
  return token && !/^(guest|offline)-session-/.test(token) ? token : null;
};
export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);
export const getUserData = () => {
  try { return JSON.parse(localStorage.getItem(USER_KEY) || "null"); }
  catch { return null; }
};

export function setAuthData(token, user, refreshToken) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    localStorage.setItem("algoviz-user-name", user.fullName || user.username || "");
  }
  localStorage.setItem("algoviz-auth", "true");
}

export function clearAuthData() {
  [TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY, "algoviz-user-name", "algoviz-user-profile"]
    .forEach((key) => localStorage.removeItem(key));
  localStorage.setItem("algoviz-auth", "false");
}

function expireSession() {
  clearAuthData();
  window.dispatchEvent(new Event("algoviz:logout"));
}

let refreshPromise = null;
async function refreshSession() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    expireSession();
    throw new Error("Session expired. Please sign in again.");
  }
  try {
    const data = await request("/auth/refresh", {
      method: "POST", body: JSON.stringify({ refreshToken }),
    }, { public: true });
    if (getRefreshToken() !== refreshToken) throw new Error("The active session changed.");
    if (!data?.token) {
      const error = new Error("Session expired. Please sign in again.");
      error.status = 401;
      throw error;
    }
    setAuthData(data.token, data.user, data.refreshToken);
  } catch (error) {
    // A temporary outage must not erase an otherwise valid refresh token.
    if (getRefreshToken() !== refreshToken || !error.status || error.status >= 500) throw error;
    expireSession();
    throw new Error("Session expired. Please sign in again.", { cause: error });
  }
}

async function request(endpoint, options = {}, settings = {}) {
  const token = settings.public ? null : getToken();
  const headers = { "Content-Type": "application/json", ...options.headers };
  if (token) headers.Authorization = `Bearer ${token}`;
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options, headers, signal: options.signal || AbortSignal.timeout(60000),
    });
  } catch (cause) {
    throw new Error("Cannot connect to the server. Please try again.", { cause });
  }
  if (response.status === 401 && !settings.public && !settings.retry) {
    if (!refreshPromise) {
      refreshPromise = refreshSession().finally(() => { refreshPromise = null; });
    }
    await refreshPromise;
    return request(endpoint, options, { ...settings, retry: true });
  }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && !settings.public) expireSession();
    const details = data?.data && typeof data.data === "object"
      ? Object.values(data.data).filter((value) => typeof value === "string").join(". ") : "";
    const error = new Error(details || data?.message || data?.error || `Request failed (${response.status}).`);
    error.status = response.status;
    throw error;
  }
  if (data === null) throw new Error("The server returned an invalid response. Please try again.");
  return data;
}

async function authenticate(endpoint, credentials) {
  const data = await request(endpoint, {
    method: "POST", body: JSON.stringify(credentials),
  }, { public: true });
  if (!data?.token || !data?.user) throw new Error("The server returned an invalid sign-in response.");
  clearAuthData();
  setAuthData(data.token, data.user, data.refreshToken);
  return data;
}

export const api = {
  login: (usernameOrEmail, password) => authenticate("/auth/login", { usernameOrEmail, password }),
  register: (username, email, password, fullName) => authenticate("/auth/register", {
    username, email, password, fullName: fullName || username,
  }),
  logout: async () => {
    try {
      await request("/auth/logout", {
        method: "POST", body: JSON.stringify({ refreshToken: getRefreshToken() }),
      }, { retry: true });
    } finally { expireSession(); }
  },
  getProblems: (difficulty = "", category = "") => {
    const params = new URLSearchParams();
    if (difficulty) params.set("difficulty", difficulty);
    if (category) params.set("category", category);
    return request(`/problems${params.size ? `?${params}` : ""}`, {}, { public: true });
  },
  getProblemById: (id) => request(`/problems/${id}`, {}, { public: true }),
  runCode: (language, code, customInput = "") => request("/submissions/run", {
    method: "POST", body: JSON.stringify({ language, code, customInput }),
  }, { public: true }),
  submitCode: (problemId, language, code) => request("/submissions/submit", {
    method: "POST", body: JSON.stringify({ problemId, language, code }),
  }),
  getSubmissionHistory: () => request("/submissions/history"),
  getProblemSubmissions: (problemId) => request(`/submissions/problem/${problemId}`),
  deleteSubmission: (id) => request(`/submissions/${id}`, { method: "DELETE" }),
  analyzeCode: (code, language = "python", problemContext = "") => request("/ai/analyze", {
    method: "POST", body: JSON.stringify({ code, language, problemContext }),
  }, { public: true }),
  getProfile: () => request("/users/me"),
  getLearningProgress: () => request("/users/learning-progress"),
  saveLearningProgress: (courseId, lessonId, completed) => request("/users/learning-progress", {
    method: "PUT", body: JSON.stringify({ courseId, lessonId, completed }),
  }),
  updateProfile: async (profile) => {
    const user = await request("/users/profile", { method: "PUT", body: JSON.stringify(profile) });
    setAuthData(null, user);
    window.dispatchEvent(new Event("algoviz:profile-updated"));
    return user;
  },
  changePassword: (currentPassword, newPassword) => request("/users/password", {
    method: "PUT", body: JSON.stringify({ currentPassword, newPassword }),
  }),
};

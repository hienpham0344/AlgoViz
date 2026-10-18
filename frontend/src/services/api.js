const API_BASE_URL = "http://localhost:8080/api";

const TOKEN_KEY = "algoviz-token";
const USER_KEY = "algoviz-user";

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setAuthData = (token, user) => {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  localStorage.setItem("algoviz-auth", "true");
  if (user?.username || user?.fullName) {
    localStorage.setItem("algoviz-user-name", user.fullName || user.username);
  }
};

export const clearAuthData = () => {
  localStorage.removeItem(TOKEN_KEY);
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

async function request(endpoint, options = {}) {
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
    const data = await request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ usernameOrEmail, password }),
    });
    if (data && data.token) {
      setAuthData(data.token, data.user);
    }
    return data;
  },

  register: async (username, email, password, fullName) => {
    const data = await request("/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, email, password, fullName: fullName || username }),
    });
    if (data && data.token) {
      setAuthData(data.token, data.user);
    }
    return data;
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

  deleteSubmission: (submissionId) => {
    return request(`/submissions/${submissionId}`, {
      method: "DELETE",
    });
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

  updateProfile: (fullName, avatarUrl = "") => {
    return request("/users/profile", {
      method: "PUT",
      body: JSON.stringify({ fullName, avatarUrl }),
    });
  },
};

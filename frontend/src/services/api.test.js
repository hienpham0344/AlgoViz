import assert from "node:assert/strict";
import { beforeEach, test } from "node:test";
import { api, clearAuthData, getToken, setAuthData } from "./api.js";

const storage = new Map();
globalThis.localStorage = {
  getItem: (key) => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, String(value)),
  removeItem: (key) => storage.delete(key),
};
globalThis.window = new EventTarget();
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: { "Content-Type": "application/json" },
});
beforeEach(() => storage.clear());

test("network failures never create a signed-in account", async () => {
  globalThis.fetch = async () => { throw new TypeError("Failed to fetch"); };
  await assert.rejects(api.login("developer", "password"));
  await assert.rejects(api.register("developer", "dev@example.com", "password", "Dev"));
  assert.equal(getToken(), null);
  assert.notEqual(localStorage.getItem("algoviz-auth"), "true");
});

test("legacy demo sessions are not backend credentials", () => {
  localStorage.setItem("algoviz-token", "offline-session-old");
  assert.equal(getToken(), null);
  localStorage.setItem("algoviz-token", "guest-session-old");
  assert.equal(getToken(), null);
});

test("login requires a valid authentication response", async () => {
  globalThis.fetch = async () => json({ message: "unexpected response" });
  await assert.rejects(api.login("developer", "password"));
  assert.equal(getToken(), null);
});

test("parallel unauthorized calls share one refresh and retry with the new token", async () => {
  setAuthData("old-token", { username: "dev" }, "refresh-token");
  let refreshes = 0;
  globalThis.fetch = async (url, options) => {
    if (url.endsWith("/auth/refresh")) {
      refreshes++;
      await new Promise((resolve) => setTimeout(resolve, 10));
      return json({ token: "new-token", refreshToken: "new-refresh", user: { username: "dev" } });
    }
    return options.headers.Authorization === "Bearer new-token"
      ? json({ username: "dev" }) : json({ message: "Expired" }, 401);
  };
  const profiles = await Promise.all([api.getProfile(), api.getProfile()]);
  assert.equal(refreshes, 1);
  assert.equal(profiles[0].username, "dev");
  assert.equal(getToken(), "new-token");
});

test("public problem browsing never refreshes or signs out a session", async () => {
  setAuthData("old-token", { username: "dev" }, "refresh-token");
  globalThis.fetch = async (_url, options) => {
    assert.equal(options.headers.Authorization, undefined);
    return json([]);
  };
  assert.deepEqual(await api.getProblems(), []);
  assert.equal(getToken(), "old-token");
});

test("failed refresh clears credentials and notifies the app", async () => {
  setAuthData("old-token", { username: "dev" }, "refresh-token");
  let loggedOut = false;
  window.addEventListener("algoviz:logout", () => { loggedOut = true; }, { once: true });
  globalThis.fetch = async () => json({ message: "Expired" }, 401);
  await assert.rejects(api.getProfile(), /sign in again/i);
  assert.equal(getToken(), null);
  assert.equal(loggedOut, true);
});

test("code execution failures preserve the server error", async () => {
  globalThis.fetch = async () => json({ message: "Execution unavailable" }, 503);
  await assert.rejects(api.runCode("python", "print(1)"), /Execution unavailable/);
});

test("profile changes reach the backend and update the cached identity", async () => {
  setAuthData("token", { username: "dev" }, "refresh");
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "/api/users/profile");
    assert.equal(options.method, "PUT");
    assert.equal(JSON.parse(options.body).fullName, "New Name");
    return json({ username: "dev", fullName: "New Name" });
  };
  await api.updateProfile({ fullName: "New Name" });
  assert.equal(localStorage.getItem("algoviz-user-name"), "New Name");
  clearAuthData();
  assert.equal(localStorage.getItem("algoviz-refresh-token"), null);
});

test("malformed refresh responses expire the session", async () => {
  setAuthData("old", { username: "dev" }, "refresh");
  globalThis.fetch = async (url) => url.endsWith("/auth/refresh") ? json({}) : json({}, 401);
  await assert.rejects(api.getProfile());
  assert.equal(getToken(), null);
});

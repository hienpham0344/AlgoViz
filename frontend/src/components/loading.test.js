import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import { fileURLToPath } from "node:url";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { createServer } from "vite";
import react from "@vitejs/plugin-react";

let server;
let ProgressView;
let ApiState;
const render = (Component, props) => renderToStaticMarkup(createElement(Component, props));

before(async () => {
  server = await createServer({
    configFile: false,
    root: fileURLToPath(new URL("../../", import.meta.url)),
    plugins: [react()],
    optimizeDeps: { noDiscovery: true, include: [] },
    server: { middlewareMode: true, hmr: false, ws: false, watch: null },
  });
  ProgressView = (await server.ssrLoadModule("/src/components/ProgressView.jsx")).default;
  ApiState = (await server.ssrLoadModule("/src/components/ApiState.jsx")).default;
});
after(async () => { await server?.close(); });

test("ring uses the same percentage for its arc, visible text and ARIA range", () => {
  const html = render(ProgressView, { variant: "ring", value: 75, min: 50, max: 150, showValue: true });
  assert.match(html, /role="progressbar"/);
  assert.match(html, /aria-valuemin="0"/);
  assert.match(html, /aria-valuemax="100"/);
  assert.match(html, /aria-valuenow="25"/);
  assert.match(html, />25%<\/text>/);
  const circumference = Number(html.match(/stroke-dasharray="([^"]+)"/)[1]);
  const offset = Number(html.match(/stroke-dashoffset="([^"]+)"/)[1]);
  assert.ok(Math.abs(offset / circumference - 0.75) < 0.00001);
});

test("native linear progress normalizes custom ranges to the visible percentage", () => {
  const html = render(ProgressView, { value: 7, min: 2, max: 10, showValue: true });
  assert.match(html, /<progress[^>]*value="63"[^>]*max="100"/);
  assert.match(html, /aria-valuenow="63"/);
  assert.match(html, />63%<\/span>/);
});

test("unknown or invalid progress shows a spinner without a fabricated percentage", () => {
  for (const props of [{}, { value: null, variant: "ring" }, { value: NaN }, { value: Infinity },
    { value: 20, min: 10, max: 10 }, { value: 20, max: Infinity }]) {
    const html = render(ProgressView, { ...props, label: "Loading results", showValue: true });
    assert.match(html, /progress-view-spinner/);
    assert.match(html, /role="progressbar"/);
    assert.doesNotMatch(html, /aria-valuenow|NaN|Infinity|\d+%/);
    assert.equal((html.match(/>Loading results</g) || []).length, 1);
  }
});

test("known zero and out-of-range progress stay determinate and clamped", () => {
  for (const [value, percent] of [[0, 0], [-5, 0], [150, 100]]) {
    const html = render(ProgressView, { variant: "ring", value, showValue: true });
    assert.match(html, new RegExp(`aria-valuenow="${percent}"`));
    assert.match(html, new RegExp(`>${percent}%</text>`));
  }
});

test("caller ARIA overrides cannot contradict the rendered value", () => {
  const html = render(ProgressView, { value: 42, "aria-valuenow": 99, "aria-valuemax": 200 });
  assert.match(html, /aria-valuenow="42"/);
  assert.doesNotMatch(html, /aria-valuemax="200"/);
});

test("pending API state hides stale errors and retry until the request settles", () => {
  const html = render(ApiState, { loading: true, error: "Old failure", label: "Loading profile…", onRetry() {} });
  assert.match(html, /progress-view-spinner/);
  assert.match(html, /Loading profile…/);
  assert.match(html, /aria-busy="true"/);
  assert.doesNotMatch(html, /Old failure|Try again|role="alert"/);
  assert.match(render(ApiState, { error: "Request failed", onRetry() {} }), /role="alert"/);
  assert.equal(render(ApiState, {}), "");
});

test("data pages render a loading indicator before statistics or empty results", async () => {
  for (const [path, props, prematureContent] of [
    ["problems/problems", {}, /<table|problems-main-layout|No problems match/],
    ["history/history", {}, /history-stats|history-table|No analyses/],
    ["learn/learn", { isAuthenticated: true }, /learn-your-courses-section|course-progress/],
    ["learn/LessonPage", { isAuthenticated: true }, /lesson-status-tag|sidebar-progress-meta/],
  ]) {
    const Page = (await server.ssrLoadModule(`/src/features/${path}.jsx`)).default;
    const html = render(Page, props);
    assert.match(html, /progress-view-spinner/, path);
    assert.doesNotMatch(html, prematureContent, path);
  }
});


test("guest learning content remains available without fetching personal progress", async () => {
  for (const path of ["learn/learn", "learn/LessonPage"]) {
    const Page = (await server.ssrLoadModule(`/src/features/${path}.jsx`)).default;
    const html = render(Page, { isAuthenticated: false });
    assert.match(html, /<h1/);
    assert.doesNotMatch(html, /Loading (learning|lesson) progress/);
  }
});

test("guest lesson presents readable math and asks to sign in before saving progress", async () => {
  const Page = (await server.ssrLoadModule("/src/features/learn/LessonPage.jsx")).default;
  const html = render(Page, { isAuthenticated: false, courseId: "sorting-masterclass", lessonId: 1 });
  assert.match(html, /lesson-math/);
  assert.match(html, /Sign in to save progress/);
  assert.match(html, /Preview/);
  assert.doesNotMatch(html, /\\le|\\dots|\$\$A\[i\]/);
});

test('the runtime visualizer renders captured state and invalidates it after editing', async () => {
  const Page = (await server.ssrLoadModule('/src/features/problems/UserCodeVisualizer.jsx')).default;
  const code = 'class Main { public static void main(String[] args) {} }';
  const trace = {source:code,customInput:'7',status:'Success',steps:[{lineNumber:1,code:'nums[0]++;',variables:[{variableName:'nums',visualValue:[7,2]}]}]};
  const props = { code, language:'Java', testCase:{input:'7'}, executionResult:{trace} };
  const html = render(Page, props);
  assert.match(html,/Runtime Trace/);
  assert.match(html,/AST \/ JDI/);
  assert.match(html,/Local Variable Scope/);
  assert.doesNotMatch(html,/Simulation|Engine Adapter Ready/);
  assert.match(render(Page,{...props,code:'edited'}),/Code or testcase changed/);
  assert.match(render(Page,{...props,loading:true}),/role="progressbar"/);
  assert.match(render(Page,{...props,language:'Python 3',executionResult:null}),/supports Java runtime traces/);
});

import assert from 'node:assert/strict';
import { test } from 'node:test';
import { checkAstToolHealth, fetchAlgorithmCatalog, fetchDebugSteps, recognizeAlgorithms } from './astApi.js';

test('AST health requires an explicit live engine response, not a generic OPTIONS or 404', async () => {
  const original = globalThis.fetch;
  const requests = [];
  globalThis.fetch = async (url, options) => {
    requests.push({ url, method: options?.method || 'GET' });
    return { ok: false, status: 404, json: async () => ({ message: 'Not found' }) };
  };
  try {
    assert.equal(await checkAstToolHealth(), false);
    assert.equal(requests[0].url, '/api/ast/health');
    assert.equal(requests[0].method, 'GET');
  } finally { globalThis.fetch = original; }
});

test('malformed debug payloads never become a successful trace', async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: true, json: async () => ({ message: 'not a trace' }) });
  try {
    await assert.rejects(fetchDebugSteps('class Main { public static void main(String[] args) {} }'), /trace|steps/i);
  } finally { globalThis.fetch = original; }
});

test('caller cancellation and request deadlines both abort the AST fetch', async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async (_url, { signal }) => new Promise((_resolve, reject) => {
    const abort = () => reject(new DOMException('Aborted','AbortError'));
    if (signal.aborted) abort();
    else signal.addEventListener('abort',abort,{once:true});
  });
  try {
    const controller = new AbortController();
    const request = fetchDebugSteps('class Main {}',{signal:controller.signal});
    controller.abort();
    await assert.rejects(request,{name:'AbortError'});
    await assert.rejects(fetchDebugSteps('class Main {}',{timeoutMs:10}),{name:'AbortError'});
  } finally { globalThis.fetch = original; }
});

test('algorithm catalog is fetched once and rejects malformed payloads', async () => {
  const original = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async (url) => {
    calls += 1;
    assert.equal(url, '/api/algorithms/catalog');
    return { ok: true, json: async () => ({ version: 'v1', algorithms: [{ type: 'bubble-sort', family: 'sort' }] }) };
  };
  try {
    const first = await fetchAlgorithmCatalog();
    const second = await fetchAlgorithmCatalog();
    assert.equal(first, second);
    assert.equal(calls, 1);
  } finally { globalThis.fetch = original; }
});

test('recognition posts the Java file and requires algorithmHints', async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/algorithms/recognize');
    assert.equal(options.method, 'POST');
    return { ok: true, json: async () => ({ catalogVersion: 'v1', algorithmHints: [] }) };
  };
  try {
    assert.deepEqual((await recognizeAlgorithms('class Main {}')).algorithmHints, []);
    globalThis.fetch = async () => ({ ok: true, json: async () => ({ message: 'nope' }) });
    await assert.rejects(recognizeAlgorithms('class Main {}'), /recognition/i);
  } finally { globalThis.fetch = original; }
});

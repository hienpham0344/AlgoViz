import test from 'node:test';
import assert from 'node:assert/strict';
import { runtimeFrames, traceMatchesEditor } from './runtimeTrace.js';

test('runtime frames display only captured values, source lines and variable names', () => {
  const source = 'int[] arr = {7, 2};\narr[0] = 10;';
  const response = { source, steps: [
    { lineNumber: 2, code: 'arr[0] = 10;', variables: [{variableName:'args',visualValue:[]}, {variableName:'arr',visualValue:[7,2]}, {variableName:'target',visualValue:99}] },
    { lineNumber: 2, code: 'arr[0] = 10;', variables: [{variableName:'arr',visualValue:[10,2]}] },
  ]};
  const frames = runtimeFrames(response);
  assert.equal(frames[0].line, 2);
  assert.equal(frames[0].arrayName, 'arr');
  assert.deepEqual(frames.map(f => f.nums), [[7,2],[10,2]]);
  assert.deepEqual(frames[0].activeIndices, []);
  assert.equal(frames[1].variables.target, undefined, 'out-of-scope values are never carried forward');
  assert.deepEqual(runtimeFrames({steps:[]}), []);
});

test('edited code or testcase invalidates a trace from an earlier run', () => {
  const trace = {source:'original',customInput:'9'};
  assert.equal(traceMatchesEditor(trace,'original','9'),true);
  assert.equal(traceMatchesEditor(trace,'edited','9'),false);
  assert.equal(traceMatchesEditor(trace,'original','6'),false);
});

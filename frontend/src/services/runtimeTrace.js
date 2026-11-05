import { formatRuntimeValue, pointersForVariable, sequenceValues } from './solutionTrace.js';

export function traceMatchesEditor(trace, code, input = '') {
  return Boolean(trace && trace.source === code && trace.customInput === input);
}

/** JDI observations describe the suspended frame before its current source location executes. */
export function runtimeFrames(response) {
  if (!Array.isArray(response?.steps)) return [];
  return response.steps.map((step, index) => {
    const variables = Array.isArray(step.variables) ? step.variables : [];
    const sequences = variables.filter(v => !v.variableName.startsWith("__algoviz") && sequenceValues(v) !== null);
    const sequence = sequences.find(v => sequenceValues(v).length > 0)
      || sequences.find(v => v.variableName !== "args") || sequences[0];
    const pointers = sequence ? pointersForVariable(sequence, step, response.source || '') : [];
    return {
      stepNumber: index + 1,
      line: step.lineNumber,
      arrayName: sequence?.variableName || '',
      nums: sequence ? sequenceValues(sequence).map(value => value !== null && typeof value === "object" ? JSON.stringify(value) : typeof value === "boolean" ? String(value) : value) : [],
      pointers: Object.fromEntries(pointers.map(p => [p.name, p.index])),
      activeIndices: pointers.map(p => p.index),
      variables: Object.fromEntries(variables.map(v => [v.variableName, formatRuntimeValue(v)])),
      annotation: `Before line ${step.lineNumber} · ${step.context?.methodName || 'main'}(): ${step.code || step.statementKind || 'statement'}`,
    };
  });
}

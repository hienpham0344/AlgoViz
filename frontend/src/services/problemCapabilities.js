const REFERENCE_TRACE_IDS = new Set([1, 704]);

export function hasReferenceTrace(problemId) {
  return REFERENCE_TRACE_IDS.has(Number(problemId));
}

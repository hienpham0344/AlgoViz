export function buildGenericProblem(id) {
  return {
    id,
    title: `Problem #${id}`,
    difficulty: "Unknown",
    topics: [],
    companies: [],
    hints: [],
    description: "",
    constraints: [],
    sampleTestCases: [],
    solutions: [],
  };
}

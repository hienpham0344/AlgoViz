# Implementation Plan: [Task / Feature Name]

- **Status**: [Draft | In-Progress | Completed]
- **Related Spec**: [`docs/specs/...`](file:///)
- **Strategy**: [Test-Driven Development / Subagent Dispatch / Incremental]

## Proposed Architecture & Seams
[Where does this change plug into the existing architecture? What are the boundaries?]

## Tasks Breakdown

### Task 1: [Short Task Title]
- **Target Files**:
  - `path/to/test.ts`
  - `path/to/implementation.ts`
- **Actions**:
  1. Write failing regression test reproducing required behavior.
  2. Implement surgical change.
  3. Run focused test suite and verify output.
- **Verification Command**: `pytest tests/test_feature.py` or `npm test -- feature.test.ts`

### Task 2: [Next Step]
...

## Verification Checklist
- [ ] All unit and integration tests passing
- [ ] No linting or TypeScript type errors
- [ ] Evidence captured before marking complete

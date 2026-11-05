# Enterprise Workflow: AI Model & Prompt Evaluation Pipeline

## 1. Overview
Establishes a continuous quality gate for LLM prompts, agent tool calling, and RAG pipelines. Prevents semantic regressions and hallucinations from reaching production users.

## 2. Participating Subagents
- **`ai_engineer`**: Configures evaluation metrics, datasets, and semantic judges.
- **`qa_reviewer`**: Reviews test coverage, regression thresholds, and token budgets.
- **`system_coordinator`**: Enforces pass/fail criteria before release sign-off.

## 3. Step-by-Step Execution Sequence

### Phase 1: Test Dataset Curation
1. Collect or generate 50+ diverse test cases across nominal and adversarial scenarios.
2. Store in version-controlled matrix `workflows/templates/eval-prompt-matrix.yaml`.
3. Include explicit ground-truth assertions or grading rubrics.

### Phase 2: Automated Benchmark Execution
1. Execute the evaluation suite against candidate prompts:
   ```bash
   python3 -m pytest tests/evals/test_rag_pipeline.py --tb=short
   ```
2. Compute Faithfulness, Context Recall, and Answer Relevancy scores.

### Phase 3: Quality Gate Verification
- **Gate 1**: Faithfulness score >= 0.85 (Zero tolerated hallucinations).
- **Gate 2**: Context Recall >= 0.80.
- **Gate 3**: Token usage within designated budget (< 4,000 prompt tokens per average turn).

### Phase 4: Promotion & Deployment
- Once all gates pass, update the production prompt manifest or model version lock.

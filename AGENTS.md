# Multi-Stack Engineering, Security & UI/UX Guidelines

This workspace is equipped with advanced UI/UX intelligence, anti-slop art direction, multi-stack subagents (Frontend, Java/JVM, Python, Security), and strict token-saving protocols.

---

## 1. Available Skills

### Design & Taste (Anti-Slop)
- **`design-taste-frontend` (`taste-skill`)**: Art direction, anti-generic aesthetic enforcement, distinctive typography, authentic color palettes, real-browser inspection.
- **`minimalist-skill` / `brutalist-skill` / `soft-skill`**: Targeted aesthetic directions.
- **`brandkit` / `redesign-skill`**: Brand identity creation and page redesign procedures.

### UI/UX Intelligence (UI/UX Pro Max)
- **`ui-ux-pro-max`**: Design intelligence engine covering 79 styles, 192 palettes, 74 font pairings, 119 UX guidelines, and 25 chart types.
  - Query command: `python3 /home/hien/.gemini/vendor/ui-ux-pro-max-skill/.claude/skills/ui-ux-pro-max/scripts/search.py "<query>" --domain <domain>`
- **`design-system`**: Design token architecture (colors, typography, spacing, elevation).
- **`ui-styling`**: Stack-specific styling best practices (Tailwind CSS, CSS modules).

### Engineering & Subagent Workflows (Superpowers)
- **`subagent-driven-development`**: Dispatch specialized implementer and reviewer subagents per task.
- **`dispatching-parallel-agents`**: Concurrent subagent task orchestration.
- **`test-driven-development`**: Strict test-first engineering workflow.
- **`verification-before-completion`**: Pre-delivery QA and validation checklists.
- **`systematic-debugging`**: Root-cause analysis and bug diagnosis.
- **`code-review-excellence`** & **`error-handling-patterns`**: Code quality and fault-tolerance patterns.

### Backend & Language Engineering (Java & Python)
- **Java/JVM**: Work with actual project build tools (`pom.xml`, `build.gradle`), framework standards, and test suites.
- **Python**: `pydantic-v2`, `python-type-safety`, `python-testing-patterns`, `python-code-style`, `python-project-structure`, `python-monorepo-architecture`.

### Application Security & Threat Modeling
- **`stride-analysis-patterns`**: STRIDE methodology for threat modeling.
- **`attack-tree-construction`**: Attack scenario visualization and defense gap identification.
- **`security-requirement-extraction`**: Translating threats into actionable security requirements.
- **`threat-mitigation-mapping`**: Mapping threats to security controls.
- **`sast-configuration`** & **`external-content-sanitizer`**: Automated SAST and prompt injection / untrusted content defense.

---

## 2. Configured Subagents (`agents.toml`)

Subagents are defined in [`.agents/agents.toml`](file:///home/hien/Project/AlgoViz/.agents/agents.toml) and registered live in Antigravity:

| Subagent Name | Role | Core Skills & Focus |
| :--- | :--- | :--- |
| **`frontend_architect`** | Frontend Architect & System Planner | Component architecture, state design, plan drafting (`writing-plans`, `ui-ux-pro-max`) |
| **`ui_ux_designer`** | UI/UX Intelligence & Design Specialist | Color harmony, typography, accessibility (WCAG AA), UI tokens (`ui-ux-pro-max`, `design-system`) |
| **`taste_craftsman`** | Anti-Slop Art Director & Visual Polish | Distinctive character, editorial typography, anti-AI-slop styling (`design-taste-frontend`) |
| **`frontend_developer`** | Frontend Component Implementer | Clean React/TypeScript/Tailwind implementation, test-first code (`test-driven-development`) |
| **`qa_reviewer`** | QA, Accessibility & Code Reviewer | Automated validation, accessibility audit, code review (`verification-before-completion`) |
| **`java_pro`** | Java & JVM Engineering Specialist | Java/Spring Boot development, testing, build tools, performance (`debugging-strategies`, `code-review-excellence`) |
| **`python_backend`** | Python Architecture & Backend Implementer | Pydantic v2, strict typing, clean architecture, pytest suites (`pydantic-v2`, `python-testing-patterns`) |
| **`security_auditor`** | Application Security & Threat Modeling | Threat modeling (STRIDE), attack trees, SAST, content sanitization (`stride-analysis-patterns`, `attack-tree-construction`) |

---

## 3. Strict Token Optimization Protocol for All Subagents

All subagents and parent orchestrators MUST strictly adhere to token hygiene to minimize context consumption:

1. **Think-in-Code via `context-mode` (MCP)**:
   - When inspecting logs, test suites, or large datasets, use `ctx_execute`, `ctx_search`, or bounded shell pipes (`grep`, `head`, `awk`).
   - The bytes processed in the sandbox NEVER enter conversation memory; only the concise printed result enters context.
2. **Precision Slicing (`view_file`)**:
   - Never read whole source files if only specific sections or functions are needed. Always specify `StartLine` and `EndLine`.
3. **Surgical File Edits (`replace_file_content`)**:
   - Prefer targeted single-chunk replacements rather than rewriting entire files for minor changes.
4. **Targeted Intelligence Queries (`search.py`)**:
   - Query UI/UX Pro Max using specific `--domain` or `--design-system` flags. Never dump or view the raw reference CSV files into context.
5. **Zero-Fluff High-Signal Communication**:
   - Omit conversational preamble, pleasantries, re-quoting user instructions, and repeating full files in markdown responses. Deliver only direct, actionable information.

---

## 4. Multi-Stack Execution Workflows

1. **Frontend Workflow**:
   - Ideation (`ui_ux_designer`, `taste_craftsman`) -> Architecture (`frontend_architect`) -> Implementation (`frontend_developer`) -> Verification (`qa_reviewer`).
2. **Java / JVM Workflow**:
   - Inspect build toolchain & version -> Implement surgical changes (`java_pro`) -> Run targeted tests -> Review (`qa_reviewer`).
3. **Python Backend Workflow**:
   - Model schemas (`python_backend` with Pydantic v2) -> Type-safe implementation -> Write pytest cases -> Run via `ctx_execute`.
4. **Security Audit Workflow**:
   - Run threat modeling & attack tree analysis (`security_auditor`) -> Review SAST & sanitize external content inputs.

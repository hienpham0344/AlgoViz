# Workspace AI & Copilot Instructions

## 1. Context Efficiency & Token Hygiene
- For multiple independent reads or lookups, batch operations or use Context Mode (`ctx_execute`, `ctx_search`).
- Never output multi-hundred line logs or raw build terminal outputs directly into chat. Filter output to errors/failures using bounded shell tools (`grep`, `head`, `awk`).
- Inspect files with targeted line slices rather than dumping whole source files.
- Prefer surgical line-level edits over rewriting entire files for minor changes.

## 2. Engineering Standards
- **Java / JVM**: Inspect build configuration (`pom.xml`, `build.gradle`), active JDK version, and conventions before assuming modern Java features or frameworks. Add tests for changed logic.
- **Python**: Follow Pydantic v2 best practices, type annotations, and clean error handling.
- **Frontend**: Follow the existing component architecture, UI tokens, and accessible WCAG AA standards.
- **Security & Quality**: Apply STRIDE threat modeling, sanitize untrusted external content, and verify via testing before reporting tasks complete.

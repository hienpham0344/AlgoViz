# Workspace AI & Copilot Instructions

## 1. Context Efficiency & Token Hygiene
- Use Context Mode (`ctx_execute`, `ctx_search`) or bounded shell tools (`grep`, `head`, `awk`) for bulk data searches and log inspection.
- Never output multi-hundred line raw logs or massive terminal outputs directly into chat.
- Inspect files with targeted line slices (`view_file` with `StartLine`/`EndLine`).
- Prefer surgical line-level edits over rewriting entire files for minor changes.

## 2. Engineering Standards
- **Java / JVM**: Inspect build configuration (`pom.xml`, `build.gradle`), active JDK version, and conventions before assuming modern Java features or frameworks. Add tests for changed logic.
- **Python**: Follow Pydantic v2 best practices, type annotations, and clean error handling.
- **Frontend**: Follow the existing component architecture, UI tokens, and accessible WCAG AA standards. Enforce anti-slop visual taste.
- **Architecture**: Design deep modules (simple interfaces, deep implementation) and maintain domain terms via `GLOSSARY.md`.
- **Security & Quality**: Apply STRIDE threat modeling, sanitize untrusted external content, and verify via testing before reporting tasks complete.

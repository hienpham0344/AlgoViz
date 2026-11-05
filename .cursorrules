# Cursor Rules & AI Assistant Configuration

> Place this file as `.cursorrules` in your project root to align Cursor AI with Agents-kit standards.

## 1. Core Engineering Principles
- **SOLID & Clean Architecture**: Follow Single Responsibility and Dependency Inversion. Domain entities must remain independent of external frameworks.
- **YAGNI (You Aren't Gonna Need It)**: Do not write speculative, unrequested, or "future-proof" features. Solve only today's acceptance criteria.
- **KISS**: Favor clear, simple code over clever one-liners. Use guard clauses (early returns) and limit nesting to <= 3 levels.
- **TDD (Test-Driven Development)**: Always write a failing unit or integration test before implementing production logic.
- **Evidence Before Assertions**: Execute tests and verify passing output before declaring work finished.

## 2. Token Hygiene & Precision Editing
- Never dump multi-hundred line raw terminal outputs into conversation memory.
- Inspect files with targeted line slices rather than dumping whole source files.
- Prefer surgical line-level edits over rewriting entire files for minor changes.

## 3. Stack Standards
- **Python**: Enforce Pydantic v2 schemas, strict type annotations (`mypy`), and pytest suites.
- **Java / JVM**: Inspect build metadata (`pom.xml` / `build.gradle`), target Java 17+, and use deterministic unit tests.
- **TypeScript / React**: Enforce WCAG AA accessibility, functional components, and strict TypeScript types without `any`.
- **Database**: Use UUIDv7 / BIGSERIAL, explicit foreign keys, indexes on all foreign keys, and zero-downtime expand-and-contract migrations.
- **Git**: Use Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`). Never force-push to shared branches.

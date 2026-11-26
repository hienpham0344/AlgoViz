# Software Engineering Principles Cheat Sheet

A rapid checklist for code reviews and architectural audits.

---

## 1. SOLID Audit Checklist

- [ ] **S (Single Responsibility)**: Does this class/module do only one thing? If you describe what it does using the word *"and"*, split it.
- [ ] **O (Open/Closed)**: Can we add a new variation/feature without modifying existing tested code?
- [ ] **L (Liskov Substitution)**: Can every subtype replace its parent without throwing unexpected exceptions or violating base class invariants?
- [ ] **I (Interface Segregation)**: Are interfaces small and cohesive? Are any implementing classes forced to leave methods empty?
- [ ] **D (Dependency Inversion)**: Does business logic depend on abstractions (interfaces) rather than direct concrete classes (databases, third-party SDKs)?

---

## 2. Pragmatic Heuristics Checklist

- [ ] **YAGNI (You Aren't Gonna Need It)**: Are there any speculative methods, unused parameters, or unneeded abstractions built for future "what-ifs"?
- [ ] **KISS (Keep It Simple, Stupid)**: Is this code easy to understand for another engineer without explanation? Are nesting levels $\le 3$? Are guard clauses used?
- [ ] **DRY vs AHA**: Are we duplicating core domain rules? Have we respected the **Rule of Three** before extracting an abstraction?
- [ ] **Law of Demeter**: Are we chaining 3+ method calls (`a.getB().getC().getD()`)? Refactor to "Tell, Don't Ask".
- [ ] **Fail-Fast**: Are all function arguments, boundary inputs, and state invariants validated at the top of the function?
- [ ] **Boy Scout Rule**: Is the modified file cleaner, more readable, or better tested than before our edit?

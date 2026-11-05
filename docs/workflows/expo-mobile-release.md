# Enterprise Workflow: React Native / Expo Mobile App Release

## 1. Overview
End-to-end release lifecycle for cross-platform mobile apps using Expo Application Services (EAS Build and Submit) with automated versioning and store publishing.

## 2. Participating Subagents
- **`mobile_dev`**: Manages app config, Hermes optimizations, and feature code.
- **`qa_reviewer`**: Verifies manual and automated test passes on real devices and emulators.
- **`sre_specialist`**: Verifies Sentry crash-reporting source maps and release tags.

## 3. Step-by-Step Execution Sequence

### Phase 1: Pre-Release Quality Audit
1. Run static checks and unit tests:
   ```bash
   npm run lint && npm test
   ```
2. Verify app bundle identifier, version code, and release notes in `app.json`.

### Phase 2: Cloud Native Compilation (EAS)
```bash
# Build Android App Bundle (.aab) and iOS Archive (.ipa)
eas build --platform all --profile production --non-interactive
```

### Phase 3: Sentry Symbolication Upload
Upload native debug symbols (dSYM on iOS, mapping.txt on Android) to Sentry to ensure clean stack traces.

### Phase 4: Store Submission & Phased Rollout
```bash
eas submit --platform all --profile production
```
- Configure Google Play staged rollout (10% -> 25% -> 100%).
- Configure Apple Phased Release for automatic 7-day rollout.

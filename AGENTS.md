# Repository Guidelines

## Project Structure & Module Organization
- Core plugin code lives under `src/` (IntelliJ Platform modules, actions, services).
- UI resources, icons, and plugin metadata are in `resources/` (e.g., `META-INF/plugin.xml`).
- Tests live in `test/` (unit and light fixture tests). Integration-like tests may be under `intTest/` if present.
- Build outputs go to `build/` or `out/` (generated). Do not commit these.

## Build, Test, and Development Commands
- `./gradlew build` — Compiles, runs tests, and assembles artifacts.
- `./gradlew runIde` — Launches a sandbox IDE with the plugin for local testing.
- `./gradlew test` — Runs unit tests headlessly.
- `./gradlew verifyPlugin` — Verifies plugin structure and compatibility.
- `./gradlew buildPlugin` — Produces a distributable ZIP under `build/distributions/`.

## Coding Style & Naming Conventions
- Kotlin-first for new code; Java acceptable where consistent with neighbors.
- Indentation: 4 spaces (no tabs). Line length target: 120.
- Naming: classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- Package by feature (e.g., `com.company.cangjie.editor`, `...actions`, `...services`).
- Use Kotlin stdlib and JetBrains annotations (`@Nullable/@NotNull`) where appropriate.
- Formatting via `./gradlew ktlintFormat` if configured; otherwise use IntelliJ’s Default Kotlin/Java style.

## Testing Guidelines
- Frameworks: JUnit 5 with IntelliJ test fixtures (`LightPlatformCodeInsightFixtureTestCase`-style for editor behavior).
- Place tests mirroring source packages in `test/`.
- Name tests with `*Test.kt` and methods as behavior-focused (e.g., `insertsCandidate_whenTypingPhonetic()`).
- Run locally with `./gradlew test` or from IDE; prefer headless gradle in CI.
- Aim for meaningful coverage on parsing, actions, and services. Add regression tests for fixed issues.

## Commit & Pull Request Guidelines
- Commits: imperative mood, concise scope prefix when helpful, e.g., `editor: fix candidate selection on IME reset`.
- Keep changes focused; include tests or rationale when tests are impractical.
- PRs must include: summary, motivation/linked issue, screenshots or short screencast for UI changes, and test notes.
- Ensure `./gradlew build` and `verifyPlugin` pass before requesting review.

## Security & Configuration Tips
- Do not commit secrets or IDE sandbox data. Use `gradle.properties`/environment vars for tokens.
- Keep plugin `since-build/until-build` accurate in `resources/META-INF/plugin.xml`.
- Avoid reflective access to IntelliJ internals unless strictly necessary; prefer stable OpenAPI.


# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Gradle Tasks Extended" is a private IntelliJ IDEA Ultimate plugin (no Marketplace distribution) that adds
a standalone ToolWindow for browsing/running Gradle tasks, with three view modes: `Default` (original
order), `Sort` (alphabetical), and `Nested` (folder hierarchy built from slash-separated task groups, e.g.
`docker/gui/react`).

- `Plan.md` is the full original spec (German). It describes the target behavior for all three modes,
  data model, persistence rules, and acceptance criteria in detail — consult it for behavioral questions
  ("what should happen when...") before guessing.
- `docs/INTEGRATION_STRATEGY.md` documents verified platform APIs, corrections vs. `Plan.md` (notably the
  `com.intellij.gradle` vs `org.jetbrains.plugins.gradle` plugin ID mixup), root-caused bugs from manual
  test sessions, and currently known/unresolved issues (e.g. tree row icons not rendering in the sandbox).
  Check it before re-investigating something that may already be diagnosed there.
- `docs/INSTALLATION.md` covers local "Install Plugin from Disk" installation (no Marketplace signing/
  publishing flow exists in this project).

## Commands

```bash
./gradlew buildPlugin   # build the plugin ZIP -> build/distributions/gradle-tasks-extended-<version>.zip
./gradlew test          # run unit tests (JUnit 5 via kotlin-test)
./gradlew runIde        # launch a sandbox IDE with the plugin installed (also: run-ide.bat)
```

Run a single test class/method the normal Gradle way, e.g.:
```bash
./gradlew test --tests "de.example.gradletasksview.model.TaskTreeBuilderTest"
```

Toolchain: Kotlin, JVM target 21, target platform `IU` (IntelliJ IDEA Ultimate). Requires Gradle
>= 9.0 (wrapper pinned to 9.7.1) because of IntelliJ Platform Gradle Plugin 2.18.1.

### Dependency versions

**All external dependency and Gradle plugin versions live in `gradle/libs.versions.toml`** (Gradle
version catalog) — never hardcode a version in `build.gradle.kts`. This needs no extra plugin;
Gradle picks the file up by its path and exposes it as the typed `libs` accessor. Bumping the
target IDE therefore means editing `intellijIdea` in the catalog, not `gradle.properties`.

`gradle.properties` keeps only what is not a dependency: `pluginGroup`, `pluginName`,
`pluginVersion`, `pluginSinceBuild`, and `platformType` (`IU` — a product code, not a version).

## Architecture

Source root: `src/main/kotlin/de/example/gradletasksview/`

- `model/` — pure model logic, no platform dependencies: `GradleTaskDescriptor` (normalized task
  representation), `TaskTreeBuilder` (builds sorted groups / nested folder tree), `TaskTreeSorting`,
  `GradleTaskViewMode`, `NestedFolderNode`, `GradleTaskDataProvider` (reads already-imported Gradle
  project data). This package is unit-testable without the IntelliJ platform test framework.
- `ui/` — the `JTree`, cell renderer, tree model builder, speed search wiring, and
  `GradleTaskViewRefresher` (owns rebuild-on-reimport + debouncing + initial-load retry logic).
- `toolwindow/` — `ToolWindowFactory`, the panel, and the `postStartupActivity` that eagerly creates the
  refresher service so it's listening even before the ToolWindow is shown.
- `actions/` — toolbar dropdown (mode switch) and context menu actions (execute, copy name/path,
  expand/collapse all).
- `persistence/` — per-project persisted state: selected view mode, and expand/collapse state. Expand
  state uses a two-tier YAML system (see below), not just IntelliJ's `PersistentStateComponent`.
- `execution/` — `GradleTaskExecutor`, which runs tasks exclusively via
  `ExternalSystemUtil.runTask(TaskExecutionSpec)` (the current, non-deprecated API — never the
  `@ApiStatus.Obsolete` 4-parameter overload, and never a hand-rolled process/command-line invocation).
- `util/` — path/tree helpers (`GradleTaskPathUtil`, `TreePathUtil`).

### Key invariants (see `Plan.md`, "Hinweise für die umsetzende KI")

- **No Gradle sync is ever triggered by this plugin.** Task data comes only from already-imported project
  data via `ProjectDataManager.getExternalProjectsData(...)` / `DataNode<ProjectData>` traversal
  (`ExternalSystemApiUtil.findAllRecursively` with `ProjectKeys.TASK`).
- **Threading:** all reads of project data (`ProjectDataManager`, `DataNode`) go through `readAction { }`;
  all tree model mutations happen on the EDT (`Dispatchers.EDT`). Use the coroutine-based
  `readAction`/`writeAction` top-level functions, not legacy `runReadAction`.
- **`stableId`** (`externalProjectPath::gradleProjectPath::taskName`) is the only reliable identity for
  selection preservation and persistence keys — never rely on visible names alone, since task names can
  collide across subprojects/composite builds.
- **Refresh must be debounced.** `GradleTaskViewRefresher` also retries the very first refresh with
  backoff (500ms/1s/2s/4s) because a restored ToolWindow can become visible before cached project data is
  actually available, and no reliable "data ready" event exists for that case.
- No backward compatibility with older IntelliJ versions is required — use the current stable platform
  APIs (coroutines, IntelliJ Platform Gradle Plugin 2.x), not legacy equivalents.
- No Marketplace-related work (signing, product code, obfuscation) is in scope.

### Expand/collapse YAML persistence (extension beyond `Plan.md`)

Two-tier, file-based config under `<project root>/gradle/gradle_tasks_extended/`:
- `default.yaml` — team-wide default, created once if missing (`GroupExpandYaml.createStarterIfMissing`),
  never overwritten if it already exists. Meant to be checked into version control.
- `custom.yaml` — personal overrides, written automatically on manual expand/collapse; takes precedence
  over `default.yaml`. Cleared via the toolbar "Reset to Default" action.

Format is intentionally flat (no nested `groups:` wrapper): one `<group/folder path>: true|false` line per
entry, written with SnakeYAML. This applies to **all three view modes**, not just `Nested` — group paths
(`Default`/`Sort`) and folder paths (`Nested`) share the same human-readable, slash-separated key space via
`GroupHeaderNodeData.groupPath` / `FolderNodeData.folder.fullPath`.

### Plugin dependency gotcha

The base Gradle integration (`GradleConstants` etc., package `org.jetbrains.plugins.gradle.*`) currently
ships under plugin ID **`com.intellij.gradle`**, not `org.jetbrains.plugins.gradle` — that ID now belongs
only to the optional "Gradle for Java" sub-plugin. Both `build.gradle.kts` (`bundledPlugin(...)`) and
`plugin.xml` (`<depends>`) must reference `com.intellij.gradle`. See
`docs/INTEGRATION_STRATEGY.md` for the full explanation if this ever needs re-verifying against a newer
platform version.

## Testing notes

- Existing tests in `src/test/kotlin/` are pure model-logic unit tests (JUnit 5 / kotlin-test), with no
  IntelliJ Platform test framework dependency — deliberately, since `testFramework(TestFrameworkType.Platform)`
  pulls in a JUnit5 launcher listener that requires a prepared sandbox to work at all.
- Integration tests using `BasePlatformTestCase` (mocking a `DataNode` tree instead of a real Gradle sync)
  are planned but not yet implemented (see `README.md` "Offene Punkte"); if added, put them in a separate
  source set with `testFramework(Platform)` enabled so the fast unit tests stay independent of it.

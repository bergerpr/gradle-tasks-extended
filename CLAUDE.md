# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Gradle Tasks Extended" is a private IntelliJ IDEA Ultimate plugin (no Marketplace distribution) that
adds a standalone ToolWindow for browsing/running Gradle tasks, with three view modes:

- `Default` — a rebuild of the IDE's original Gradle view: module level, a `Tasks` node per module,
  alphabetically sorted groups below it, except that the groups are a **folder tree** rather than flat
  rows (REQ-077). The module level is fixed in this mode, so the toolbar toggle is greyed out there.
- `Sort` — flat group rows carrying the full group path, alphabetical.
- `Nested` — the folder hierarchy built from slash-separated task groups (e.g. `docker/gui/react`),
  without a `Tasks` node and without a forced module level.

A separate toolbar toggle adds a **module level** on top of `Sort` and `Nested` (five combinations in
total, not a fourth mode), and modules can be excluded/re-included per mode. In the folder tree, a folder
with no tasks of its own and exactly one child is compacted into one row (`docker/server/gui`) the way the
IDE's project view compacts middle packages — the config key stays the full path, so an entry for a
compacted-away intermediate node has no effect (REQ-004).

- **Knowledge base — start here for behavioral questions** ("what should happen when..."):
  `docs/requirements/` holds one small file per **implemented** requirement, `docs/decisions/` one per
  architectural decision, and `backlog/` (repo root, deliberately outside `docs/`) one per open item.
  Every file carries YAML frontmatter, so grep the frontmatter instead of reading the catalogue:
  `grep -l "kategorie: persistenz" docs/requirements/*.md`, `grep -rl "<file>.kt" docs/`,
  `grep -h "^titel:" backlog/*.md`. Field reference and further recipes: `docs/SCHEMA.md`.
  Everything under `docs/` describes the current state and is true; nothing there is aspirational.
- Verified platform APIs, root-caused bugs from manual test sessions, and every deliberate deviation from
  the original spec live in `docs/decisions/` — check there before re-investigating something that may
  already be diagnosed (`grep -rl "<file>.kt" docs/decisions/`).
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

Two maintenance-only plugins keep the catalog current (neither affects the built plugin, and
neither runs as part of `build`/`buildPlugin`):

```bash
./gradlew dependencyUpdates                                              # report only
./gradlew versionCatalogUpdate --interactive --no-configuration-cache    # propose
./gradlew versionCatalogApplyUpdates                                     # apply proposals
```

`--no-configuration-cache` is required for the `versionCatalog*` tasks — they touch
`Task.project` at execution time, which the globally enabled configuration cache rejects.
`dependencyUpdates` is unaffected. The `keep { }` block in `build.gradle.kts` must stay:
`intellijIdea` is not referenced by any `[libraries]`/`[plugins]` entry, so without it
`versionCatalogUpdate` would delete that version as unused and break configuration.
`dependencyUpdates` cannot resolve `idea:idea:<version>` (the target IDE is not a regular Maven
artifact) — that report line is expected, and the IDE version is bumped by hand.

### No deprecated plugins, libraries, or APIs

Deprecated things must not be used — pick the current, non-deprecated variant instead. This
covers Gradle plugin IDs (`io.github.ben-manes.versions`, **not** the deprecated
`com.github.ben-manes.versions`), libraries, and IntelliJ Platform APIs. Since the project
targets only the newest IDE and needs no backward compatibility, there is never a reason to
stay on an outdated API.

Beware that the Kotlin compiler stays silent on IntelliJ's `@ApiStatus.Obsolete` markers, so a
clean compile is not proof. Verify against the platform jars — that is how
`com.intellij.openapi.actionSystem.DataProvider` was found and replaced with `UiDataProvider` /
`DataSink` in `GradleTasksTree`.

Two known, unavoidable exceptions, both third-party and disclosed rather than hidden:
`ToolWindowFactory.isApplicable` / `isDoNotActivateOnStart` appear in the compiled factory only
because Kotlin auto-generates overrides for a Java interface's default methods; and
`nl.littlerobots.version-catalog-update` internally calls the deprecated `Task.project` at
execution time (re-check before moving to Gradle 10). The project's own build script produces
zero deprecation warnings under `--warning-mode all`.

## Architecture

Source root: `src/main/kotlin/de/example/gradletasksview/`

- `model/` — pure model logic, no platform dependencies: `GradleTaskDescriptor` (normalized task
  representation), `TaskTreeBuilder` (builds sorted groups / nested folder tree), `TaskTreeSorting`,
  `GradleTaskViewMode`, `NestedFolderNode`, `ModuleTreeNode` (Gradle project hierarchy), `ModuleFilter`
  (exclude/include precedence rules), `GradleTaskDataProvider` (reads already-imported Gradle
  project data — the only file here that touches the platform). Everything except the data provider is
  unit-testable without the IntelliJ platform test framework.
- `ui/` — the `JTree`, cell renderer, tree model builder, speed search wiring, and
  `GradleTaskViewRefresher` (owns rebuild-on-reimport + debouncing + initial-load retry logic).
- `toolwindow/` — `ToolWindowFactory`, the panel, and the `postStartupActivity` that eagerly creates the
  refresher service so it's listening even before the ToolWindow is shown.
- `actions/` — toolbar dropdown (mode switch) and context menu actions (execute, copy name/path,
  expand/collapse all).
- `persistence/` — per-project persisted state: selected view mode via `PersistentStateComponent`;
  everything else (expand/collapse state, icons, module grouping toggle, exclude/include lists) via the
  two-tier YAML system described below. `TaskViewConfigKey` addresses a node in that config — module
  node, module-scoped entry, or module-independent entry.
- `execution/` — `GradleTaskExecutor`, which runs tasks exclusively via
  `ExternalSystemUtil.runTask(TaskExecutionSpec)` (the current, non-deprecated API — never the
  `@ApiStatus.Obsolete` 4-parameter overload, and never a hand-rolled process/command-line invocation).
- `util/` — path/tree helpers (`GradleTaskPathUtil`, `TreePathUtil`).

### Key invariants (see `docs/decisions/`)

- **No Gradle sync is ever triggered by this plugin.** Task data comes only from already-imported project
  data via `ProjectDataManager.getExternalProjectsData(...)` / `DataNode<ProjectData>` traversal
  (`ExternalSystemApiUtil.findAllRecursively` with `ProjectKeys.TASK`).
- **Threading:** all reads of project data (`ProjectDataManager`, `DataNode`) go through `readAction { }`;
  all tree model mutations happen on the EDT (`Dispatchers.EDT`). Use the coroutine-based
  `readAction`/`writeAction` top-level functions, not legacy `runReadAction`.
- **`stableId`** (`externalProjectPath::gradleProjectPath::taskName`) is the only reliable identity for
  selection preservation and persistence keys — never rely on visible names alone, since task names can
  collide across subprojects/composite builds.
- **Module identity comes from the parent node**, not from `TaskData`: `taskNode.getParent(ModuleData::class.java)`
  plus the `ModuleData.gradleIdentityPathOrNull` / `gradlePathOrNull` extensions in
  `org.jetbrains.plugins.gradle.util`. `ModuleData.getId()` is *not* the Gradle project path, and the
  non-nullable `gradlePath`/`gradleIdentityPath` accessors on `GradleModuleData` are deprecated. Tasks
  are executed with the **qualified** path (`:app:buildImage`) — a bare name runs the task in every
  matching project. See `docs/decisions/ADR-015-*.md`.
- **Refresh must be debounced.** `GradleTaskViewRefresher` also retries the very first refresh with
  backoff (500ms/1s/2s/4s) because a restored ToolWindow can become visible before cached project data is
  actually available, and no reliable "data ready" event exists for that case.
- No backward compatibility with older IntelliJ versions is required — use the current stable platform
  APIs (coroutines, IntelliJ Platform Gradle Plugin 2.x), not legacy equivalents.
- No Marketplace-related work (signing, product code, obfuscation) is in scope.

### YAML view config (REQ-070, REQ-071, REQ-076, ADR-003, ADR-014)

Two-tier, file-based config under `<project root>/gradle/gradle_tasks_extended/`, covering the
expand/collapse state, per-node custom icons, and the view settings (module grouping + exclude/include):
- `default.yaml` — team-wide default, created once if missing (`TaskViewConfigYaml.createStarterIfMissing`),
  never overwritten if it already exists. Meant to be checked into version control.
- `custom.yaml` — personal overrides, written automatically on manual expand/collapse and by the module
  toggle; takes precedence over `default.yaml`. Cleared via the toolbar "Reset to Default" action, which
  therefore also resets the module toggle and personal exclude/include lists.
- `icons/` — custom icon files that the YAML entries reference by file name.

Three kinds of key sit side by side at the top level, and node entries stay flat (no `groups:` wrapper)
so existing files keep working unchanged — the plugin never rewrites `default.yaml`, so migration is not
an option. A node value is either a bare boolean (short form, expand state only) or a mapping with the
optional keys `expanded` and `icon`:

```yaml
gradle_tasks_extended:            # reserved key: view settings
  showModules: true
  exclude:
    - ":legacy"                   # hides the module and its submodules
  include:
    ":legacy":                    # ... except these group paths
      - docker/gui
  modes:
    sort:
      exclude:
        - ":sandbox"              # only in Sort mode

docker/gui: false                 # short form, unchanged — applies to every module
docker/gui/jsp:
  icon: smile.svg                 # replaces the built-in folder icon
printGroup-docker-server-gui:
  icon: smile.svg                 # tasks are keyed by task name

":app":                           # leading colon = module block; root project is ":"
  expanded: false                 #   reserved: the module node itself
  icon: app.svg
  docker/gui/jsp:                 #   anything else: a group path only inside :app
    expanded: true
```

Expand state and icons apply to **all three view modes**, not just `Nested` — group paths (`Sort`) and
folder paths (`Default`/`Nested`) share the same human-readable, slash-separated key space
via `GroupHeaderNodeData.groupPath` / `FolderNodeData.folder.fullPath`. The `Tasks` node of the `Default`
mode is the one exception: it is pure structure, has no key at all (`configKeyOf` returns `null`), and
follows its module's expand state (REQ-077). Task entries share that key space
but are keyed by `taskName`, module nodes by their Gradle project path; no ambiguity arises because each
node type only ever looks up its own key, and module paths are recognisable by the leading `:`.
Exclude/include are the exception: they are explicitly **per mode** (this amends ADR-003).

Three format rules are worth remembering (full reasoning in ADR-014): the reserved key is named after the
plugin so it cannot collide with a real Gradle group; `:` distinguishes module from group because Gradle
project paths always start with one; and module keys stay **absolute** (`":app:core"` is a top-level key,
not nested inside `":app"`) even though the *display* is nested — otherwise a sub-key inside a module
block could not be told apart from a group path.

Resolution per node, most specific first: module-scoped `custom.yaml` > module-scoped `default.yaml` >
module-independent `custom.yaml` > module-independent `default.yaml` > built-in default. The
exclude/include **lists** follow the opposite idea — they are unioned rather than overridden, so a team
default can never silently disappear; precedence between conflicting rules is specificity, then
mode-specific, then include (ADR-016).

Icon rules: the value is a file name relative to `gradle/gradle_tasks_extended/icons/`, loaded via
`IconLoader.findIcon(URL)`. A sibling `<name>_dark.<ext>` is preferred automatically under dark themes
(`JBColor.isBright()`) — unlike bundled classpath icons, file icons get no automatic variant lookup, so
the store does it. Paths escaping the icons directory are rejected, and a missing file logs a warning and
falls back to the built-in icon rather than rendering nothing. `GradleTasksViewConfigStore.reloadIfChanged()`
re-reads both files on refresh when their timestamps changed, so hand edits apply without an IDE restart.
`setExpanded` preserves an entry's `icon`, and `write` keeps icon-less entries (and group-less module
blocks) in the short boolean form.

### Plugin dependency gotcha

The base Gradle integration (`GradleConstants` etc., package `org.jetbrains.plugins.gradle.*`) currently
ships under plugin ID **`com.intellij.gradle`**, not `org.jetbrains.plugins.gradle` — that ID now belongs
only to the optional "Gradle for Java" sub-plugin. Both `build.gradle.kts` (`bundledPlugin(...)`) and
`plugin.xml` (`<depends>`) must reference `com.intellij.gradle`. See
`docs/decisions/ADR-002-*.md` for the full explanation if this ever needs re-verifying against a newer
platform version.

## Testing notes

### Manual sandbox runs (`./gradlew runIde`)

The multi-project fixture for manual testing lives **outside this repository**, at
`../gradle-tasks-extended-testproject` (see its `README.md`). It has a root project with its own
tasks, a nested subproject `:app:core`, deliberately colliding task names across modules, and
slash-separated group paths that overlap between modules. Open it once via File > Open in the
sandbox IDE — the sandbox remembers opened projects. Keeping it out of this repo keeps test
fixtures distinguishable from this project's own configuration, which matters once documentation
is generated from `docs/`.


The project-local **personal** config may be reset or deleted freely while testing — no need to back it
up and restore it around a sandbox run; just mention the change in the result. That covers
`.idea/gradleTasksView.xml` (the persisted view mode, which has to be changed anyway to verify all three
modes) and `gradle/gradle_tasks_extended/custom.yaml` (personal expand/collapse overrides, rewritten on
the next manual expand). Backing them up is not even reliable: a running sandbox IDE rewrites or removes
`.idea/gradleTasksView.xml` on exit. Excluded is `gradle/gradle_tasks_extended/default.yaml` — the
team-wide, version-controlled default the plugin never overwrites.

A still-running sandbox IDE keeps `snakeyaml-2.6.jar` locked inside the sandbox `plugins/` directory,
which makes `prepareSandbox` — and with it `buildPlugin` and `runIde` — fail with a copy error. Kill the
previous sandbox process before rebuilding.

- Existing tests in `src/test/kotlin/` are pure model-logic unit tests (JUnit 5 / kotlin-test), with no
  IntelliJ Platform test framework dependency — deliberately, since `testFramework(TestFrameworkType.Platform)`
  pulls in a JUnit5 launcher listener that requires a prepared sandbox to work at all.
- Integration tests using `BasePlatformTestCase` (mocking a `DataNode` tree instead of a real Gradle sync)
  are planned but not yet implemented (see `README.md` "Offene Punkte"); if added, put them in a separate
  source set with `testFramework(Platform)` enabled so the fast unit tests stay independent of it.

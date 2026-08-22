# Gradle Tasks Extended

IntelliJ IDEA Ultimate Plugin, das die Gradle-Task-Ansicht um eine eigene
ToolWindow-Ansicht mit drei Darstellungsmodi erweitert (`Default`, `Sort`, `Nested`).
Siehe [Plan.md](Plan.md) fuer die vollstaendige Spezifikation und
[docs/INTEGRATION_STRATEGY.md](docs/INTEGRATION_STRATEGY.md) fuer die verifizierten
Plattform-APIs.

Rein privates Projekt, kein Marketplace-Vertrieb (siehe [docs/INSTALLATION.md](docs/INSTALLATION.md)).

## Build

```bash
./gradlew buildPlugin
```

## Tests

```bash
./gradlew test
```

## In einer Sandbox-IDE ausprobieren

```bash
./gradlew runIde
```

## Offene Punkte / Naechste Schritte

- **Integrationstests mit `BasePlatformTestCase`** (Plan-Implementierungsschritt 16,
  siehe [Plan.md:863](Plan.md) und Testkonzept in [Plan.md:753](Plan.md)): Mock-`DataNode`-Baum
  statt echtem Gradle-Sync, um ToolWindow, Moduswechsel, Kontextmenue und Filterung
  gegen die reale Platform-API abzudecken. Noch nicht umgesetzt.
- **`gradleProjectPath`-Verfeinerung fuer Multi-Modul-/Composite-Build-Projekte**
  (siehe [docs/INTEGRATION_STRATEGY.md](docs/INTEGRATION_STRATEGY.md), Abschnitt
  "Bekannte Risiken"): aktuell nur ein Platzhalter (identisch zu `externalProjectPath`),
  muss gegen ein echtes Multi-Modul-Testprojekt verifiziert werden, damit `stableId`
  bei identischen Tasknamen in unterschiedlichen Subprojekten zuverlaessig eindeutig ist.
- **Tree-Row-Icons werden in der Sandbox nicht sichtbar gezeichnet** (siehe
  [docs/INTEGRATION_STRATEGY.md](docs/INTEGRATION_STRATEGY.md), Abschnitt "Bekannte
  Risiken"): ausfuehrlich debuggt und mehrere Ursachen ausgeschlossen (siehe dort),
  aber ungeloest. Naechster Schritt waere ein angehaengter Java-Debugger mit Breakpoint
  in `SimpleColoredComponent.doPaint`.

## Projektstruktur

```text
src/main/kotlin/de/example/gradletasksview/
  toolwindow/   ToolWindowFactory, Panel, Startup-Activity
  actions/      Toolbar-Dropdown, Kontextmenue-Aktionen
  model/        Reine Modelllogik (Sortierung, Nested-Baum, stableId)
  persistence/  Projektbezogene Persistenz (Modus, Expand-States)
  ui/           JTree, Renderer, SpeedSearch, Refresher
  execution/    Task-Ausfuehrung ueber ExternalSystemUtil
```

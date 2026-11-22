# Gradle Tasks Extended

IntelliJ IDEA Ultimate Plugin, das die Gradle-Task-Ansicht um eine eigene
ToolWindow-Ansicht mit drei Darstellungsmodi erweitert: `Default` (Nachbau der originalen
Gradle-Ansicht, aber mit Ordnerbaum statt flacher Gruppenzeilen), `Sort` (flach, alphabetisch)
und `Nested` (Ordnerbaum). `Sort` und `Nested` sind zusaetzlich mit einer zuschaltbaren
Modulgliederung kombinierbar; in `Default` ist sie fest eingebaut.
Spezifikation, verifizierte Plattform-APIs und Entscheidungen sind in eine durchsuchbare
Wissensbasis aufgeloest (siehe unten).

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

## Wissensbasis

- `docs/requirements/` - je eine kleine Datei pro **umgesetzter** Anforderung
- `docs/decisions/` - je eine Datei pro Architekturentscheidung, inklusive bewusster
  Abweichungen von der urspruenglichen Spezifikation
- `backlog/` - offene Punkte, bewusst ausserhalb von `docs/`
- `docs/SCHEMA.md` - Feldschema und Suchrezepte

Offene Punkte auflisten:

```bash
grep -h "^titel:" backlog/*.md
```

## Projektstruktur

```text
src/main/kotlin/de/berger/gradletasksview/
  toolwindow/   ToolWindowFactory, Panel, Startup-Activity
  actions/      Toolbar-Dropdown, Feststellbuttons (Module, geerbte Tasks, Ausgeblendete),
                Kontextmenue-Aktionen
  model/        Reine Modelllogik (Sortierung, Nested-Baum, Modulbaum, Filter, stableId)
  persistence/  Projektbezogene Persistenz (Modus, Expand-States, Icons, Ansichtseinstellungen)
  ui/           JTree, Renderer, SpeedSearch, Refresher
  execution/    Task-Ausfuehrung ueber ExternalSystemUtil
```

## Lizenz

[Apache License 2.0](LICENSE), Hinweise in [NOTICE](NOTICE). Der Code ist weitgehend mit
Claude Code (Anthropic) entstanden; die `Co-Authored-By`-Zeilen der Commits zeigen, wo.
Das Elefanten-Logo ist mit dem Bildmodell von OpenAI (`gpt-image`) erzeugt.
Das Plugin-Jar traegt `LICENSE`, `NOTICE` und `THIRD-PARTY-NOTICES.txt` (SnakeYAML, ebenfalls
Apache 2.0) unter `META-INF/` - siehe ADR-027.

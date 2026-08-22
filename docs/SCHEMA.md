# Wissensbasis: Schema und Abfragen

Die Projektwahrheit liegt in kleinen Einzeldateien mit YAML-Frontmatter, damit sie gezielt
durchsuchbar sind und spaeter maschinell zu Dokumentation verarbeitet werden koennen.

## Wo was liegt

| Ort | Inhalt | Regel |
|---|---|---|
| `docs/requirements/` | Anforderungen, die **umgesetzt** sind | Ist-Zustand. Was hier steht, ist wahr. |
| `docs/decisions/` | Getroffene Entscheidungen mit Begruendung | Auch bewusste Abweichungen von Vorgaben. |
| `backlog/` | Was **noch nicht** existiert | Bewusst ausserhalb von `docs/`. |

Die Trennung ist der Grund, warum ein Generator spaeter **nicht nach Status filtern muss**:
Alles unterhalb von `docs/` darf ungeprueft in eine Dokumentation uebernommen werden.

## Frontmatter

Anforderung (`docs/requirements/REQ-###-<slug>.md`):

```yaml
id: REQ-013                 # stabil, wird nie neu vergeben
titel: Filterung ueber Speed-Search
kategorie: modi | darstellung | interaktion | daten | persistenz | plattform | test | vertrieb
verifiziert: unit | visuell | manuell | konstruktiv
betrifft:                   # Quelldateien, die diese Anforderung tragen
  - src/main/kotlin/.../GradleTasksSpeedSearch.kt
offene_punkte:              # optional: bekannte Luecke im Backlog
  - OPEN-003
quelle: Plan.md, Abschnitt "Filterung"
```

Entscheidung (`docs/decisions/ADR-###-<slug>.md`): `id`, `titel`, `betrifft`, optional
`ersetzt_vorgabe`, `quelle`.

Offener Punkt (`backlog/OPEN-###-<slug>.md`): `id`, `titel`, `prioritaet` (hoch/mittel/
niedrig), optional `betrifft_anforderung`, `betrifft`, `quelle`.

### Herkunftsangabe `quelle: Plan.md ...`

`Plan.md` war die urspruengliche, in einem Stueck geschriebene Spezifikation. Sie wurde in
diesen Katalog aufgeloest und danach entfernt; die Datei existiert weiterhin in der
Git-Historie. Ein `quelle: Plan.md, Abschnitt "..."` ist also eine Herkunftsangabe, kein
Verweis auf eine noch vorhandene Datei.

Dasselbe gilt fuer `quelle: OPEN-###, ...`: Wird ein Backlog-Eintrag umgesetzt, wandert seine
Aussage nach `docs/` und die Datei verschwindet aus `backlog/`. Die Herkunftsangabe bleibt als
Hinweis darauf stehen, woher die Festlegung stammt, und ist ebenfalls in der Git-Historie
nachlesbar.

Nicht uebernommen wurde aus `Plan.md` alles Prozesshafte und Ueberholte: Rechercheauftrag,
Implementierungsreihenfolge und die Code-Geruestentwuerfe. Letztere waren teils schlicht
falsch geworden - etwa die Plugin-ID `org.jetbrains.plugins.gradle` (siehe ADR-002), die
inzwischen obsolete `runTask`-Ueberladung (ADR-006) und der Persistenzentwurf mit
`expandedFolderPaths`/`knownFolderPaths`, der so nie gebaut wurde (ADR-003). Sie
mitzunehmen haette bekannt Falsches in den Katalog getragen.

`verifiziert` sagt, **wie** der Nachweis erbracht wurde - `konstruktiv` heisst: folgt aus dem
Aufbau des Codes, ohne eigenen Test.

## Abfragen

```bash
grep -l "kategorie: persistenz" docs/requirements/*.md      # alles zu einem Thema
grep -rl "GradleTasksTree.kt" docs/                          # was haengt an dieser Datei?
grep -h "^titel:" backlog/*.md                               # offene Punkte auflisten
grep -l "verifiziert: konstruktiv" docs/requirements/*.md    # ungetestete Zusagen
grep -rn "^ersetzt_vorgabe:" docs/decisions/                 # bewusste Abweichungen
```

Das Frontmatter **ist** der Index. Es gibt bewusst keine Indexdatei, die Inhalte dupliziert -
sie waere nach kurzer Zeit falsch und muesste bei jedem Blick mitgelesen werden.

## Regeln beim Pflegen

- Eine Anforderung pro Datei. Was nicht in ~30 Zeilen passt, sind zwei Anforderungen.
- IDs werden nie wiederverwendet, auch nicht nach dem Entfernen eines Eintrags.
- `betrifft` beim Umbenennen von Quelldateien mitziehen - sonst bricht die Rueckrichtung
  "welche Anforderung haengt an dieser Datei?".
- Wird ein Backlog-Eintrag erledigt, wandert die Aussage nach `docs/` und der Eintrag
  verschwindet aus `backlog/`; das `offene_punkte`-Feld der Anforderung entfaellt dabei.
- Nichts hiervon gehoert nach `CLAUDE.md`. Dort steht nur der Zeiger auf dieses Verzeichnis,
  damit der Kontext nicht mit dem Katalog belastet wird.

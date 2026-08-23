---
id: OPEN-008
titel: Einzelne Knoten ueber das Kontextmenue ausblenden
prioritaet: mittel
betrifft_anforderung:
  - REQ-011
  - REQ-070
  - REQ-075
  - REQ-076
betrifft:
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTasksTreeContextActions.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
quelle: Benutzerwunsch aus der Testsitzung zu OPEN-004
---

Setzt **OPEN-009 voraus**: `hidden` wird gleich im neuen Format gebaut, statt es zweimal zu
implementieren. Die Beispiele unten stehen deshalb in der Schreibweise von OPEN-009.

Ein Rechtsklick auf einen Knoten bietet **Hide** an. Der Knoten verschwindet aus der Ansicht,
und zwar **nur an dieser Stelle**: Derselbe Gruppenpfad oder Taskname in einem anderen Modul
bleibt sichtbar. Der Ausschluss wird in `custom.yaml` festgehalten und ueberlebt damit einen
IDE-Neustart.

## Format

Kein neuer Schluesselraum, sondern ein drittes Feld neben `expanded` und `icon`:

```yaml
modules:
  ":app":
    groups:
      docker/gui:
        hidden: true        # nur in :app; :lib und das Wurzelprojekt behalten die Gruppe
    tasks:
      buildImage:
        hidden: true        # nur der Task in :app
```

Die Ortsgenauigkeit ergibt sich aus dem vorhandenen Schluesselraum (REQ-076): Ist die
Modulebene an, adressiert der angeklickte Knoten einen Modulblock; ist sie aus, gibt es keinen
Modulbezug, der Schluessel landet auf oberster Ebene und blendet den Knoten in allen Modulen
aus. Das ist keine Einschraenkung, sondern die einzige sinnvolle Lesart: In dieser Ansicht
*ist* die Zeile die Zusammenfassung aller Module.

Wie `expanded` und `icon` gilt `hidden` fuer alle drei Modi, sofern kein `modes`-Eintrag
etwas anderes sagt (OPEN-009).

## Wirkung

Gefiltert wird **vor** dem Baumaufbau, wie exclude/include (REQ-075): Ein ausgeblendeter
Ordner nimmt seinen Teilbaum mit, und es bleiben keine leeren Gruppen- oder Ordnerknoten
zurueck. Die Filterung greift damit auch vor der Textsuche.

Der `Tasks`-Knoten des `Default`-Modus hat bewusst keinen Konfigurationsschluessel (REQ-077,
`configKeyOf` liefert `null`). Auf ihm ist der Menueeintrag deaktiviert.

## Modulknoten

Auf einem Modulknoten schreibt **Hide** `hidden: true` in den Modulblock. Die Entscheidung
dahinter - ein Modul nicht auf zwei Wegen ausblendbar zu machen - ist dieselbe wie zuvor, nur
faellt die Doppelung jetzt auf der anderen Seite weg: Mit OPEN-009 geht die `exclude`-Liste
selbst in `hidden` auf, es gibt also nur noch diesen einen Weg. Die Ausnahme, die frueher
`include` war, ist ein `hidden: false` auf dem spezifischeren Pfad.

## Zurueckholen

Ausgeblendete Knoten sind unsichtbar, es braucht also einen Weg zurueck, der nicht
`Reset to Default` ist - das wuerde Aufklappzustand und Modultoggle mitloeschen.

Vorgesehen ist ein Feststellbutton in der Toolbar, **Ausgeblendete anzeigen**: Er zeigt die
versteckten Knoten wieder an, ausgegraut kenntlich gemacht, und auf ihnen heisst der
Kontextmenueeintrag dann **Wieder einblenden** (entfernt den Eintrag aus `custom.yaml`). Der
Zustand des Buttons ist reine Ansichtssache und gehoert nicht in die Konfiguration.

## Mehrfachselektion

Hide wirkt auf **alle** selektierten Knoten und schreibt einen Eintrag je Knoten - wie die
uebrigen Kontextmenue-Aktionen, die ebenfalls die ganze Selektion lesen (REQ-011). Der
Menueeintrag ist aktiv, sobald mindestens ein selektierter Knoten einen
Konfigurationsschluessel hat; Knoten ohne Schluessel werden uebersprungen statt die Aktion zu
sperren.

## `hidden: false` als Gegenmittel

`hidden` loest wie `expanded` und `icon` nach Spezifitaet auf: Ein modulbezogener Eintrag
schlaegt den modulunabhaengigen, `custom.yaml` schlaegt `default.yaml`. Damit laesst sich eine
Team-Vorgabe fuer einzelne Module zuruecknehmen, ohne `default.yaml` anzufassen:

```yaml
# default.yaml: ueberall ausgeblendet
groups:
  docker/gui:
    hidden: true

# custom.yaml: ausser in :app
modules:
  ":app":
    groups:
      docker/gui:
        hidden: false
```

Das ist bewusst **anders** als bei exclude/include (REQ-075, ADR-016): Dort werden Listen
vereinigt, damit eine Team-Vorgabe nicht unbemerkt verschwindet. Hier geht es nicht um Listen,
sondern um denselben Knotenschluessel auf zwei Ebenen - und dafuer gilt im ganzen Format
"spezifischer gewinnt".

Beim Umsetzen zu beachten: `hidden` braucht deshalb `Boolean?` mit `null` = "nicht
konfiguriert", nicht `Boolean` mit Vorgabe `false` - sonst laesst sich "nicht gesetzt" nicht
von "ausdruecklich sichtbar" unterscheiden, und die Kurzform wuerde beim Schreiben verloren
gehen.

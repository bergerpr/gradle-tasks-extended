# Installation (ohne Marketplace)

Dieses Plugin wird **nicht** ueber den JetBrains Marketplace vertrieben. Fertige Fassungen
liegen als GitHub-Releases bereit und kommen ueber ein eigenes Plugin-Repository in die IDE
(REQ-051, ADR-028); lokale Staende lassen sich weiterhin per "Install Plugin from Disk"
installieren.

## 0. Repository eintragen (empfohlen)

1. **Settings > Plugins**, Zahnrad oben > **Manage Plugin Repositories...**
2. Mit **+** die Adresse eintragen:
   `https://github.com/<owner>/<repo>/releases/latest/download/updatePlugins.xml`
3. Im Reiter **Marketplace** nach "Gradle Tasks Extended" suchen und installieren.

Danach bietet die IDE neue Fassungen an wie bei jedem anderen Plugin. War das Plugin bisher
per Datei installiert, fragt eine neuere IDE "Cannot update plugins - ... Choose where to
get updates from": dort die eingetragene Quelle waehlen, oder das Plugin einmal
deinstallieren und aus dem Repository neu installieren.

## Release anlegen

`pluginVersion` in `gradle.properties` anheben, committen, Tag `v<pluginVersion>` setzen und
pushen. Der Workflow `.github/workflows/release.yml` baut, testet und verifiziert und legt das
Release mit ZIP und `updatePlugins.xml` an. Ein Tag, der nicht zu `pluginVersion` passt,
bricht ab.

## 1. Build

```bash
./gradlew clean buildPlugin
```

Erzeugt eine ZIP-Datei unter `build/distributions/gradle-tasks-extended-<version>.zip`.
Sie enthaelt alles, was das Plugin braucht: das eigene Jar und SnakeYAML.

Eine **laufende Sandbox-IDE muss vorher beendet werden** - sie haelt
`snakeyaml-2.7.jar` im Sandbox-Verzeichnis gesperrt, woran `prepareSandbox` und damit
`buildPlugin` scheitert.

## 1a. Vor einem Release

Der Info-Dialog des Plugins zeigt Version, Git-Commit und Branch an und markiert einen
Build aus einem Arbeitsverzeichnis mit nicht eingecheckten Aenderungen (REQ-080). Fuer
eine Fassung, die weitergegeben wird, lohnt sich deshalb diese Reihenfolge:

1. `pluginVersion` in `gradle.properties` anheben - die Nummer landet im Dateinamen der
   ZIP, in der `plugin.xml` und im Info-Dialog.
2. Alles committen (sonst steht im Dialog "with uncommitted changes"), optional taggen.
3. `./gradlew clean test buildPlugin verifyPluginStructure verifyPluginProjectConfiguration`

Zusaetzlich laesst sich der IntelliJ Plugin Verifier ueber die fertige ZIP laufen:

```bash
./gradlew verifyPlugin
```

Er prueft gegen die Plattform, gegen die auch uebersetzt wird (`ides { current() }`), und
braucht dafuer rund zwei Minuten. Gegen **welche** IDEs geprueft wird, leitet er nicht aus
der `plugin.xml` ab - das steht im `pluginVerification`-Block von `build.gradle.kts`. Die
Kompatibilitaetsangabe selbst ist davon unabhaengig und steht als `since-build` in der
`plugin.xml` (aus `pluginSinceBuild`); ein `until-build` gibt es bewusst nicht (REQ-050).

`signPlugin` und `publishPlugin` sind dagegen bewusst nicht eingerichtet: Beides gehoert zum
Marketplace-Vertrieb, den es hier nicht gibt (REQ-051). Die `updatePlugins.xml` fuer das eigene
Repository erzeugt `./gradlew generateUpdatePluginsXml -PpluginRepositoryUrl=https://github.com/<owner>/<repo>`
neben die ZIP; im Release-Workflow geschieht das automatisch.

## 2. Installation in IntelliJ IDEA Ultimate

1. **Settings/Preferences → Plugins**.
2. Auf das Zahnrad-Icon oben rechts klicken → **Install Plugin from Disk...**.
3. Die zuvor erzeugte ZIP-Datei auswaehlen.
4. IDE neu starten.

> Hinweis: Die Datei-Erweiterung muss kleingeschrieben `.zip` sein, sonst bleibt
> der OK-Button in manchen IDE-Versionen deaktiviert.

## 3. Update

Bei einer neuen Plugin-Version:

1. Alte Version zunaechst deinstallieren, **oder** direkt erneut ueber
   "Install Plugin from Disk" installieren (ueberschreibt die alte Version).
2. IDE neu starten.

Eine Fassung mit der frueheren Plugin-ID `de.example.gradletasksview` wird dabei **nicht**
ersetzt: Seit der Umbenennung des Pakets lautet die ID `de.berger.gradletasksview`, und fuer
die IDE sind das zwei verschiedene Plugins. Die alte muss einmal von Hand deinstalliert
werden, sonst laufen beide nebeneinander.

## Voraussetzungen

- IntelliJ IDEA Ultimate, aktuellste unterstuetzte Version (siehe
  `gradle/libs.versions.toml`, Eintrag `intellijIdea` unter `[versions]`).
- Die gebuendelte Gradle-Integration muss aktiviert sein (Standard in IntelliJ IDEA
  Ultimate). Ihre Plugin-ID lautet `com.intellij.gradle`; `org.jetbrains.plugins.gradle`
  bezeichnet inzwischen nur noch das optionale "Gradle for Java" (siehe ADR-002).

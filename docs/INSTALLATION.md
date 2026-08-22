# Installation (privat, ohne Marketplace)

Dieses Plugin wird **nicht** ueber den JetBrains Marketplace vertrieben, sondern
ausschliesslich lokal per "Install Plugin from Disk" installiert.

## 1. Build

```bash
./gradlew buildPlugin
```

Erzeugt eine ZIP-Datei unter `build/distributions/gradle-tasks-extended-<version>.zip`.

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

## Voraussetzungen

- IntelliJ IDEA Ultimate, aktuellste unterstuetzte Version (siehe
  `gradle/libs.versions.toml`, Eintrag `intellijIdea` unter `[versions]`).
- Das gebuendelte Gradle-Plugin (`org.jetbrains.plugins.gradle`) muss aktiviert sein
  (Standard in IntelliJ IDEA Ultimate).

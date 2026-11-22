---
id: ADR-027
titel: Das Projekt steht unter der Apache License 2.0
betrifft:
  - LICENSE
  - NOTICE
  - src/main/legal/THIRD-PARTY-NOTICES.txt
  - build.gradle.kts
  - elefant.png
quelle: OPEN-011, Abschnitt "Lizenz" (Entscheidung des Benutzers, 2026-11-22)
---

Code, Dokumentation und Bilder des Repositories stehen unter der **Apache License 2.0**.
`LICENSE` im Wurzelverzeichnis ist der unveraenderte Text von apache.org (SHA-256
`cfc7749b...d30`), `NOTICE` nennt Projekt und Copyright-Inhaber.

Warum Apache 2.0: Plattform, IntelliJ Platform Gradle Plugin, die Plugin-Vorlage von JetBrains
und das einzige mitgelieferte Jar (SnakeYAML) stehen unter derselben Lizenz - keine
Vertraeglichkeitsfrage, und ein Lizenztext deckt alles ab. Gegenueber MIT kommen eine
ausdrueckliche Patentlizenz und die Regel fuer Beitraege (Abschnitt 5) hinzu. Ein Copyleft
waere wenig wert, weil weitgehend KI-erzeugter Code voraussichtlich nur in den menschlichen
Anteilen urheberrechtlich geschuetzt ist; den Haftungsausschluss braucht es trotzdem.

**Im Plugin-Jar:** Apache 2.0 verlangt bei Weitergabe eine Kopie des Lizenztexts, und das
SnakeYAML-Jar enthaelt keine. Die Gradle-Aufgabe `generateLegalFiles` legt deshalb `LICENSE`,
`NOTICE` und `THIRD-PARTY-NOTICES.txt` unter `META-INF/` des Plugin-Jars ab. Die letzte Datei
entsteht aus `src/main/legal/` mit der SnakeYAML-Version aus dem Version Catalog, damit sie
nach einem Update nicht veraltet. SnakeYAML fuehrt keine eigene NOTICE-Datei (Repository auf
Codeberg und Jar am 2026-11-22 nachgesehen), es gibt also nichts weiterzureichen.

**Das Logo** (`elefant.png`, `images/elefant*.png`, `icons/toolWindowElefant*.png`) ist mit
OpenAI `gpt-image` erzeugt - belegt durch das C2PA-Zertifikat im `caBX`-Block der Datei. Die
Nutzungsbedingungen von OpenAI uebertragen die Rechte am Output dem Nutzer und verbieten nur,
ihn als menschengemacht auszugeben; deshalb steht die Herkunft in `NOTICE`. Das Bild faellt
unter dieselbe Lizenz wie der Rest.

Kein Lizenzhinweis in `plugin.xml` oder der Overview: Die Lizenz betrifft die Weitergabe,
nicht die Benutzung in der IDE.

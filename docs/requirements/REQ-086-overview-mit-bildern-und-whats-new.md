---
id: REQ-086
titel: Overview mit Bildern und What's New in der Plugin-Liste
kategorie: vertrieb
verifiziert: manuell
betrifft:
  - build.gradle.kts
  - src/main/plugin-meta/description.html
  - src/main/plugin-meta/change-notes.html
  - src/main/resources/META-INF/plugin.xml
  - gradle.properties
quelle: Wunsch des Benutzers
---

In `Einstellungen > Plugins > Installed > Gradle Tasks Extended` zeigt der Reiter **Overview**
zuerst den Beschreibungstext und darunter die Bildstrecke; der Reiter **What's New** listet die
Aenderungen je Version, neueste zuerst.

Die Bilder standen zunaechst oben. Vier Bilder zu je 360 px sind aber hoeher als der Reiter,
sodass man an ihnen vorbeiscrollen musste, bevor ueberhaupt ein Satz zu lesen war - der Text
gehoert dorthin, wo der Blick zuerst landet.

Beide Texte stehen als Dateien in `src/main/plugin-meta/` und werden von `patchPluginXml` in
die `plugin.xml` eingetragen - die haelt sie nicht mehr selbst. Der Grund ist die Bildstrecke:
Ihre Dateipfade stehen erst beim Bauen fest.

## Warum die Bilder an absoluten `file:`-URLs haengen

Das ist nicht die schoene Loesung, sondern die einzige. Drei Wege wurden gegen die Plattform
geprueft (IU-2026.2):

- **Die eingebaute Bildergalerie des Reiters** speist sich aus `PluginUiModel.getScreenShots()`,
  und das liefert nur fuer einen `PluginNode` etwas - also fuer Plugins, deren Daten vom
  Marketplace stammen. Ein lokal installiertes Plugin hat keinen.
- **Relative Pfade** koennen nicht aufgeloest werden: Die Beschreibung zeichnet ein
  gewoehnliches Swing-`JEditorPane` (`PluginDetailsPageComponent.createDescriptionComponent`),
  das keine Basis-URL setzt. Ohne Basis bleibt jeder relative `src` ohne Ziel.
- **`data:`-URIs** kennt die JVM nicht; es gibt keinen Handler dafuer.

Bleiben absolute URLs, die die JVM oeffnen kann. `https://` scheidet ohne Marketplace aus, also
`file:` - erzeugt beim Bauen aus `src/main/resources/help/screenshots/` samt Bildunterschriften
aus `screenshots.txt`, denselben, die auch der Info-Dialog verwendet (REQ-081).

**Preis:** Die Pfade zeigen auf die Arbeitskopie, aus der gebaut wurde. Wird sie verschoben oder
das ZIP auf einem anderen Rechner installiert, bleiben im Reiter leere Bildrahmen; der Text
darunter ist davon unberuehrt. Fuer ein privates Ein-Rechner-Plugin ohne Marketplace-Vertrieb
(REQ-051) ist das vertretbar - die Alternative waere, gar keine Bilder zu zeigen.

Die Bilder werden mit `width="360"` eingebunden. Die Vorlagen sind 773 px breit, und das
`JEditorPane` skaliert nicht von sich aus; ohne Breitenangabe schoebe jedes Bild den Reiter in
einen waagerechten Bildlauf.

## Kein Karussell

Das Bilderkarussell, das andere Plugins an dieser Stelle zeigen (etwa Junie), ist nicht
nachbaubar. Zustaendig dafuer ist `PluginImagesComponent`, und gefuellt wird es ueber
`PluginNode.setScreenShots(...)` - dessen einzige Aufrufer sind `IntellijPluginMetadata` und
`PluginDto`, also die **Marketplace-Metadaten** zu einer `xmlId`. Die Bilder liest die
Komponente anschliessend aus `<system>/plugins/imageCache/<externalPluginId>/`, dem Cache des
zuvor Heruntergeladenen.

Fuer ein Plugin, das nicht im Marketplace steht, entsteht dieser `PluginNode` nie, und damit
gibt es keine Bilderliste - unabhaengig vom Inhalt des ZIP. Ueber ein eigenes
Plugin-Repository laesst es sich auch nicht nachreichen: Der Parser dafuer
(`RepositoryHelper`) kennt keine Screenshots. Anders als bei den Anpassungsschnittstellen der
Detailseite ist das keine Frage abgekuendigter oder interner API (ADR-008), sondern schlicht
fehlender Daten.

Ein Karussell mit Schaltern gibt es deshalb nur dort, wo die Oberflaeche uns gehoert: im
Info-Dialog des Plugins (REQ-081).

## Versionsstand

`pluginVersion` in `gradle.properties` ist die einzige Quelle der Version (REQ-073 sinngemaess:
zentral gepflegt, nicht mehrfach). Sie steht auf `0.3.0`; die zugehoerigen Aenderungen sind in
`change-notes.html` protokolliert.

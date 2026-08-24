---
id: REQ-086
titel: Overview-Text und What's New in der Plugin-Liste
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
den Beschreibungstext, der Reiter **What's New** die Aenderungen je Version, neueste zuerst.

Beide Texte stehen als Dateien in `src/main/plugin-meta/` und werden von `patchPluginXml` in
die `plugin.xml` eingetragen; die haelt sie nicht mehr selbst. So lassen sie sich ohne
XML-Escaping pflegen, und die Versionsstaende stehen an einer Stelle beieinander.

## Keine Bilder im Overview

Bilder waren dort und mussten wieder weichen - nicht aus Geschmack, sondern weil die Seite
darauf mit einer waagerechten Scrollbar reagiert und der Text dann **gar nicht mehr umbricht**.

Der Grund steht in `PluginDetailsPageComponent.createHtmlImageViewHandler`: Die Detailseite
laesst sich jede `ImageView` melden, vergleicht deren `getPreferredSpan(X_AXIS)` mit der Breite
ihres Scroll-Bereichs und schaltet bei Ueberbreite `setHorizontalScrollBarPolicy` ein. Beim
ersten Layout ist diese Breite noch 0, der Vergleich faellt also unabhaengig von der Bildbreite
aus - eine kleinere Angabe als `width="360"` half daher nicht. Und sobald der Bereich waagerecht
scrollbar ist, richtet sich das `JEditorPane` nicht mehr nach der Fensterbreite: Jeder Absatz
landet in einer einzigen langen Zeile.

Die Bildstrecke liegt deshalb dort, wo die Oberflaeche uns gehoert - im Info-Dialog des Plugins
(REQ-081), mit Vor/Zurueck, Bildunterschrift und Zoom. Der Beschreibungstext verweist darauf.

Zwischenzeitlich waren die Bilder ueber absolute `file:`-URLs auf die Arbeitskopie eingebunden;
das war ohnehin nur auf diesem einen Rechner tragfaehig.

## Kein Karussell

Das Bilderkarussell, das andere Plugins an dieser Stelle zeigen (etwa Junie), ist nicht
nachbaubar. Zustaendig dafuer ist `PluginImagesComponent`, gefuellt wird es ueber
`PluginNode.setScreenShots(...)`, und dessen einzige Aufrufer sind `IntellijPluginMetadata` und
`PluginDto` - also die **Marketplace-Metadaten** zu einer `xmlId`. Die Bilder liest die
Komponente anschliessend aus `<system>/plugins/imageCache/<externalPluginId>/`, dem Cache des
zuvor Heruntergeladenen.

Fuer ein Plugin, das nicht im Marketplace steht, entsteht dieser `PluginNode` nie. Ueber ein
eigenes Plugin-Repository laesst es sich auch nicht nachreichen: Der Parser dafuer
(`RepositoryHelper`) kennt keine Screenshots. Anders als bei den Anpassungsschnittstellen der
Detailseite (alle `@ApiStatus.Internal`, ADR-008) ist das keine Frage der API, sondern
fehlender Daten.

## Versionsstand

`pluginVersion` in `gradle.properties` ist die einzige Quelle der Version. Sie steht auf
`0.3.0`; die zugehoerigen Aenderungen sind in `change-notes.html` protokolliert.

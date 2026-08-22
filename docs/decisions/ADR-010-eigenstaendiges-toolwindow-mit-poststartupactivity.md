---
id: ADR-010
titel: Eigenstaendiges ToolWindow mit postStartupActivity
betrifft:
  - src/main/resources/META-INF/plugin.xml
  - src/main/kotlin/de/example/gradletasksview/toolwindow/GradleTasksExtendedToolWindowFactory.kt
  - src/main/kotlin/de/example/gradletasksview/toolwindow/GradleTasksExtendedStartupActivity.kt
quelle: Technischer Rechercheauftrag der urspruenglichen Spezifikation
---

Die Ansicht wird als eigenes `toolWindow`-Extension-Element mit eigener `id`, eigener
`factoryClass` und `anchor="right"` registriert - nicht innerhalb der bestehenden
Gradle-ToolWindow-Gruppe. Das folgt direkt aus ADR-001: Es werden keine fremden Komponenten
gepatcht.

Zusaetzlich legt eine `postStartupActivity` (`ProjectActivity` mit
`suspend fun execute(project)`) den Refresher-Dienst frueh an, damit er auf Reimport-Events
hoert, noch bevor das ToolWindow ueberhaupt sichtbar wird.

Nebeneffekt der eigenen `id`: Es gibt keine Kollision mit der originalen Gradle-Ansicht oder
mit anderen Plugins, die diese patchen.

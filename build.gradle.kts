import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

// Alle Versionen stehen zentral in `gradle/libs.versions.toml` (Gradle Version Catalog).
// In dieser Datei darf deshalb keine Versionsnummer mehr hartkodiert werden.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)

    // Reine Wartungswerkzeuge fuer den Version Catalog, siehe Abschnitt am Dateiende.
    // Sie veraendern weder das gebaute Plugin noch dessen Abhaengigkeiten.
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.version.catalog.update)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Produktcode weiterhin aus gradle.properties (keine Version), die Version aus dem
        // Version Catalog - `libs.versions.intellijIdea` liefert einen `Provider<String>` und
        // bleibt damit konfigurationscache-tauglich.
        create(
            providers.gradleProperty("platformType"),
            libs.versions.intellijIdea,
        )
        // Hinweis: Die im urspruenglichen Plan angenommene ID "org.jetbrains.plugins.gradle"
        // bezeichnet in aktuellen Plattformversionen nur noch das optionale "Gradle for Java"-
        // Sub-Plugin. Die Basis-Gradle-Integration (u. a. GradleConstants, Sync-Datenmodell)
        // wurde unter der ID "com.intellij.gradle" ausgelagert - siehe docs/INTEGRATION_STRATEGY.md.
        bundledPlugin("com.intellij.gradle")
        // Bewusst KEIN testFramework(TestFrameworkType.Platform) hier: die bisherigen Tests
        // sind reine Modelllogik-Unit-Tests ohne Platform-Testframework (siehe Plan.md,
        // Abschnitt "Testing"). testFramework(Platform) zieht einen JUnit5-Launcher-Listener
        // der IDE-Sandbox nach sich, der ohne vorbereitete Sandbox fehlschlaegt. Sobald die
        // Integrationstests mit BasePlatformTestCase (Plan-Implementierungsschritt 16)
        // hinzukommen, sollte dafuer ein eigenes Source-Set mit testFramework(Platform)
        // angelegt werden, damit die schnellen Unit-Tests unabhaengig davon bleiben.
    }

    // Fuer das YAML-basierte Default-/Custom-Konfigurationssystem des Expand-States
    // (siehe persistence/GroupExpandYaml.kt). IntelliJ-Plugin-Klassenlader sind pro
    // Plugin isoliert, daher ist eine eigene, gebuendelte SnakeYAML-Version hier
    // unproblematisch (keine Kollision mit einer eventuell von der Plattform selbst
    // verwendeten Version). Version: siehe gradle/libs.versions.toml.
    implementation(libs.snakeyaml)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    // Reines Kotlin-Projekt ohne Java-Quellen: die NotNull-Bytecode-Instrumentierung
    // ist fuer Java-Annotationen gedacht und bei Kotlin ueberfluessig, da Kotlin
    // Null-Sicherheit bereits zur Kompilierzeit erzwingt.
    instrumentCode = false

    pluginConfiguration {
        id = providers.gradleProperty("pluginGroup")
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // Bewusst kein untilBuild: laut Plan wird ausschliesslich die
            // aktuellste unterstuetzte IntelliJ IDEA Ultimate Version unterstuetzt.
        }
    }

    // Kein Marketplace-Vertrieb -> kein publishPlugin{} / kein signPlugin{} Setup.
    // Verteilung erfolgt ausschliesslich per "Install Plugin from Disk" (siehe docs/INSTALLATION.md).
}

tasks {
    test {
        useJUnitPlatform()
    }
}

// Test-/Demo-Tasks, um Sort- und Nested-Modus mit dem Gruppenpfad-Beispiel aus
// Plan.md (Abschnitt "Nested") gegen ein echtes, importiertes Gradle-Projekt
// auszuprobieren. Jeder Task gibt beim Ausfuehren lediglich seine eigene Gruppe aus.
listOf(
    "docker/gui",
    "docker/gui/jsp",
    "docker/gui/react",
    "docker/server/gui"
).forEach { groupPath ->
    tasks.register("printGroup-${groupPath.replace('/', '-')}") {
        group = groupPath
        description = "Test-Task fuer Gradle Tasks Extended: gibt die eigene Gruppe aus."
        doLast {
            println("Gruppe: $groupPath")
        }
    }
}


// ---------------------------------------------------------------------------------------------
// Versions-Updates fuer gradle/libs.versions.toml
//
//   ./gradlew dependencyUpdates
//       Bericht, welche Versionen veraltet sind (ben-manes). Aendert nichts.
//
//   ./gradlew versionCatalogUpdate --interactive --no-configuration-cache
//       Schreibt die Vorschlaege nach `gradle/libs.versions.updates.toml`, damit sie vor der
//       Uebernahme geprueft werden koennen. Anschliessend uebernimmt
//       `./gradlew versionCatalogApplyUpdates` sie in den Katalog; die Vorschlagsdatei ist
//       ein Wegwerf-Artefakt und wird danach geloescht.
//
//   ./gradlew versionCatalogUpdate --no-configuration-cache
//       Uebernimmt alle Updates direkt in gradle/libs.versions.toml.
//
// `--no-configuration-cache` ist bei den versionCatalog*-Tasks noetig: Sie greifen zur
// Ausfuehrungszeit auf `Task.project` zu, was mit dem (hier per gradle.properties global
// aktivierten) Configuration Cache nicht unterstuetzt ist. Ohne das Flag laeuft der Task zwar
// durch, verwirft aber den Cache-Eintrag mit Warnungen. `dependencyUpdates` ist davon nicht
// betroffen.
//
// OFFENGELEGT: Genau dieser `Task.project`-Zugriff ist eine von Gradle abgekuendigte API und
// wird "mit Gradle 10 zu einem Fehler". Die Abkuendigung stammt NICHT aus diesem Buildskript -
// `./gradlew test composedJar --warning-mode all` meldet null Deprecations -, sondern aus dem
// Fremd-Plugin `nl.littlerobots.version-catalog-update` (hier 1.1.1, die aktuelle Version; das
// Plugin selbst ist nicht abgekuendigt). Vor einem Wechsel auf Gradle 10 muss daher geprueft
// werden, ob eine bereinigte Version vorliegt - andernfalls entfaellt das automatische
// Zurueckschreiben und der Katalog wird anhand des `dependencyUpdates`-Berichts von Hand
// gepflegt.
//
// Zwei erwartbare Meldungen im Bericht, die kein Problem sind:
//   - "Failed to determine the latest version for idea:idea:<version>" - die Ziel-IDE ist kein
//     regulaeres Maven-Artefakt mit Versions-Metadaten; sie wird ueber `intellijIdea` im
//     Katalog von Hand gepflegt.
//   - Kotlin-/JUnit-Sprunge auf Hauptversionen erscheinen unter "later milestone versions";
//     ob sie zur Zielplattform passen, ist eine bewusste Entscheidung, kein Automatismus.
//
// Beide Tasks sind Wartungswerkzeuge und laufen nie als Teil von `build`/`buildPlugin`.
// ---------------------------------------------------------------------------------------------

versionCatalogUpdate {
    // Reihenfolge der Eintraege beibehalten: Der Katalog ist bewusst thematisch gruppiert und
    // ausfuehrlich kommentiert - alphabetisches Umsortieren wuerde Kommentare von ihren
    // Eintraegen trennen.
    sortByKey = false

    keep {
        // ZWINGEND fuer dieses Projekt: `intellijIdea` wird von keinem [libraries]- oder
        // [plugins]-Eintrag ueber `version.ref` referenziert, sondern direkt im
        // `intellijPlatform`-Block via `libs.versions.intellijIdea` gelesen. Ohne diese
        // Einstellung haelt `versionCatalogUpdate` den Eintrag fuer unbenutzt und LOESCHT ihn -
        // der Build waere danach nicht mehr konfigurierbar.
        keepUnusedVersions = true

        // Zusaetzlich noch explizit beim Namen genannt, damit der Zusammenhang auch dann
        // erhalten bleibt, wenn `keepUnusedVersions` spaeter einmal umgestellt wird.
        // (Die frueheren Flags `keepUnusedLibraries`/`keepUnusedPlugins` aus 0.8.x gibt es in
        // 1.x nicht mehr - unbenutzte Libraries/Plugins werden dort ohnehin nicht entfernt.)
        versions.add("intellijIdea")
    }
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    // Keine Vorabversionen vorschlagen, solange die aktuell verwendete Version stabil ist -
    // sonst schlaegt der Bericht z. B. Kotlin-Betas oder EAP-Builds vor.
    rejectVersionIf {
        isNonStableVersion(candidate.version) && !isNonStableVersion(currentVersion)
    }
}

/** `true`, wenn die Versionsnummer nach Vorab-/Entwicklungsversion aussieht (RC, Beta, EAP, ...). */
fun isNonStableVersion(version: String): Boolean {
    val hasStableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val looksLikePlainVersion = "^[0-9,.v-]+(-r)?$".toRegex().matches(version)
    return !hasStableKeyword && !looksLikePlainVersion
}

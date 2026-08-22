// Alle Versionen stehen zentral in `gradle/libs.versions.toml` (Gradle Version Catalog).
// In dieser Datei darf deshalb keine Versionsnummer mehr hartkodiert werden.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
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

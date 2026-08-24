import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import java.time.LocalDateTime

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
        // wurde unter der ID "com.intellij.gradle" ausgelagert - siehe ADR-002.
        bundledPlugin("com.intellij.gradle")
        // Bewusst KEIN testFramework(TestFrameworkType.Platform) hier: die bisherigen Tests
        // sind reine Modelllogik-Unit-Tests ohne Platform-Testframework (siehe REQ-060,
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
    // Die Zielplattform 2026.2 laeuft auf Java 25 und erwartet dieselbe Bytecode-Version von
    // Plugins - `verifyPluginProjectConfiguration` meldet jede Abweichung. Setzt Java- und
    // Kotlin-Ziel gemeinsam, deshalb kein separates `compilerOptions.jvmTarget`.
    jvmToolchain(25)
}

// Quellen fuer die beiden Reiter in Einstellungen > Plugins (REQ-086). Als Dateien und nicht
// in der plugin.xml, damit sich beide Texte ohne XML-Escaping pflegen lassen und patchPluginXml
// sie beim Bauen eintraegt.
val pluginMetaDir = layout.projectDirectory.dir("src/main/plugin-meta").asFile

intellijPlatform {
    // Reines Kotlin-Projekt ohne Java-Quellen: die NotNull-Bytecode-Instrumentierung
    // ist fuer Java-Annotationen gedacht und bei Kotlin ueberfluessig, da Kotlin
    // Null-Sicherheit bereits zur Kompilierzeit erzwingt.
    instrumentCode = false

    pluginConfiguration {
        id = providers.gradleProperty("pluginGroup")
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Einzige Quelle fuer den Herausgeber: patchPluginXml schreibt ihn in die plugin.xml,
        // dieselben Werte landen ueber generateBuildInfo im Info-Dialog (REQ-080).
        vendor {
            name = providers.gradleProperty("pluginVendor")
            email = providers.gradleProperty("pluginVendorEmail")
            url = providers.gradleProperty("pluginVendorUrl")
        }

        // Reiter "Overview": nur Text, keine Bilder (REQ-086).
        //
        // Bilder waren einmal hier und mussten wieder weichen. Die Detailseite meldet jede
        // ImageView an einen eigenen Handler, der ihre Breite mit der des Scroll-Bereichs
        // vergleicht und bei Ueberbreite die waagerechte Scrollbar einschaltet
        // (PluginDetailsPageComponent.createHtmlImageViewHandler). Beim ersten Layout ist diese
        // Breite 0, also greift das immer - und ein JEditorPane in einem horizontal scrollbaren
        // Bereich richtet sich nicht mehr nach der Fensterbreite: Jeder Absatz landet in einer
        // einzigen langen Zeile. Die Bildstrecke liegt deshalb dort, wo die Oberflaeche uns
        // gehoert (Info-Dialog, REQ-081).
        description = providers.fileContents(
            layout.projectDirectory.file("src/main/plugin-meta/description.html")
        ).asText.map { it.substringAfter("-->").trim() }

        // Reiter "What's New" (REQ-086).
        changeNotes = provider {
            pluginMetaDir.resolve("change-notes.html").readText().substringAfter("-->").trim()
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // Bewusst kein untilBuild: laut Plan wird ausschliesslich die
            // aktuellste unterstuetzte IntelliJ IDEA Ultimate Version unterstuetzt.
        }
    }

    // Kein Marketplace-Vertrieb -> kein publishPlugin{} / kein signPlugin{} Setup.
    // Verteilung erfolgt ausschliesslich per "Install Plugin from Disk" (siehe docs/INSTALLATION.md).

    // `./gradlew verifyPlugin` laesst den IntelliJ Plugin Verifier ueber die gebaute ZIP laufen
    // und meldet Aufrufe entfernter oder interner API. Gegen WELCHE IDEs geprueft wird, muss
    // man ihm sagen - er leitet es nicht aus der plugin.xml ab. Hier ist das genau die eine
    // Zielversion aus dem Version Catalog (REQ-050): Sie liegt durch den Build ohnehin lokal
    // vor, und mehr Versionen zu pruefen haette ohne Rueckwaertskompatibilitaet keinen Zweck.
    pluginVerification {
        ides {
            // `current()` = genau die Plattform, gegen die auch uebersetzt wird. Sie liegt
            // durch den Build ohnehin lokal vor, es wird also nichts zusaetzlich geladen.
            // Alternativen waeren `recommended()` (alle Releases im since/until-Bereich der
            // plugin.xml) oder `create(IntellijIdeaUltimate, "<version>")` - beides zieht
            // weitere IDE-Distributionen und lohnt nur mit Rueckwaertskompatibilitaet.
            current()
        }

        // Woran der Build scheitern soll, ausdruecklich aufgezaehlt statt ueber `ALL`: Zwei
        // Kategorien meldet der Verifier hier bekanntermassen und unvermeidbar, weil Kotlin
        // fuer die Java-Default-Methoden von ToolWindowFactory automatisch Ueberschreibungen
        // erzeugt (isApplicable, isDoNotActivateOnStart, manage, getAnchor, getIcon) - im
        // Quellcode steht davon nichts, und der Compiler warnt entsprechend auch nicht. Dazu
        // kommt IconUtil.downscaleIconToSize als experimentelle API.
        //
        //   DEPRECATED_API_USAGES, EXPERIMENTAL_API_USAGES
        //
        // INTERNAL_API_USAGES steht bewusst NICHT in dieser Ausnahme: Die einzige interne
        // Nutzung war ExternalSystemTaskLocation, und die ist seit dem eigenen
        // RunConfigurationProducer verschwunden (ADR-022).
        failureLevel = listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
            VerifyPluginTask.FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
            VerifyPluginTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
            VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
            VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES,
        )
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

// ---------------------------------------------------------------------------------------------
// Build-Infos fuer den Info-Dialog der Ansicht (REQ-080)
//
// Version, Plugin-ID und Zielplattform stehen in gradle.properties bzw. im Version Catalog,
// nicht im Quellcode - der Dialog liest sie deshalb aus einer beim Build erzeugten
// `build-info.properties` im Ressourcen-Wurzelverzeichnis. Die Plattform-API waere der
// naheliegende Weg gewesen, aber alle Zugaenge zum eigenen Plugin-Deskriptor
// (`PluginManagerCore.getPlugin`, `PluginManager.getPluginByClass`, `findEnabledPlugin`) sind
// mit @ApiStatus.Internal markiert und damit nach ADR-008 tabu.
//
// Der Zeitstempel ist bewusst KEIN Task-Input: Sonst waere die Aufgabe bei jedem Build
// veraltet und zoege Jar und Plugin-ZIP jedes Mal mit. So steht dort der Zeitpunkt, zu dem die
// Datei zuletzt wirklich neu erzeugt wurde.
// ---------------------------------------------------------------------------------------------

abstract class GenerateBuildInfo : DefaultTask() {
    @get:Input abstract val pluginId: Property<String>
    @get:Input abstract val pluginName: Property<String>
    @get:Input abstract val pluginVersion: Property<String>
    @get:Input abstract val sinceBuild: Property<String>
    @get:Input abstract val pluginVendor: Property<String>
    @get:Input abstract val pluginVendorEmail: Property<String>
    @get:Input abstract val pluginVendorUrl: Property<String>
    @get:Input abstract val gitBranch: Property<String>

    /**
     * Nur das Ja/Nein, nicht die Liste der geaenderten Dateien: Sonst waere die Aufgabe bei
     * jeder Bearbeitung im Arbeitsverzeichnis veraltet.
     */
    @get:Input abstract val gitDirty: Property<Boolean>
    @get:Input abstract val targetPlatform: Property<String>
    @get:Input abstract val gitCommit: Property<String>
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                appendLine("# Erzeugt von der Gradle-Aufgabe generateBuildInfo - nicht von Hand bearbeiten.")
                appendLine("pluginId=" + pluginId.get())
                appendLine("pluginName=" + pluginName.get())
                appendLine("version=" + pluginVersion.get())
                appendLine("sinceBuild=" + sinceBuild.get())
                appendLine("vendor=" + pluginVendor.get())
                appendLine("vendorEmail=" + pluginVendorEmail.get())
                appendLine("vendorUrl=" + pluginVendorUrl.get())
                appendLine("gitBranch=" + gitBranch.get())
                appendLine("gitDirty=" + gitDirty.get())
                appendLine("targetPlatform=" + targetPlatform.get())
                appendLine("gitCommit=" + gitCommit.get())
                appendLine("buildTime=" + LocalDateTime.now().withNano(0))
            }
        )
    }
}

val buildInfoDir = layout.buildDirectory.dir("generated/buildInfo")

val generateBuildInfo = tasks.register<GenerateBuildInfo>("generateBuildInfo") {
    group = "build"
    description = "Schreibt Version, Plugin-ID und Git-Commit fuer den Info-Dialog des Plugins."
    pluginId = providers.gradleProperty("pluginGroup")
    pluginName = providers.gradleProperty("pluginName")
    pluginVersion = providers.gradleProperty("pluginVersion")
    sinceBuild = providers.gradleProperty("pluginSinceBuild")
    pluginVendor = providers.gradleProperty("pluginVendor")
    pluginVendorEmail = providers.gradleProperty("pluginVendorEmail").orElse("")
    pluginVendorUrl = providers.gradleProperty("pluginVendorUrl").orElse("")
    targetPlatform = providers.gradleProperty("platformType")
        .zip(libs.versions.intellijIdea) { type, ide -> "$type $ide" }
    // `isIgnoreExitValue`, damit ein Export ohne .git-Verzeichnis den Build nicht kippt.
    gitCommit = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }
    gitBranch = providers.exec {
        commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }
    gitDirty = providers.exec {
        commandLine("git", "status", "--porcelain")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.isNotBlank() }
    outputFile = buildInfoDir.map { it.file("build-info.properties") }
}

sourceSets["main"].resources.srcDir(buildInfoDir)
tasks.named("processResources") { dependsOn(generateBuildInfo) }

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

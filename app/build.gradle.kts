import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.marcogn.kartlog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.marcogn.kartlog"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Stesso schema di ThePatientGamerHelper: keystore di release fisso (SHA1 stabile),
    // segreti letti da variabili d'ambiente, mai hardcoded. Debug usa il keystore
    // effimero di default di Android Studio/Gradle, nessun segreto richiesto.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Non firmato (default Android) se i segreti di release non sono presenti — es. una
            // ./gradlew assembleRelease locale senza i secret di CI — stesso pattern del
            // riferimento invece di far fallire la build.
            if (!System.getenv("RELEASE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // SPEC §5.4: i JSON di /seed diventano un asset "seed/*.json" impacchettato nell'APK,
    // sia in debug sia in release. Il contenuto arriva da copySeedAssets qui sotto.
    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/seedAssets"))
        }
        // Schema esportati da Room (room.schemaLocation sotto): servono a MigrationTestHelper per
        // MigrationTest (Robolectric, non uno unitTest sourceSet: i test JVM con
        // isIncludeAndroidResources leggono gli asset del variant "debug" via mergeDebugAssets,
        // non un asset set proprio — vedi test_config.properties generato da AGP).
        getByName("debug") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// `assets.srcDir("$projectDir/schemas")` sopra punta a una cartella semplice: KSP ci scrive lo
// schema come effetto collaterale, non come output dichiarato del task, quindi Gradle non deduce
// da solo che mergeDebugAssets va dopo kspDebugKotlin. Dipendenza esplicita per evitare che, nella
// stessa build, l'asset merge legga la cartella schemas prima che KSP l'abbia scritta.
// NB: `app/schemas/` va comunque versionata (vedi Decisioni prese in CLAUDE.md) — Room non la
// rigenera per intero a ogni build, accumula un file per versione nel tempo, quindi dichiararla
// come *output* di kspDebugKotlin (tentato in una versione precedente di questa fix) sarebbe
// sbagliato: Gradle cancellerebbe gli schemi delle versioni precedenti non riscritti dall'ultima
// esecuzione.
tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn("kspDebugKotlin")
}

// --- Dati di gioco (SPEC §5.4) -----------------------------------------------------------

val seedDir = rootProject.file("seed")
val seedgenDir = rootProject.file("tools/seedgen")

// Copia sempre i JSON già versionati: è quello che rende il debug completamente offline.
// Non cacheable nel senso stretto ma innocuo da rieseguire: è una semplice copia di file locali.
val copySeedAssets by tasks.registering(Copy::class) {
    description = "Copia i JSON di /seed negli asset dell'app (debug e release)."
    from(seedDir) { include("*.json") }
    into(layout.buildDirectory.dir("generated/seedAssets/seed"))
}

tasks.named("preBuild") {
    dependsOn(copySeedAssets)
}

// Solo release, prima che i suoi asset vengano assemblati: verifica/rigenera /seed dal wiki
// (SPEC §5.4). copySeedAssets deve girare DOPO, per impacchettare l'eventuale seed accettato.
val generateSeed by tasks.registering {
    description = "Verifica/rigenera i dati di gioco dal wiki (solo release, SPEC §5.4)."
    group = "kartlog"
    outputs.upToDateWhen { false } // la parte di rete non è mai cacheable

    doLast {
        val offline = project.hasProperty("offlineSeed")
        val acceptChanges = project.hasProperty("acceptSeedChanges")
        val pythonExecProperty = project.findProperty("pythonExec") as String?

        val systemPython = pythonExecProperty
            ?: findExecutableOnPath("python3")
            ?: findExecutableOnPath("python")
            ?: throw GradleException(
                "Python 3 non trovato sul PATH. Installalo o passa -PpythonExec=<percorso>."
            )

        val venvDir = layout.buildDirectory.dir("seedgen-venv").get().asFile
        val venvPython = File(venvDir, "bin/python")
        val venvMarker = File(venvDir, "kartlog-requirements-installed")
        if (!venvMarker.exists()) {
            runProcess(listOf(systemPython, "-m", "venv", venvDir.absolutePath), seedgenDir)
            // python -m venv non fallisce sempre se ensurepip non è disponibile (crea la
            // struttura della venv comunque, senza pip) — controllato esplicitamente per un
            // errore chiaro invece del ModuleNotFoundError confuso che darebbe seedgen dopo.
            val (pipCheckExit, pipCheckOutput) =
                runProcessCapturingOutput(listOf(venvPython.absolutePath, "-m", "pip", "--version"), seedgenDir)
            if (pipCheckExit != 0) {
                throw GradleException(
                    "pip non disponibile nella venv creata in $venvDir (ensurepip mancante):\n$pipCheckOutput\n" +
                        "Installa il pacchetto di sistema che fornisce python3-venv/ensurepip, poi rilancia."
                )
            }
            runProcess(
                listOf(venvPython.absolutePath, "-m", "pip", "install", "-q", "-r", "requirements.txt"),
                seedgenDir,
            )
            venvMarker.writeText("ok")
        }

        val workDir = layout.buildDirectory.dir("seedgen/candidate").get().asFile
        val releaseArgs = mutableListOf(
            venvPython.absolutePath, "-m", "seedgen", "release",
            "--seed", seedDir.absolutePath, "--work", workDir.absolutePath,
        )
        if (offline) releaseArgs += "--offline"

        val (releaseExit, releaseOutput) = runProcessCapturingOutput(releaseArgs, seedgenDir)
        when (releaseExit) {
            0 -> logger.lifecycle("[generateSeed] $releaseOutput")
            3 -> {
                if (!acceptChanges) {
                    throw GradleException(
                        "I dati del wiki sono cambiati rispetto a /seed:\n$releaseOutput\n" +
                            "Rilancia con -PacceptSeedChanges per accettarli."
                    )
                }
                val (acceptExit, acceptOutput) = runProcessCapturingOutput(
                    listOf(
                        venvPython.absolutePath, "-m", "seedgen", "accept",
                        "--seed", seedDir.absolutePath, "--candidate", workDir.absolutePath,
                    ),
                    seedgenDir,
                )
                if (acceptExit != 0) {
                    throw GradleException("`seedgen accept` fallito (exit $acceptExit):\n$acceptOutput")
                }
                logger.lifecycle("[generateSeed] $acceptOutput")
                prependChangelogEntry(rootProject.file("CHANGELOG.md"), acceptOutput.trim())
            }
            2 -> throw GradleException("Dati di gioco non validi, build fermata:\n$releaseOutput")
            4 -> throw GradleException("Rete non disponibile verso mariowiki.com:\n$releaseOutput\nUsa -PofflineSeed per saltare la verifica.")
            1 -> throw GradleException("Configurazione di seedgen non valida:\n$releaseOutput")
            else -> throw GradleException("`seedgen release` ha restituito l'exit code $releaseExit:\n$releaseOutput")
        }
    }
}

tasks.matching { it.name == "mergeReleaseAssets" }.configureEach {
    dependsOn(generateSeed)
}
// copySeedAssets è condiviso da debug e release: mustRunAfter (non dependsOn) impone l'ordine
// solo quando generateSeed è già nel grafo (cioè in una build di release), mai in debug.
copySeedAssets.configure {
    mustRunAfter(generateSeed)
}

fun findExecutableOnPath(name: String): String? {
    val path = System.getenv("PATH") ?: return null
    return path.split(File.pathSeparator)
        .map { File(it, name) }
        .firstOrNull { it.canExecute() }
        ?.absolutePath
}

fun runProcess(command: List<String>, workingDir: File) {
    val (exit, output) = runProcessCapturingOutput(command, workingDir)
    if (exit != 0) throw GradleException("${command.joinToString(" ")} fallito (exit $exit):\n$output")
}

fun runProcessCapturingOutput(command: List<String>, workingDir: File): Pair<Int, String> {
    val stdout = ByteArrayOutputStream()
    val result = project.exec {
        commandLine(command)
        this.workingDir = workingDir
        standardOutput = stdout
        errorOutput = stdout
        isIgnoreExitValue = true
    }
    return result.exitValue to stdout.toString(Charsets.UTF_8).trim()
}

/**
 * Inserisce [line] come primo bullet sotto "## [Unreleased]" in CHANGELOG.md, nel formato del
 * progetto (`- **Sintesi.** dettaglio`, vedi CLAUDE.md "Changelog and release process").
 */
fun prependChangelogEntry(changelog: File, line: String) {
    val entry = Regex("""^(.*)\(seedVersion (\d+); (.*)\)$""").find(line)?.let { m ->
        val (prefix, version, revs) = m.destructured
        "- **${prefix.trim()} (seedVersion $version).** $revs"
    } ?: "- $line"

    val lines = changelog.readLines().toMutableList()
    val headingIndex = lines.indexOfFirst { it.trim() == "## [Unreleased]" }
    if (headingIndex == -1) throw GradleException("CHANGELOG.md: sezione [Unreleased] non trovata")
    // La riga dopo l'intestazione è vuota per convenzione: il nuovo bullet va subito dopo quella.
    val insertAt = if (lines.getOrNull(headingIndex + 1)?.isBlank() == true) headingIndex + 2 else headingIndex + 1
    lines.add(insertAt, entry)
    changelog.writeText(lines.joinToString("\n") + "\n")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Immagini di personaggi/outfit/eventi scaricate a runtime dal CDN di Super Mario Wiki
    // (mai impacchettate: vedi CLAUDE.md, Decisioni prese).
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

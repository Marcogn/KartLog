# Aggiornamento delle dipendenze (2026-10-04)

Stesso aggiornamento fatto su PdfToolkit il 2026-10-01 (il suo `docs/plan.md`, "Dependency
upgrade", ha i passi e le fonti), ripetuto qui su richiesta dell'autore. "Sicuro" vuol dire: solo
versioni stabili (niente alpha, beta, RC), lette dai `maven-metadata.xml` di Google Maven, Maven
Central e Gradle; le librerie di terze parti restano nella loro major; `targetSdk` non si tocca.

| Componente | Prima | Ora |
|---|---|---|
| Gradle | 8.13 | 9.8.0 (wrapper con `distributionSha256Sum`) |
| Android Gradle Plugin | 8.13.0 | 9.4.1 |
| Kotlin | 2.0.21 | 2.4.20 |
| KSP | 2.0.21-1.0.28 | 2.3.12 |
| Hilt | 2.52 | 2.60.1 |
| `androidx.hilt` | `hilt-navigation-compose` 1.2.0 | `hilt-lifecycle-viewmodel-compose` 1.4.0 |
| Compose BOM | 2024.12.01 | 2026.09.00 |
| Navigation Compose | 2.8.5 | 2.10.2 |
| Lifecycle | 2.8.7 | 2.11.0 |
| Activity Compose | 1.9.3 | 1.13.0 |
| Core KTX | 1.13.1 | 1.19.1 |
| Room | 2.6.1 | 2.8.5 |
| DataStore | 1.1.1 | 1.2.1 |
| AppCompat | 1.7.0 | 1.8.0 |
| kotlinx.serialization | 1.7.3 | 1.11.0 |
| kotlinx.coroutines | 1.9.0 | 1.11.0 |
| Coil | 3.0.4 | 3.6.3 (stessa major) |
| Palette | 1.0.0 | 1.0.0 (è l'ultima) |
| Robolectric | 4.16.1 | 4.17 (sempre con `sdk=34`) |
| Espresso | 3.6.1 | 3.7.0 |
| `compileSdk` | 36 | 37 (il massimo per AGP 9.4) |
| `targetSdk` | 36 | **36, invariato** |

## Cosa è cambiato nel codice

- Niente plugin `org.jetbrains.kotlin.android`: AGP 9 compila Kotlin da sé. `kotlinOptions`
  diventa `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }`.
- `hiltViewModel` si importa da `androidx.hilt.lifecycle.viewmodel.compose` (12 file).
- `Project.exec` è stato rimosso in Gradle 9.0
  ([guida all'aggiornamento](https://docs.gradle.org/9.8.0/userguide/upgrading_major_version_9.html),
  "Removal of Project#exec"): `runProcessCapturingOutput` in `app/build.gradle.kts` (usata da
  `generateSeed`) ora usa `ProcessBuilder`, con stdout e stderr nello stesso flusso come prima.
  Provato con `assembleRelease -PofflineSeed` (crea la venv, installa i requisiti, lancia seedgen).
- AGP 9 rifiuta un `Provider` in `sourceSets.main.assets.srcDir(...)`: la cartella degli asset del
  seed è passata come `File`. L'ordine con `copySeedAssets` lo dà ancora `preBuild.dependsOn`.
  Verificato: l'APK debug contiene i 15 file `assets/seed/*.json`.

## Perché `targetSdk` resta 36

`compileSdk` decide solo con quali API si compila; `targetSdk` cambia il comportamento dell'app a
runtime sui telefoni con Android 17. PdfToolkit l'ha alzato a 37 insieme al resto; qui, per un
aggiornamento "sicuro", resta 36 finché qualcuno non prova l'app su un dispositivo con Android 17.
Lint lo segnala come avviso (`OldTargetApi`), non come errore.

## Verificato

`lintDebug` (0 errori), `testDebugUnitTest` (74 test, gli stessi di prima, tutti verdi),
`assembleDebug`, `assembleRelease -PofflineSeed`. Lo schema Room esportato non cambia.

**Non verificato**: l'app su un telefono. Da provare prima del merge: avvio, Personaggi con le
immagini (Coil 3.6), Mappa (zoom e trascinamento), cambio lingua e tema, backup export/import.

## Avvisi rimasti

- Deprecazione nuova (avviso del compilatore, non errore): `createComposeRule` di `junit4` nei 4 test
  Compose (sostituto: `junit4.v2.createComposeRule`, che usa `StandardTestDispatcher` e può chiedere
  sincronizzazioni esplicite). Lasciata così: toccarla non fa parte di un aggiornamento sicuro.
- Due `!!` superflui segnalati dal nuovo compilatore, innocui.

# KartLog — memoria di progetto per l'agente

App Android offline per tracciare i collectibles di Mario Kart World (outfit, Monete Peach, Pulsanti P, pannelli "?") e consigliare quale Gran Premio o Knockout Tour correre, e con chi, per sbloccare più outfit.
Questo file contiene regole, protocollo, comandi, convenzioni e stato. Non ripete gli altri documenti:

| Documento | Cosa contiene |
|---|---|
| `SPEC.md` | Specifica funzionale e tecnica dell'app com'è (UI, modello dati, seed, algoritmo Consigliami) |
| `docs/roadmap.md` | Le prossime fasi, con modello assegnato, scope e "Fatto quando" |
| `docs/decisioni.md` | Ogni scelta non ovvia e il suo perché, per argomento. **Leggila prima di cambiare dati, fonti o grafica** |
| `docs/ci.md` | I workflow di GitHub Actions (condivisi con gli altri progetti Android dell'autore) |
| `tools/seedgen/README.md` | Uso dello script che genera `seed/` |

## Regole non negoziabili
1. **Nessun dato di gioco inventato.** Tutto arriva da `seed/`, generato da `tools/seedgen`. Mai modificare `seed/` a mano, mai scrivere dati di gioco nel codice o nei test (per i test Kotlin si usa un seed fittizio marcato `FAKE_FOR_TESTS`).
2. **Nessun asset Nintendo** nel repository né nell'APK: niente render, loghi, font, screenshot. Le immagini di gioco sono solo URL nel seed, scaricate a runtime. Unica eccezione voluta dall'autore: le icone della Home e il banner del suo mockup (`res/drawable-nodpi/home_*`, `kart_*`). Niente "Mario", "Mario Kart" o "Nintendo" nel nome dell'app o nel package.
3. **App offline tranne le immagini**: INTERNET serve solo al download delle immagini (CDN di Super Mario Wiki e mappa di mkworld-checklist da GitHub). I dati di gioco si scaricano solo al build release (seedgen).
4. **I conteggi in `tools/seedgen/expected_counts.yaml` non si rilassano mai** per far passare build o test. Si cambiano solo se il gioco cambia, in un commit dedicato che cita la fonte.
5. Se un requisito è ambiguo o manca un dato, **fermati e chiedi**. Non scegliere il valore "plausibile".
6. A fine sessione il progetto compila e i test passano. Migrazioni Room sempre scritte e testate, mai `fallbackToDestructiveMigration()` (utenti reali).

## Protocollo di sessione
Quando l'autore scrive "vai avanti con la prossima fase" (o simile):
0. **Controllo del modello, prima di tutto.** Prendi la prossima fase da **Stato attuale** e il suo modello dalla tabella in `docs/roadmap.md`. Controlla su quale modello stai girando (nel system prompt; in una sessione cloud di claude.ai lo dice il tool `get_session`). Se non è quello assegnato, o non lo sai, **fermati** e rispondi in una riga: fase, modello richiesto, come cambiarlo (`/model opus`, `/model sonnet` o una nuova sessione). Non leggere prima SPEC o codice. Vai avanti comunque solo se l'autore lo dice esplicitamente.
1. Leggi la sezione della fase in `docs/roadmap.md` (obiettivo, prerequisiti, scope, "Fatto quando") e le parti di `SPEC.md` e `docs/decisioni.md` che tocca.
2. Controlla i prerequisiti **prima** di scrivere codice. Se ne manca uno, fermati e chiedi.
3. Esegui **solo** quella fase. Ciò che appartiene a una fase successiva va annotato in Stato attuale, non implementato.
4. Chiudi con i punti "Fatto quando" della fase e quelli comuni in fondo a `docs/roadmap.md`. Una fase Opus scrive un **handoff** (max 15 righe) in Stato attuale per la successiva; ogni fase finisce elencando i **controlli a schermo** (qui non c'è emulatore).
5. Non iniziare la fase successiva nella stessa sessione, a meno che l'autore non lo chieda.

## Comandi
```bash
./gradlew assembleDebug       # APK debug: nessuna rete, nessun Python
./gradlew testDebugUnitTest   # test JVM (Robolectric sdk=34)
./gradlew lint                # Android Lint

cd tools/seedgen && python -m pytest                    # test dello script, senza rete
python -m seedgen validate --seed ../../seed            # valida seed/
python -m seedgen release --seed ../../seed --work ../../app/build/seedgen/candidate   # quello che fa Gradle in release

./gradlew generateSeed -PofflineSeed          # valida seed/ senza rete (come la CI)
./gradlew assembleRelease -PacceptSeedChanges # accetta i dati cambiati e aggiorna da solo il CHANGELOG
```
Flag della build release: `-PacceptSeedChanges`, `-PofflineSeed`, `-PpythonExec=<path>` (SPEC §5.4).
**Ambiente cloud:** Android SDK da installare in `/opt/android-sdk` con `sdkmanager` (piattaforma `android-37.0`; `local.properties` è gitignorato); Gradle con `LC_ALL=C.UTF-8` (nomi di test accentati); Maven Central può rispondere 429, basta ritentare; seedgen funziona col `python3` di sistema (a volte la venv di Gradle non si crea per `ensurepip` mancante).

## Convenzioni
- UI e documentazione in italiano (eccetto `docs/ci.md`, copiato da PdfToolkit), identificatori in inglese. UI IT (`values/`) + EN (`values-en/`). CHANGELOG: ogni modifica visibile sotto `## [Unreleased]` come `- **Sintesi.** dettaglio` (`release.yml` legge i grassetti).
- Stack e struttura ricalcano **ThePatientGamerHelper** (l'autore lo mette nel contesto, non va cercato online): Kotlin, Compose, Material 3, Room, Hilt, MVVM con `StateFlow`. Package `com.marcogn.kartlog`: `ui/<feature>/` (`home`, `skin` = Personaggi, `medallions`, `pswitches`, `panels`, `map`, `consigliami`, `results`, `settings`, più `theme`, `navigation`, `common`), `data/`, `domain/`, `di/`.
- ID dei dati di gioco: slug stabili di seedgen (`mario__touring`, `cup_mushroom`), mai autoincrement.
- **Dati:** le entità Room (SPEC §3) sono la fonte di verità; `SeedRepository` fa il reseed confrontando `seedVersion` senza toccare lo stato utente. A runtime solo gli asset `assets/seed/*.json` copiati da `copySeedAssets`, mai `seed/`. Un DAO per schermata con query SQL dirette (JOIN, GROUP_CONCAT) e un solo `Flow`. Schemi Room versionati in `app/schemas/`.
- **Navigazione:** `Destination` sealed interface con rotte `@Serializable`. Drawer `ModalNavigationDrawer` (`ui/navigation/KartDrawer.kt`), voci con `popUpTo(Home){saveState}` + `launchSingleTop` + `restoreState`, tranne Home (`popUpTo<Home>{inclusive}` + `launchSingleTop`). Transizioni 300 ms slide+fade su `NavHost`, anche `predictivePop*`. Guardia `lifecycleIsResumed()` su ogni `navigate()`/`popBackStack()`.
- **Grafica "da gioco"** (mockup dell'autore), da usare in ogni schermata nuova: `ui/common/KartChrome.kt` (`KartTopBar` con banner, `KartPanel`, `KartBadge`, `KartCounterPill`, `OutlinedTitle`, `KartTitle`), `Modifier.kartSky(dark)` dietro ogni `Scaffold` con `containerColor = Color.Transparent`, scelte con `KartChoiceButton`/`KartDropdown` (mai `FilterChip`, mai `KartChoiceButton` in `horizontalScroll`), schede `KartTabs`, popup `KartPopup`/`KartPopupText`/`KartSwitchRow`/`KartInfoButton`, font Lilita One (`KartFont`), `isKartDarkTheme()`. Polaroid in `ui/skin/Polaroid.kt`, colore del cartellino estratto dall'immagine (`rememberImageAccentColor`). Mai `BoxWithConstraints`/SubcomposeLayout dove il genitore chiede misure intrinseche (crash, `KartDropdownTest`). Colori: **grigio = ti manca, colorato = ce l'hai**.
- **Immagini:** `imageUrl` dal seed, caricate con Coil (`CharacterPortrait`, `CharacterAvatar`, `EventIcon` in `ui/common/CharacterAvatar.kt`), messe in cache all'avvio da `WikiImagePrefetcher`; segnaposto con iniziali solo finché non sono caricate.
- **Personaggi:** 50 piloti, i 32 `starter` sempre sbloccati (`CASE WHEN c.starter THEN 1 ELSE COALESCE(cu.unlocked, 0) END` in `SkinDao` e `ConsigliamiDao`); comportamento del tap in `cardBehavior()` (`CharacterCardBehavior.kt`, testato su tutti i piloti del seed reale). L'outfit di default è sempre in `owned_outfits`.
- **Consigliami:** algoritmo puro in `domain/consigliami/` (`ConsigliamiUseCase`, nessuna dipendenza Android), test con dati sintetici; il DAO espone solo `Flow` grezzi, il ViewModel li ricombina. Mai logica di dominio nel ViewModel o nel DAO. Risultati: solo dalla schermata `ui/results/`, un `TrophyRank` per (evento, cilindrata).
- **Localizzazione:** `localizedName(name, nameIt)` con `AppLocale`; ogni ViewModel che localizza passa il suo Flow da `relocalizing()`. `MainActivity : AppCompatActivity` (`setApplicationLocales()`).
- **Backup** (`data/backup/`): l'import sostituisce tutto lo stato utente e riporta gli ID sconosciuti.

## Stato attuale
- **App alla 1.1.2** (roadmap originale chiuso, poi immagini, grafica dal mockup, mappa dei collezionabili, toolchain aggiornata). `seedVersion` 8, Room v7, backup v3. `versionCode`/`versionName` li aggiorna `release.yml`. Secret GitHub presenti.
- **Prossima fase: C1 Dati degli stand (Opus)**, poi C2 sezione Cibi, C3 e C4 grafica di Consigliami (Sonnet): `docs/roadmap.md`. Motivo: in gioco i consigli non corrispondono agli outfit sbloccati, perché SPEC §6.1 dà per certi stand che sono solo nell'area dei percorsi.
- **Da verificare a schermo** (nessun emulatore qui; screenshot Robolectric impossibili: `captureToImage` va in timeout, il disegno manuale crasha): grafica del mockup (tessere, contorno dei titoli, bagliore nel tema scuro, banner su schermi stretti), fluidità e memoria della mappa (immagine 2580×2322, ~24 MB), gesto indietro, immagini con Coil 3.6, lingua/tema, backup. `targetSdk` resta 36 finché l'app non è provata su Android 17.

## Manutenzione di questo file
- A fine sessione: aggiorna **Stato attuale**; le scelte non ovvie vanno in `docs/decisioni.md`, nella sezione giusta (una decisione che ne supera un'altra la sostituisce).
- Se cambia un comando, una convenzione o la struttura, aggiorna la sezione qui nella stessa sessione.
- Resta corto (sotto ~15 KB): i dettagli vanno in `SPEC.md`, `docs/decisioni.md` o nei README, qui solo il rimando.

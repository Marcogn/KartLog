# KartLog — memoria di progetto per l'agente

App Android offline per tracciare i collectibles di Mario Kart World (outfit, Peach Medallions, P Switch) e consigliare quale Gran Premio o Knockout Tour correre, e con chi, per sbloccare più outfit.
La specifica completa è `SPEC.md`. Questo file **non la ripete**: contiene regole operative, comandi, stato e decisioni.

## Regole non negoziabili
1. **Nessun dato di gioco inventato.** Tutto arriva da `seed/`, generato da `tools/seedgen`. Mai modificare `seed/` a mano, mai scrivere dati di gioco nel codice o nei test (per i test Kotlin si usa un seed fittizio marcato `FAKE_FOR_TESTS`).
2. **Nessun asset Nintendo** nel repository: niente render, loghi, font, screenshot. Niente "Mario", "Mario Kart" o "Nintendo" nel nome dell'app, nel package o nell'icona.
3. **App offline**: nessun permesso INTERNET. La rete serve solo al build release (seedgen).
4. **I conteggi in `tools/seedgen/expected_counts.yaml` non si rilassano mai** per far passare build o test. Si cambiano solo se il gioco cambia, in un commit dedicato che cita la fonte.
5. Se un requisito è ambiguo o manca un dato, **fermati e chiedi**. Non scegliere il valore "plausibile".
6. A fine sessione il progetto compila e i test passano.

## Protocollo di sessione
Quando l'autore scrive "vai avanti con la prossima fase" (o simile):
1. Leggi **Stato attuale** qui sotto per sapere qual รจ la prossima fase.
2. Leggi la sezione di quella fase in `docs/PHASES.md`: obiettivo, prerequisiti, fuori scope, "Fatto quando".
3. Controlla i prerequisiti **prima** di scrivere codice. Se ne manca uno, fermati e chiedi.
4. Esegui **solo** quella fase. Se ti accorgi di qualcosa che appartiene a una fase successiva, annotalo negli aperti e non implementarlo.
5. Chiudi con i punti "Fatto quando" della fase e quelli comuni in fondo a `docs/PHASES.md`.
6. Non iniziare la fase successiva nella stessa sessione, a meno che l'autore non lo chieda esplicitamente.

## Progetto di riferimento
Stack, struttura Gradle, signing, varianti, pipeline di release, README e CHANGELOG ricalcano **ThePatientGamerHelper**, che l'autore mette nel contesto della sessione. Non va cercato online.
Le scelte di allineamento sono in `docs/ALIGNMENT.md` (creato in fase 1). In caso di conflitto vince `SPEC.md`, e la divergenza si annota lì.

## Struttura del repository
```
SPEC.md                  specifica funzionale e tecnica
CLAUDE.md                questo file
docs/PHASES.md           definizione operativa di ogni fase (obiettivo, prerequisiti, fuori scope, "fatto quando")
docs/ALIGNMENT.md        allineamento a ThePatientGamerHelper (fase 1)
seed/                    JSON dei dati di gioco (CC BY-SA 4.0, vedi seed/LICENSE), generati
tools/seedgen/           script Python che genera seed/ da Super Mario Wiki (vedi il suo README)
app/                     app Android (fase 1)
```

## Comandi
```bash
# App
./gradlew assembleDebug       # APK debug: nessuna rete, nessun Python
./gradlew testDebugUnitTest   # test unitari JVM
./gradlew lint                # Android Lint

# Dati di gioco
cd tools/seedgen && python -m pytest                    # test dello script, senza rete
python -m seedgen validate --seed ../../seed            # valida seed/
python -m seedgen release --seed ../../seed --work ../../app/build/seedgen/candidate   # quello che fa Gradle in release
```
Flag della build release: `-PacceptSeedChanges`, `-PofflineSeed`, `-PpythonExec=<path>` (SPEC §5.4).

## Convenzioni
- Lingua di UI e documentazione: italiano. Identificatori nel codice: inglese.
- ID dei dati di gioco: slug stabili generati da seedgen (`mario__touring`, `cup_mushroom`), mai autoincrement.
- Package `com.marcogn.kartlog`, struttura per feature sotto `ui/<feature>/` (`home`, `skin`, `medallions`, `pswitches`, `consigliami`, `settings`, più `theme`, `navigation`, `common`), stesso schema di ThePatientGamerHelper. `data/` e `domain/` si creano dalla fase 3 in poi, quando arrivano le entità Room.
- Navigazione: `Destination` è una `sealed interface` con rotte `@Serializable` (Navigation Compose type-safe), non un grafo con stringhe. Il drawer è `ModalNavigationDrawer` con `drawerState` sollevato a livello di `KartLogNavGraph`; ogni voce del drawer naviga con `popUpTo(Destination.Home){saveState=true}` + `launchSingleTop` + `restoreState`.
- Tema: palette Material 3 custom in `ui/theme/Color.kt`/`Theme.kt` (arancio/verde acqua/magenta), colore dinamico **disattivato** deliberatamente — la palette "da gioco" non deve dipendere dal wallpaper dell'utente. Dettaglio completo dell'allineamento a ThePatientGamerHelper in `docs/ALIGNMENT.md`.

## Stato attuale
<!-- Aggiornare a OGNI fine sessione: fase corrente, cosa è fatto, cosa resta, problemi aperti. -->
- **Fase corrente:** 2, Verifica di seedgen sulle pagine reali (SPEC §8, `docs/PHASES.md`). Completata: `fetch-fixtures` + `pytest` sulle 16 pagine reali, `seed/` aggiornato a `seedVersion 2` con `origin: api` e i `revid` di tutte le pagine, `seed/p_switches.json` creato con le 394 voci. Le fixture reali sono versionate in `tools/seedgen/tests/fixtures/real/` (non più ignorate) perché `test_real_fixtures.py` le richiede per non essere skippato.
- **Fase 1** (Scaffold): completata — struttura Gradle/toolchain/signing/release allineati a ThePatientGamerHelper, Hilt, Navigation Compose con `ModalNavigationDrawer`, Home 2×2 con contatori fittizi, tema Material 3 custom. Schermate Skin/Monete Peach/Pulsanti P/Consigliami/Impostazioni sono ancora placeholder.
- **Già presente prima della fase 1:** `SPEC.md`, `tools/seedgen/` (script, test, configurazione) e `seed/` iniziale (da trascrizione manuale) — erano sotto `docs/`, spostati alla radice nella sessione della fase 1 (nessun contenuto modificato).
- **Secret GitHub:** `RELEASE_KEYSTORE_BASE64`/`RELEASE_KEYSTORE_PASSWORD`/`RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD` configurati durante la fase 1; `RELEASE_PUSH_TOKEN` aggiunto dall'autore. Tutti e 5 presenti, ma la prima release reale (`release.yml`) va fatta solo a fine fasi, come richiesto dall'autore.
- **Aperto:** nessuna entità Room né caricamento seed nell'app, Task Gradle `generateSeed` — tutto rimandato alla fase 3 (schermate Skin/Monete/Pulsanti P/Consigliami restano placeholder fino alle fasi 4-6).

## Decisioni prese
<!-- Una riga per decisione: data, cosa, perché. Aggiungere, non riscrivere. -->
- 2026-09-25 · Fonte dati: Super Mario Wiki (CC BY-SA 4.0), pagine fissate per titolo in `sources.yaml`. Le guide commerciali servono solo a verificare i conteggi.
- 2026-09-25 · Totale outfit 127 (103 alternativi + 24 default), 20 gruppi di cibo: verificato sulla pagina Dash Food e coerente con Nintendo Life.
- 2026-09-25 · Consigliami: criterio primario = outfit ottenibili col miglior **singolo** personaggio. Pari merito 1,1,1,4 raggruppati in una card (SPEC §6.4).
- 2026-09-25 · Cibo per percorso: `ON_COURSE` (stand sul tracciato, da List of Yoshi's locations) vs `NEARBY` (solo Dash Food, include le strade vicine). Consigliami usa solo `ON_COURSE` di default, toggle per includere i dintorni. Gli stand sulle strade sono v2.
- 2026-09-25 · Update 1.8.0: nessun outfit nuovo (confermato dall'autore), solo 2 rally già presenti.
- 2026-09-25 · Pulsanti P da mariowiki (List of Mario Kart World missions), per regione e percorso, 394.
- 2026-09-25 · Peach Medallions: nessuna fonte libera; conteggi per le 10 regioni da Nintendo Life in un file manuale. Mai scraping automatico di IGN, Game8 o Nintendo Life.
- 2026-09-25 · IGN è il riferimento dell'autore per il controllo a campione degli outfit: controllo manuale, non automatizzato.
- 2026-09-25 · Build release: seedgen gira prima degli asset; dati cambiati fermano la build finché non si accettano con `-PacceptSeedChanges`. Debug non usa rete né Python.
- 2026-09-25 · Stato di sblocco dei personaggi non estraibile: default sbloccato, lo gestisce l'utente.
- 2026-09-25 · Nessuna immagine ufficiale per ora: placeholder con iniziali; slot `imageRes` opzionale.
- 2026-09-25 · `SPEC.md`, `seed/`, `tools/seedgen/` erano stati aggiunti sotto `docs/`: spostati alla radice del repository per allinearsi alla struttura descritta in questo file e in `SPEC.md` §5.1. Nessun contenuto modificato, solo posizione.
- 2026-09-25 · Package `com.marcogn.kartlog`, `applicationId` uguale. `versionCode = 1`, `versionName = "0.1.0"` come punto di partenza dello scaffold (il riferimento non documenta una convenzione per la primissima versione, la sua storia parte già da 1.0.0 come prima release pubblica): rivedere quando si taglierà la prima release reale.
- 2026-09-25 · Tema: palette Material 3 custom vivace (arancio/verde acqua/magenta) invece del colore dinamico di sistema, per garantire lo stesso look "da gioco" a tutti (SPEC §2.2). Icona app: monogramma "K" vettoriale originale (nessun asset del riferimento è riusabile, sarebbe comunque il logo di un altro progetto; un vector evita di dover generare un PNG binario).
- 2026-09-25 · Nessuna localizzazione EN in questa fase (a differenza del riferimento): `SPEC.md`/`CLAUDE.md` richiedono solo italiano. `MainActivity` estende `ComponentActivity`, non serve `AppCompatActivity` senza `AppCompatDelegate.setApplicationLocales()`.
- 2026-09-25 · README/CHANGELOG in italiano (il riferimento li ha in inglese): applicata la regola di `CLAUDE.md` "lingua di UI e documentazione: italiano", che vince sulla convenzione del progetto di riferimento. Stessa convenzione di bullet nel CHANGELOG (`- **Sintesi.** dettaglio`) perché `release.yml` la replica identica per estrarre le release notes.
- 2026-09-25 · Dipendenze Gradle allineate a `SPEC.md` §1: rimosse da `libs.versions.toml` le librerie del riferimento non richieste qui (WorkManager, Credential Manager/googleid/play-services-auth, DataStore, AppCompat, Coil) — nessuna feature di KartLog le usa ancora.
- 2026-09-25 · Rimossa la configurazione Dependabot (`.github/dependabot.yml`), a differenza del riferimento: aggiornamento dipendenze a mano per ora.
- 2026-09-25 · Fase 2: aggiunta l'area "Big Donut" (regione `volcanic`) ad `aliases.yaml`, verificata sulla pagina reale delle missioni (sotto l'intestazione "Volcanic biome"). Corretta la regione di `toads_factory`: era `volcanic`, è `central_grassland` — le 10 missioni ambientate lì sono tutte sotto "Central grassland biome" sulla pagina reale, non "Volcanic biome" come assunto in fase 1 da Nintendo Life.
- 2026-09-25 · `tests/fixtures/real/` di seedgen (pagine reali scaricate, ~1.9MB) va **committata**, non ignorata: `test_real_fixtures.py` la richiede per non essere skippata in CI, ed è il "test di accettazione della fase 2" per SPEC §5.3.
- 2026-09-25 · `test_versioned_seed_matches_golden` (tools/seedgen/tests/test_seed.py) esclude ora `p_switches.json` dal confronto con la trascrizione golden, stesso pattern già usato da `test_real_pages_match_golden_transcription`: la trascrizione manuale non ha mai coperto i pulsanti P (arrivano solo con l'estrazione via API), quindi un seed con `p_switches.json` valido non deve far fallire questo test.

## Manutenzione di questo file
- A fine sessione: aggiorna **Stato attuale** e aggiungi a **Decisioni prese** ogni scelta non ovvia fatta durante la sessione.
- Se cambia un comando, una convenzione o la struttura del repository, aggiorna la sezione corrispondente nella stessa sessione.
- Resta sotto le ~150 righe: i dettagli vanno in `SPEC.md` o nei README, qui solo il rimando.

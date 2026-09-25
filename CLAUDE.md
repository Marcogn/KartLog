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

# Task Gradle (SPEC §5.4)
./gradlew generateSeed -PofflineSeed          # valida seed/ senza rete, come farebbe la CI senza accesso a mariowiki.com
./gradlew assembleRelease -PacceptSeedChanges # accetta i dati cambiati e aggiorna da solo il CHANGELOG
```
Flag della build release: `-PacceptSeedChanges`, `-PofflineSeed`, `-PpythonExec=<path>` (SPEC §5.4).

## Convenzioni
- Lingua di UI e documentazione: italiano. Identificatori nel codice: inglese.
- ID dei dati di gioco: slug stabili generati da seedgen (`mario__touring`, `cup_mushroom`), mai autoincrement.
- Package `com.marcogn.kartlog`, struttura per feature sotto `ui/<feature>/` (`home`, `skin`, `medallions`, `pswitches`, `consigliami`, `settings`, più `theme`, `navigation`, `common`), stesso schema di ThePatientGamerHelper. `data/` e `domain/` si creano dalla fase 3 in poi, quando arrivano le entità Room.
- Navigazione: `Destination` è una `sealed interface` con rotte `@Serializable` (Navigation Compose type-safe), non un grafo con stringhe. Il drawer è `ModalNavigationDrawer` con `drawerState` sollevato a livello di `KartLogNavGraph`; ogni voce del drawer naviga con `popUpTo(Destination.Home){saveState=true}` + `launchSingleTop` + `restoreState`, **tranne Home stesso** (`popUpTo<Destination.Home>{inclusive=true}` + `launchSingleTop`, senza `restoreState`: vedi Decisioni prese). Transizioni 300ms slide+fade su `NavHost` e guardia `lifecycleIsResumed()` su ogni `navigate()`/`popBackStack()`, allineate a ThePatientGamerHelper.
- Tema: palette Material 3 custom in `ui/theme/Color.kt`/`Theme.kt` (arancio/verde acqua/magenta), colore dinamico **disattivato** deliberatamente — la palette "da gioco" non deve dipendere dal wallpaper dell'utente. Dettaglio completo dell'allineamento a ThePatientGamerHelper in `docs/ALIGNMENT.md`.
- Dati: `data/local/entity` (entità Room, SPEC §3) è la fonte di verità; `data/seed` ha i DTO kotlinx.serialization sui JSON reali e `SeedRepository`, che fa il reseed confrontando `seedVersion` senza mai toccare le tabelle di stato utente. Mai leggere `seed/` direttamente a runtime: solo l'asset `assets/seed/*.json` copiato da `copySeedAssets` (debug e release).
- **Niente immagini di personaggi prese da wiki/fandom Nintendo**, nemmeno se richiesto esplicitamente: sono artwork Nintendo senza licenza di ridistribuzione, a prescindere da dove sono ospitate pubblicamente. Placeholder con iniziali (SPEC §0.2) finché non si trova un'alternativa originale.
- Skin: `SkinDao` incrocia dati seed e stato utente con query SQL dirette (JOIN + GROUP_CONCAT), non con più chiamate separate — un solo `Flow` per schermata. L'outfit di default è sempre in `owned_outfits` (`ensureDefaultOutfitsOwned()`, chiamata da `SeedRepository` a ogni avvio): il contatore "ottenuti/totali" non ha bisogno di trattarlo come caso speciale in UI, solo di non mostrargli la checkbox.
- Monete Peach/Pulsanti P: stesso pattern di `SkinDao` (`MedallionsDao`/`PSwitchesDao`, una query con JOIN per schermata) e stesso componente condiviso `ui/common/RegionSection.kt` (intestazione regione + dialog di conferma "segna tutti") per le due schermate, che sono strutturalmente identiche (10 regioni collassabili).
- Consigliami: l'algoritmo (SPEC §6) vive in `domain/consigliami/` come Kotlin puro (`ConsigliamiUseCase`, nessuna dipendenza Android/Room), testato con dati sintetici in `ConsigliamiUseCaseTest`. `ConsigliamiDao` espone solo `Flow` sulle tabelle grezze; `ConsigliamiViewModel` fa da ponte, ricombinando i `Flow` e rieseguendo l'algoritmo a ogni cambiamento — mai logica di dominio nel ViewModel o nel DAO.
- Risultati: `bestStars(E, cc)` si calcola nel ViewModel (`ConsigliamiViewModel`) da `UserStateDao.allRaceResults()`, mai in una query SQL — il "miglior risultato" per una card è per stelle decrescenti poi posizione crescente, una decisione di prodotto non un dettaglio di persistenza.
- Backup: `data/backup/` (non `domain/`) perché è solo mappatura dati↔JSON, nessun algoritmo puro da isolare. Un import **sostituisce** tutto lo stato utente (mai un merge) e valida ogni ID contro il seed corrente prima di scrivere, riportando gli sconosciuti invece di scartarli.
- Consigliami, form di registrazione: `RaceResultBottomSheet` non è più legato a un evento fisso, elenca tutti gli eventi (`ConsigliamiViewModel`/`ConsigliamiDetailViewModel` espongono `allEvents: List<EventPickerItem>` da `dao.events()`, **non filtrata** da eventFilter/"Solo utili"). Punto di ingresso globale nella TopAppBar di `ConsigliamiScreen`, oltre al FAB per-evento nel dettaglio.

## Stato attuale
<!-- Aggiornare a OGNI fine sessione: fase corrente, cosa è fatto, cosa resta, problemi aperti. -->
- **Roadmap `SPEC.md` §8 completo, release 0.1.1 pubblicata dall'autore.** Dopo la 0.1.1 l'autore ha provato l'app su dispositivo reale e segnalato tre problemi, tutti corretti in questa sessione (branch/PR separati, non ancora mergiati a fine sessione):
  1. **Navigazione**: "Home" dal drawer non portava mai a Home (solo il tasto Indietro), transizioni percepite lente. Corretto: transizioni 300ms + guardia `lifecycleIsResumed()` (allineate a ThePatientGamerHelper) e Home non usa più `restoreState` (vedi Decisioni prese).
  2. **Consigliami, buco di analisi**: nessun modo di registrare un risultato per un evento a punteggio 0 (nascosto di default da "Solo utili"). Corretto: form con selettore di evento + punto di ingresso globale in Consigliami.
  3. **Nomi ufficiali in italiano** (outfit/pulsanti P/biomi, fonte indicata dall'autore: `mariowiki.com/List_of_Mario_Kart_World_outfit_names_in_other_languages`): **non ancora iniziato**, richiede prima di capire quali pagine mariowiki coprano quali categorie — da affrontare in una sessione dedicata.
- **Secret GitHub:** tutti e 5 presenti. `versionCode`/`versionName` fermi a `1`/`0.1.0` (punto di partenza dello scaffold, fase 1): da rivedere alla prossima release reale.
- **Aperto:** il percorso di rete reale di `generateSeed` non è stato eseguito end-to-end in nessuna sessione (l'ambiente di sviluppo non ha `python3-venv`/`ensurepip` di sistema, niente sudo) — solo verificato con un eseguibile finto al posto di seedgen. Nessun emulatore/dispositivo Android disponibile in questo ambiente: le correzioni di questa sessione sono verificate solo con build/lint/test JVM (Robolectric), non visivamente — il bug di navigazione era stato segnalato dall'autore proprio su un dispositivo reale, quindi vale la pena che lo riconfermi lui prima del merge. Immagini dei personaggi: ancora nessuna alternativa originale ai render Nintendo (placeholder con iniziali su tutte le schermate). Nomi ufficiali in italiano: vedi punto 3 sopra.

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
- 2026-09-25 · Rifiutato di scaricare icone dei personaggi da mario.fandom.com su richiesta esplicita dell'autore, che aveva chiesto di ignorare la regola "niente asset Nintendo" per questo caso specifico: le immagini restano artwork Nintendo senza licenza di ridistribuzione (pubblicamente visibili ≠ ridistribuibili), esattamente il rischio che il progetto evita già omettendo "Mario Kart" dal nome. Un disclaimer nel README non risolve il problema legale. Placeholder con iniziali confermato anche per la fase 4.
- 2026-09-25 · `SeedFile<T>.source` rimosso dal DTO: nei JSON è a volte una stringa (un solo URL) a volte un array (`events.json`, che viene da più pagine), e l'app non lo usa comunque (solo `meta.json.sources` servirà alla schermata Info, fase successiva). `ignoreUnknownKeys` lo salta senza bisogno di un serializer polimorfico.
- 2026-09-25 · Robolectric pinnato a `sdk=34` (`app/src/test/resources/robolectric.properties`): le shadow per l'SDK 36 (compileSdk) richiedono JDK 21, mentre tutto il resto del progetto compila con JDK 17 (SPEC §1) — stesso vincolo di ThePatientGamerHelper.
- 2026-09-25 · `expected_counts.yaml` letto lato Kotlin con un parser di poche righe (solo `chiave: intero`) invece di aggiungere una dipendenza YAML: nessun'altra parte dello stack Android ne ha bisogno.
- 2026-09-25 · Fase 4: l'outfit di default è reso "sempre posseduto" inserendo davvero una riga in `owned_outfits` per ciascuno (`ensureDefaultOutfitsOwned()`, chiamata a ogni avvio da `SeedRepository`), non con un caso speciale sparso nelle query/UI. Effetto collaterale voluto: il contatore Home passa da `0/127` (fine fase 3) a `24/127` non appena questa logica esiste, il che non contraddice il criterio "fatto quando" della fase 3 (già chiuso a suo tempo).
- 2026-09-25 · "Chi ha tutti gli outfit va in fondo" (SPEC §2.3) si applica **sempre**, qualunque ordinamento scelto (roster/alfabetico/%completamento): prima si separano completi/incompleti, poi si ordina ciascun gruppo, mai il contrario.
- 2026-09-25 · Nessun emulatore/dispositivo Android disponibile in questo ambiente: il criterio "Test UI del flusso spunta outfit → contatore aggiornato" (fase 4, fatto quando) è coperto da un test Robolectric/JVM sulle stesse query (`SkinDaoTest`), non da un vero test strumentato Compose. Stesso limite già segnalato per `generateSeed` in fase 3.
- 2026-09-25 · Fase 5: "segna tutti" implementato solo come marca-tutti-mancanti (`INSERT OR IGNORE`), nessuna azione simmetrica di "smarca tutti" — non richiesta da SPEC §2.4, e comunque reversibile togliendo i singoli check.
- 2026-09-25 · Fase 5: il contatore globale dei Pulsanti P non cambia mentre si usa la ricerca (conta sempre sul totale non filtrato) — solo la lista visibile si restringe. Le sezioni si espandono automaticamente durante la ricerca per mostrare subito i risultati.
- 2026-09-25 · `SeedAssetLoader.readSourceUrl()` (per "Apri guida", SPEC §2.4) legge il campo `source` di un singolo file al bisogno con un DTO dedicato (`SourceOnlyDto`), invece di riportarlo nel `SeedFile<T>` condiviso: è quello già tolto in fase 3 perché non sempre una stringa (`events.json`).
- 2026-09-25 · Fase 6: "Pesa i risultati" (switch + slider del peso, SPEC §2.5) **non implementato** in questa fase — non solo il peso `w`, tutta la sezione: senza `RaceResult` (fase 7) sarebbe un controllo che non fa nulla. `score(E)` usa sempre la formula "risultati disattivati" (§6.3); `worstFirst` non entra mai negli spareggi, come previsto da §6.4 in quel caso.
- 2026-09-25 · Fase 6: il filtro "Solo utili" (default on, §6.3) è applicato nel `ViewModel`, non nell'algoritmo puro: `ConsigliamiUseCase` restituisce sempre la classifica completa (compresi gli eventi a gain 0, già in fondo per costruzione), la UI nasconde quelli a punteggio 0 quando il filtro è attivo.
- 2026-09-25 · Fase 6: il toggle Gran Premi/Knockout Tour/Entrambi filtra gli eventi **prima** di ricalcolare l'algoritmo (non dopo): con solo "Gran Premi" selezionato, posizioni e pari merito si ricalcolano esclusivamente tra i Gran Premi, non tra tutti gli eventi con i Rally nascosti in UI.
- 2026-09-25 · Pulsante "Registra risultato" (SPEC §2.5, dettaglio evento) **non aggiunto**: punterebbe a una funzionalità che non esiste ancora (fase 7); un bottone che non fa nulla è peggio di nessun bottone.
- 2026-09-25 · Fase 7: l'eliminazione (checkpoint N invece di posizione) è selezionabile solo per i Knockout Tour, mai per i Gran Premi — SPEC §2.6 la prevede solo per KO ("GP: 1–24; KO: 1–24 oppure eliminato al checkpoint N"), un GP corre sempre un giro intero.
- 2026-09-25 · Fase 7: "miglior risultato" per una card/evento = stelle decrescenti, poi posizione crescente (non l'ultimo registrato né una media) — coerente con SPEC §6.3 che usa `bestStars`, non una media.
- 2026-09-25 · Fase 8: l'import **sostituisce** interamente lo stato utente (come il restore di ThePatientGamerHelper), non lo unisce a quello presente — "l'import ripristina tutto" (fatto quando) si legge come ripristino completo, non merge. Gli outfit di default rientrano da soli al prossimo avvio se un backup vecchio non li includeva (`ensureDefaultOutfitsOwned()` è già idempotente).
- 2026-09-25 · Fase 8: l'attribuzione dati (SPEC §5.5) aggiunta nella stessa schermata Impostazioni/Info insieme al backup — nessuna fase in `docs/PHASES.md` la nominava esplicitamente, ma condivide la voce di drawer (SPEC §2.1) ed era l'ultima occasione per completarla prima della fine del roadmap.
- 2026-09-25 · Fase 8, ultima del roadmap: **nessuna release tagliata**. L'autore ha chiesto esplicitamente di arrivare fino alla fine e occuparsi lui della release e della verifica finale.
- 2026-09-25 · Post-0.1.1, bug navigazione: `navigate(Destination.Home){ popUpTo(Destination.Home){saveState=true}; restoreState=true }` non ripristinava in modo affidabile Home quando il target della navigazione coincide col target del `popUpTo` e Home non è più in cima allo stack — caso limite di Navigation Compose. Home ora fa `popUpTo<Destination.Home>{inclusive=true}` + `launchSingleTop`, senza `saveState`/`restoreState`: perde lo stato di scroll di Home, accettabile perché Home non ha nulla da preservare (griglia 2×2 con contatori sempre live).
- 2026-09-25 · Post-0.1.1, Consigliami: il form di registrazione ora accetta **qualsiasi** evento (selettore aggiunto a `RaceResultBottomSheet`), non solo quello del dettaglio da cui si apre — altrimenti gli eventi a punteggio 0, nascosti di default da "Solo utili", non erano mai raggiungibili per registrare un risultato. `EventPickerItem` (id/nome/tipo) è il tipo condiviso tra `ConsigliamiViewModel` e `ConsigliamiDetailViewModel`.
- 2026-09-25 · Nomi ufficiali in italiano (outfit/pulsanti P/biomi ecc., richiesti dall'autore dopo la 0.1.1): **non affrontato in questa sessione**. Tocca la regola "nessun dato inventato" (va aggiunto via `tools/seedgen`, non a mano in Kotlin) e richiede prima di identificare quali pagine di mariowiki.com coprano quali categorie — la fonte per gli outfit è nota (`List_of_Mario_Kart_World_outfit_names_in_other_languages`), le altre no. Da fare in una sessione dedicata, non insieme ai fix di navigazione/Consigliami.

## Manutenzione di questo file
- A fine sessione: aggiorna **Stato attuale** e aggiungi a **Decisioni prese** ogni scelta non ovvia fatta durante la sessione.
- Se cambia un comando, una convenzione o la struttura del repository, aggiorna la sezione corrispondente nella stessa sessione.
- Resta sotto le ~150 righe: i dettagli vanno in `SPEC.md` o nei README, qui solo il rimando.

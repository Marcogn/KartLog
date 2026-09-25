# Changelog

Tutte le modifiche rilevanti a KartLog sono documentate in questo file.
Il formato ricalca liberamente [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
il versionamento segue il `versionName` dell'app in `app/build.gradle.kts`.

## [Unreleased]

- **Nomi ufficiali in italiano per personaggi, outfit e corsi.** I nomi
  di gioco erano tutti in inglese anche con l'app in italiano. Aggiunto
  `seedgen/i18n.py`: estrae i nomi ufficiali dalle tabelle "Names in
  other languages" di Super Mario Wiki (mai una traduzione automatica),
  con le varianti maschile/femminile italiane degli outfit dove esistono
  (es. "Esploratore"/"Esploratrice"). Copertura: tutti i 24 personaggi,
  30 corsi e 103 outfit alternativi. **Non coperti** (nessuna fonte
  ufficiale verificabile): missioni dei pulsanti P, regioni/biomi
  (nemmeno i nomi inglesi sono ufficiali per queste), Gran Premi/Knockout
  Tour — restano in inglese, invece di mostrare una traduzione
  inventata.
- **App localizzata anche in inglese.** `values-en/strings.xml` accanto
  all'italiano (resta la lingua di default), più `locales_config.xml`
  per il selettore lingua di sistema (Impostazioni > App > KartLog >
  Lingua, Android 13+). Un nome di gioco senza traduzione italiana
  ufficiale resta in inglese anche con l'app in italiano — mai una
  traduzione automatica.
- **Nuova icona dell'app.** Il monogramma "K" provvisorio è sostituito
  da un'illustrazione originale (globo con anello e lettera "L"), adattata
  alla forma delle adaptive icon Android: cielo esteso attorno al disegno
  in modo che l'anello resti visibile con qualsiasi maschera del launcher.
- **Corretta la navigazione dal drawer verso Home.** "Home" nel menu
  laterale non portava mai alla schermata Home (funzionava solo il tasto
  Indietro di sistema): il `popUpTo(Destination.Home)` combinato con la
  navigazione verso lo stesso Home non ripristinava in modo affidabile lo
  stato salvato. Home ora si raggiunge sempre con un pop completo dello
  stack e una nuova istanza, senza dipendere da `restoreState`.
- **Transizioni di navigazione più veloci.** Sostituito il crossfade di
  default di Navigation Compose (~700ms) con transizioni scorrevoli da
  300ms allineate a
  [ThePatientGamerHelper](https://github.com/Marcogn/ThePatientGamerHelper),
  e aggiunta una guardia (`lifecycleIsResumed()`) su ogni `navigate()`/
  `popBackStack()` per evitare che un tap durante una transizione atterri
  sulla schermata sbagliata.
- **Consigliami: il form di registrazione ora elenca tutti gli eventi.**
  Il form era raggiungibile solo dal dettaglio di un evento mostrato
  dalla lista, che di default nasconde con "Solo utili" gli eventi a
  punteggio 0 — rendendo impossibile registrare un risultato per quegli
  eventi. Aggiunto un selettore di evento al form e un punto di ingresso
  globale ("Registra risultato" nella barra in alto di Consigliami), così
  ogni Gran Premio/Knockout Tour è sempre registrabile.

## [0.1.1] - 2026-09-25

- **Scaffold iniziale del progetto (SPEC §8, fase 1).** Struttura Gradle,
  toolchain e schema di signing/release allineati a
  [ThePatientGamerHelper](https://github.com/Marcogn/ThePatientGamerHelper)
  (dettaglio in `docs/ALIGNMENT.md`): Kotlin, Jetpack Compose, Material 3,
  Navigation Compose con rotte type-safe, Hilt, dipendenze Room e
  kotlinx.serialization pronte per la fase 3. `ModalNavigationDrawer` con
  le voci Home/Skin/Monete Peach/Pulsanti P/Consigliami/Impostazioni,
  ciascuna una schermata placeholder. Home con griglia 2×2 di pulsanti
  quadrati e contatori fittizi. Tema Material 3 con palette custom
  vivace (arancio/verde acqua/magenta) e angoli arrotondati generosi.
  Nessun permesso INTERNET nel manifest, nessun asset Nintendo.
  `README.md`, `CHANGELOG.md`, `.gitignore`, workflow GitHub Actions
  (`android-ci`, `build-apk`, `release`) e `SECURITY.md` ricalcano gli
  equivalenti del progetto di riferimento.
- `SPEC.md`, `seed/` e `tools/seedgen/` spostati alla radice del
  repository (erano sotto `docs/`), per allinearsi alla struttura
  descritta in `CLAUDE.md` e in `SPEC.md` §5.1 — nessun contenuto
  modificato.
- Rimossa la configurazione Dependabot (`.github/dependabot.yml`): a
  differenza del progetto di riferimento, per ora si preferisce
  aggiornare le dipendenze a mano, senza PR automatiche settimanali.
- **Dati di gioco aggiornati da Super Mario Wiki (seedVersion 2; Dash
  Food@5455370, List of Yoshi's locations@5263331, List of Mario Kart
  World missions@5472356, Template:Mario Kart World@5478848, Golden
  Rally@5489159, Ice Rally@5479135, Moon Rally@5479138, Spiny
  Rally@5479139, Cherry Rally@5479140, Acorn Rally@5479142, Cloud
  Rally@5479143, Heart Rally@5479144, Drill Rally@5479145, Boomerang
  Rally (Mario Kart World)@5479146, Propeller Rally@5492298, Turnip
  Rally@5488553).** Fase 2 (SPEC §8): prima estrazione reale via API,
  `seed/meta.json.origin` passa da `manual-transcription` ad `api`.
  Aggiunti i 394 Pulsanti P (`seed/p_switches.json`, assente nel seed
  iniziale). Corretti due problemi trovati confrontando l'estrazione
  con la trascrizione manuale: area mancante "Big Donut" (regione
  volcanica) e regione errata di Toad's Factory (era `volcanic`, è
  `central_grassland` — le missioni lì ambientate sono tutte sotto
  "Central grassland biome" sulla pagina reale). Nessun'altra
  differenza: Dash Food, Yoshi's locations, navbox e rally combaciano
  esattamente con la trascrizione manuale iniziale.
- **L'app carica i dati reali da `seed/` (SPEC §8, fase 3).** Entità Room
  definitive (SPEC §3, sia i dati di gioco sia lo stato utente), modelli
  kotlinx.serialization sui JSON reali, `SeedRepository` che fa il
  reseed a ogni cambio di `seedVersion` senza mai toccare lo stato
  utente (test di migrazione incluso). I contatori della Home sono ora
  collegati ai dati reali tramite `HomeViewModel` (mostrano `0 / 127`,
  `0 / 200`, `0 / 394` a stato utente vuoto, "n/d" se una sorgente
  manca). Test di validazione lato app sugli asset effettivamente
  impacchettati (SPEC §5.6), con i conteggi letti da
  `tools/seedgen/expected_counts.yaml`.
- **Task Gradle `generateSeed` (SPEC §5.4).** Il build `release` esegue
  seedgen (in una venv sotto `build/`, mai nel sistema) prima di
  impacchettare gli asset: dati identici a `seed/` prosegue, dati
  cambiati ferma la build mostrando il diff (`-PacceptSeedChanges` per
  accettarli e aggiungere automaticamente la riga al changelog), dati
  non validi o rete/configurazione assenti fermano sempre la build
  (`-PofflineSeed` per validare solo `seed/` versionato senza rete). Il
  build `debug` resta completamente offline: copia solo i JSON già
  versionati (`copySeedAssets`).
- **Schermata Skin funzionante (SPEC §2.3, fase 8, fase 4).** Griglia a 2
  colonne dei 24 personaggi con contatore `ottenuti/totali`; chi ha
  tutti gli outfit si attenua e va in fondo alla griglia, con filtro
  Tutti/Incompleti e ordinamento roster/alfabetico/%completamento.
  Dettaglio personaggio: switch "Sbloccato" (default acceso), lista
  outfit con checkbox e i gruppi di cibo che li sbloccano ("cibo
  sconosciuto" se non ce ne sono), outfit di default sempre mostrato
  come posseduto e non spuntabile. Spuntare un outfit aggiorna subito
  sia il contatore del personaggio sia quello della Home (stesse righe
  in `owned_outfits`, SPEC §9).
- **Schermate Monete Peach e Pulsanti P (SPEC §2.4, fase 5).** Entrambe
  organizzate per le 10 regioni, in sezioni collassabili con contatore
  `x/y` e azione "segna tutti" (con conferma). Monete Peach: checkbox
  numerate "Medaglione N" e pulsante "Apri guida" che apre nel browser
  la fonte di `peach_medallions.json` (`Intent.ACTION_VIEW`, nessun
  permesso INTERNET). Pulsanti P: sottogruppi per percorso/luogo,
  ricerca testuale sul nome della missione (il contatore globale non
  cambia durante la ricerca), messaggio "Dati non ancora disponibili"
  se `p_switches.json` manca dal seed.
- **Consigliami (SPEC §2.5/§6, fase 6), senza registrazione risultati
  (fase 7).** `ConsigliamiUseCase`: algoritmo puro (nessuna dipendenza
  Android) con gain per personaggio, punteggio, pari merito (numerazione
  "competition ranking") e spareggi — tutti i test di SPEC §6.5 scritti
  prima della UI. Lista con toggle Gran Premi/Knockout Tour/Entrambi,
  switch "Includi cibi nei dintorni" e "Solo utili" (default on),
  gruppi a pari merito espandibili con il corso in comune nel titolo
  quando presente. Ogni card mostra personaggio consigliato, le 2
  alternative successive e i cibi rilevanti con l'indicazione sul
  percorso/nei dintorni. Il dettaglio evento elenca tutti i personaggi
  con gain > 0 e gli outfit specifici ottenibili. Si aggiorna in tempo
  reale quando cambia lo stato di outfit o sblocchi (SPEC §9).
  Peso dei risultati e pulsante "Registra risultato": fase 7.
- **Registrazione risultati e peso in Consigliami (SPEC §2.6/§6.3, fase
  7).** Bottom sheet di registrazione dal dettaglio evento: cilindrata,
  stelle 0-3, posizione finale (1-24) o, per i Knockout Tour, eliminato
  al checkpoint N, personaggio usato opzionale, data automatica.
  Storico risultati per evento con cancellazione. Sezione "Pesa i
  risultati" in Consigliami: switch, slider del peso `w` e cilindrata
  di riferimento (default 150cc) — `score(E) = (1-w)·normGain(E) +
  w·improvement(E)` quando attiva, `worstFirst` entra negli spareggi
  solo in quel caso. Le card mostrano il miglior risultato registrato
  (stelle e posizione) quando i risultati sono pesati. Il test "con
  w=1 l'ordinamento dipende solo da improvement" (SPEC §6.5, rimasto
  aperto dalla fase 6) ora passa anche sui dati reali dell'app, non
  solo sullo UseCase.
- **Backup dello stato utente (SPEC §4, fase 8 — ultima fase del
  roadmap).** Export/import JSON tramite Storage Access Framework,
  dalla schermata Impostazioni: mai i dati di gioco, solo outfit
  posseduti, sblocchi personaggio, monete e pulsanti P raccolti,
  risultati registrati. Formato versionato (`backupVersion`).
  L'import valida ogni ID contro il seed corrente e **riporta** quelli
  sconosciuti invece di scartarli in silenzio; sostituisce interamente
  lo stato utente (un backup è un ripristino completo, non un merge).
  La stessa schermata mostra anche l'attribuzione dei dati di gioco
  (CC BY-SA 4.0, Super Mario Wiki) con i `revid` delle pagine usate
  (SPEC §5.5), letti da `meta.json`.

Con questa fase il roadmap di `SPEC.md` §8 è completo (fasi 1-8).

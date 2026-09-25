# Changelog

Tutte le modifiche rilevanti a KartLog sono documentate in questo file.
Il formato ricalca liberamente [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
il versionamento segue il `versionName` dell'app in `app/build.gradle.kts`.

## [Unreleased]

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

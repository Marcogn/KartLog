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

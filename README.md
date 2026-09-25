# KartLog

[![Latest release](https://img.shields.io/github/v/release/Marcogn/KartLog?label=release)](https://github.com/Marcogn/KartLog/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![minSdk 26](https://img.shields.io/badge/minSdk-26-brightgreen.svg)](app/build.gradle.kts)

Tracker Android **offline** per i collectibles di Mario Kart World: skin
(outfit) dei personaggi, Monete Peach e Pulsanti P, con un modulo
"Consigliami" che suggerisce quale Gran Premio o Knockout Tour correre — e
con quale personaggio — per sbloccare più outfit mancanti.

> Nome di lavoro "KartLog": nessun asset, nome o logo Nintendo compare nel
> repository, nel package o nell'icona (vedi [`SPEC.md`](SPEC.md) §0).

## Perché

Gli outfit di Mario Kart World si sbloccano dando da mangiare ai
personaggi cibi trovati sui percorsi, in combinazioni che il gioco non
spiega mai in un unico posto: KartLog le raccoglie da Super Mario Wiki e
le trasforma in un tracker con contatori di completamento, checklist per
Monete Peach e Pulsanti P, e un suggerimento su quale evento correre (e
con chi) per avvicinarsi il più possibile alla collezione completa.

## Stato del progetto

Il roadmap di [`SPEC.md`](SPEC.md) §8 (vedi anche
[`docs/PHASES.md`](docs/PHASES.md) per il dettaglio di ogni fase) è
**completo**, fase 1-8. Segui [`CHANGELOG.md`](CHANGELOG.md) per la
cronologia di cosa è arrivato in ciascuna fase.

## Funzionalità

Dalla specifica completa ([`SPEC.md`](SPEC.md)):

- **Skin**: griglia dei personaggi con contatore ottenuti/totali; per
  ciascuno, checklist degli outfit con i gruppi di cibo che li
  sbloccano e uno switch per lo stato di sblocco del personaggio.
- **Monete Peach e Pulsanti P**: checklist per regione, con ricerca,
  "segna tutti" e contatore globale.
- **Consigliami**: classifica dei Gran Premi/Knockout Tour da correre in
  base agli outfit ancora mancanti, con personaggio consigliato, le due
  alternative successive e i cibi rilevanti sul percorso (o nei
  dintorni, se attivato).
- **Registrazione risultati**: stelle e piazzamento per evento, storico
  consultabile, peso opzionale nel punteggio di Consigliami.
- **Backup**: export/import JSON dello stato utente (non dei dati di
  gioco) tramite Storage Access Framework, dalla schermata Impostazioni.

## Dati di gioco

Tutti i dati di gioco (personaggi, outfit, cibi, percorsi, eventi,
Monete Peach, Pulsanti P) vengono da **Super Mario Wiki** (mariowiki.com),
contenuti testuali in licenza **CC BY-SA 4.0**, estratti dallo script in
[`tools/seedgen/`](tools/seedgen/README.md) e versionati in
[`seed/`](seed/README.md). Nessun dato è inventato: dettagli su fonti,
regole di estrazione e attribuzione in [`SPEC.md`](SPEC.md) §5.

Le immagini di mariowiki (asset Nintendo) non sono mai scaricate: i
personaggi in app usano un placeholder con le iniziali finché non si
decide diversamente.

## Come compilare

```bash
./gradlew assembleDebug       # APK debug: nessuna rete, nessun Python
./gradlew testDebugUnitTest   # test unitari JVM
./gradlew lint                # Android Lint
```

Il build **debug** usa solo i JSON già versionati in `seed/`. Il build
**release** esegue in più `tools/seedgen` per rigenerare e validare i dati
dal wiki (vedi `SPEC.md` §5.4) — richiede Python 3 e, salvo `-PofflineSeed`,
accesso di rete a mariowiki.com.

Richiede l'Android SDK (`compileSdk 36`) e accesso di rete al repository
Maven di Google.

## Privacy

KartLog è completamente offline: nessun permesso INTERNET nel manifest,
nessuna analytics, nessun account. L'unico uso della rete nell'intero
progetto è a **build time**, nello script `tools/seedgen` che rigenera i
dati di gioco dal wiki — mai a runtime sul dispositivo.

## Stack tecnico

Kotlin e Jetpack Compose con Material 3, stessa architettura di
riferimento di [ThePatientGamerHelper](https://github.com/Marcogn/ThePatientGamerHelper)
(vedi [`docs/ALIGNMENT.md`](docs/ALIGNMENT.md) per il dettaglio
dell'allineamento):

- **Room** come fonte di verità per i dati seed e lo stato utente
- **Hilt** per la dependency injection
- **Navigation Compose** con rotte type-safe (`kotlinx.serialization`)
- **ViewModel + StateFlow**, flusso dati unidirezionale
- `minSdk 26`, `targetSdk 36`, `compileSdk 36`

## Struttura del progetto

```
SPEC.md                  specifica funzionale e tecnica completa
CLAUDE.md                guida operativa per chi (o cosa) sviluppa il progetto
docs/ALIGNMENT.md        allineamento a ThePatientGamerHelper (fase 1)
seed/                    JSON dei dati di gioco (CC BY-SA 4.0), generati
tools/seedgen/           script Python che genera seed/ da Super Mario Wiki
app/src/main/java/com/marcogn/kartlog/
├── ui/                  schermate Compose (home, skin, medaglioni, pulsanti P,
│                        consigliami, impostazioni) + tema + navigazione
├── data/                Room (entity/dao), DTO e repository del seed (fase 3)
└── domain/              modelli puri e l'algoritmo di Consigliami (fase 6), nessuna dipendenza Android
```

## Documentazione

- [`CHANGELOG.md`](CHANGELOG.md) — cosa è cambiato in ogni release
- [`SPEC.md`](SPEC.md) — specifica funzionale e tecnica, con la roadmap
- [`docs/ALIGNMENT.md`](docs/ALIGNMENT.md) — allineamento a
  ThePatientGamerHelper (Gradle, signing, release, convenzioni)
- [`CLAUDE.md`](CLAUDE.md) — regole operative, comandi, stato e decisioni
  per chi sviluppa il progetto
- [`tools/seedgen/README.md`](tools/seedgen/README.md) — uso dello script
  di estrazione dati
- [`seed/README.md`](seed/README.md) — formato dei file seed

## Contributing

Progetto personale a uso singolo utente: nessuna roadmap pubblica aperta a
proposte. Segnalazioni di bug e piccole PR di fix mirati sono benvenute —
apri una issue prima di lavorare su qualcosa di sostanziale. Per
segnalare una vulnerabilità di sicurezza vedi [`SECURITY.md`](SECURITY.md)
invece di aprire una issue pubblica.

## About this project

KartLog è un progetto indipendente sviluppato da marcogn in stretta
collaborazione con Claude, l'assistente di programmazione AI di
Anthropic — Claude scrive e revisiona gran parte del codice, sotto
supervisione e design umano diretti a ogni passo.

## License

MIT, vedi [`LICENSE`](LICENSE). I dati in `seed/` restano sotto la
licenza CC BY-SA 4.0 di Super Mario Wiki (vedi [`seed/LICENSE`](seed/LICENSE)),
separata dalla licenza del codice.

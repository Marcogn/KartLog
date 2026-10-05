# KartLog

[![Latest release](https://img.shields.io/github/v/release/Marcogn/KartLog?label=release)](https://github.com/Marcogn/KartLog/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![minSdk 26](https://img.shields.io/badge/minSdk-26-brightgreen.svg)](app/build.gradle.kts)

Tracker Android per i collectibles di Mario Kart World: skin
(outfit) dei personaggi, Monete Peach e Pulsanti P, con un modulo
"Consigliami" che suggerisce quale Gran Premio o Knockout Tour correre — e
con quale personaggio — per sbloccare più outfit mancanti.

> Nome di lavoro "KartLog": nessun asset, nome o logo Nintendo compare nel
> repository, nel package o nell'icona (vedi [`SPEC.md`](SPEC.md) §0). Le immagini di
> gioco si scaricano a runtime, vedi [Immagini](#immagini).

## Perché

Gli outfit di Mario Kart World si sbloccano dando da mangiare ai
personaggi cibi trovati sui percorsi, in combinazioni che il gioco non
spiega mai in un unico posto: KartLog le raccoglie da Super Mario Wiki e
le trasforma in un tracker con contatori di completamento, checklist per
Monete Peach e Pulsanti P, e un suggerimento su quale evento correre (e
con chi) per avvicinarsi il più possibile alla collezione completa.

## Stato del progetto

L'app è usabile e pubblicata (vedi [`CHANGELOG.md`](CHANGELOG.md)). Il
lavoro in corso è in [`docs/roadmap.md`](docs/roadmap.md): revisione di
Consigliami e una nuova sezione Cibi.

## Funzionalità

Dalla specifica completa ([`SPEC.md`](SPEC.md)):

- **Personaggi**: tutti i 50 piloti, con contatore ottenuti/totali degli
  outfit per i 24 che ne hanno; per ciascuno, checklist degli outfit
  con i gruppi di cibo che li sbloccano e uno switch per lo stato di
  sblocco. I piloti senza outfit si segnano come sbloccati con un tap.
- **Monete Peach**: le 200 monete una per una, con le istruzioni per
  trovarle e il tasto per vederle sulla mappa.
- **Pulsanti P**: checklist per bioma e percorso, con ricerca e
  "segna tutti".
- **Pannelli "?"**: i 150 pannelli uno per uno, come le Monete Peach.
  Nel menu laterale le tre liste stanno sotto "Collezionabili".
- **Mappa**: Monete Peach, Pulsanti P e pannelli "?" sulla mappa del
  mondo, con zoom, filtri per tipo e per "fatti", istruzioni e video.
  Si apre dal menu o dal pulsantone rosso di Monete Peach e Pulsanti P.
- **Consigliami**: classifica dei Gran Premi/Knockout Tour da correre in
  base agli outfit ancora mancanti, con personaggio consigliato, le due
  alternative successive e i cibi che potrebbero esserci: gli stand
  Yoshi's sono nell'area dei percorsi, non per forza sul tracciato di
  gara, quindi ogni consiglio è una possibilità ("occhi aperti").
- **Risultati**: schermata dedicata, raggiungibile dalla Home, dove si
  registra il miglior trofeo (bronzo … oro ★★★) di ogni Gran Premio e
  Knockout Tour per cilindrata; peso opzionale nel punteggio di
  Consigliami.
- **Backup**: export/import JSON dello stato utente (non dei dati di
  gioco) tramite Storage Access Framework, dalla schermata Impostazioni.
- **Impostazioni**: tema (sistema/chiaro/scuro) e lingua dell'app
  (sistema/italiano/inglese).

## Dati di gioco

I dati di gioco (personaggi, outfit, cibi, percorsi, eventi, Pulsanti P)
vengono da **Super Mario Wiki** (mariowiki.com),
contenuti testuali in licenza **CC BY-SA 4.0**, estratti dallo script in
[`tools/seedgen/`](tools/seedgen/README.md) e versionati in
[`seed/`](seed/README.md). Nessun dato è inventato: dettagli su fonti,
regole di estrazione e attribuzione in [`SPEC.md`](SPEC.md) §5.

I nomi italiani vengono da mariowiki.com e, per quello che lì manca
(piloti senza outfit, biomi, Gran Premi e Knockout Tour), dalla
[**Super Mario Wiki italiana**](https://www.mariowiki.it) (anch'essa
CC BY-SA 4.0). Due eccezioni: le missioni dei Pulsanti P restano in
inglese (la lista italiana è incompleta), e i nomi italiani dei **cibi**
sono una **traduzione non ufficiale** fatta a mano, perché nessuna fonte
li riporta. Lo stesso vale per le **condizioni della modalità specchio** in
italiano: tradotte a mano dal testo di mariowiki.com, perché quello della
wiki italiana è meno aggiornato, e per i **luoghi e i tipi di locale degli
stand Yoshi's** (pagina "List of Yoshi's locations"). I criteri di sblocco dei piloti vengono
invece dalle due wiki, ciascuno nella sua lingua.

## Mappa dei collezionabili

La mappa, le posizioni di Monete Peach, Pulsanti P e pannelli "?", le
istruzioni per trovarli (solo in inglese) e i video vengono dal progetto
[**mkworld-checklist**](https://github.com/BamisWasTaken/mkworld-checklist)
di BamisWasTaken, che pubblica [mktools.io](https://www.mktools.io).
Grazie! `tools/seedgen` legge i suoi dati a un commit fissato
(`sources.yaml`) e li abbina alle missioni di mariowiki; l'immagine della
mappa non è nel repository né nell'APK, l'app la scarica a runtime come
le immagini del wiki.

## Immagini

Le immagini di personaggi, outfit, Gran Premi e Knockout Tour vengono da
[**Super Mario Wiki**](https://www.mariowiki.com/Mario_Kart_World)
(pagina "Mario Kart World"), quelle dei cibi dalla pagina
[Dash Food](https://www.mariowiki.com/Dash_Food), servite dal suo CDN
`mario.wiki.gallery`.

- **Non sono nel repository né nell'APK.** `tools/seedgen` ne registra
  solo l'URL in `seed/` (`imageUrl`); l'app le scarica all'avvio e le
  tiene nella propria cache. Senza rete e senza cache si vedono i
  segnaposto con le iniziali.
- **Non sono coperte dalla licenza CC BY-SA** del testo del wiki: sono
  screenshot e artwork di Mario Kart World. Mario Kart World, i suoi
  personaggi e le relative immagini sono © Nintendo.
- Grazie a Super Mario Wiki e ai suoi contributori, che le hanno raccolte
  e caricate.

La grafica dell'interfaccia (banner, pista in fondo alla Home, logo e
icone della Home) viene da un mockup dell'autore; le icone della Home
richiamano oggetti di gioco e verranno sostituite. Font dei titoli:
[Lilita One](https://fonts.google.com/specimen/Lilita+One) di Juan
Montoreano, SIL Open Font License 1.1 (testo in
`app/src/main/assets/licenses/`).

KartLog è un progetto amatoriale e gratuito, **non affiliato né approvato
da Nintendo**. Lo stesso avviso è nell'app, in Impostazioni / Info.

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

Richiede JDK 17 o successivo, l'Android SDK (`compileSdk 37`) e accesso di rete
al repository Maven di Google. Gradle 9.8 e AGP 9.4 (vedi
[`docs/decisioni.md`](docs/decisioni.md), sezione "Toolchain").

**Quale APK installare:** quello del workflow *Build APK* (Actions → Build
APK → Run workflow) o di una release: è la build di release firmata sempre
con la stessa chiave, quindi si installa sopra l'app già presente senza
perdere i dati. La CI non carica più APK; dettagli in [`docs/ci.md`](docs/ci.md).

## Privacy

Nessuna analytics, nessun account. Il permesso INTERNET serve **solo** a
scaricare le immagini (vedi [Immagini](#immagini)) dal CDN di Super Mario
Wiki: nessun altro dato esce dal dispositivo, e tutto il resto dell'app
funziona anche offline. A build time la rete serve anche allo script
`tools/seedgen`, che rigenera i dati di gioco dal wiki.

## Stack tecnico

Kotlin e Jetpack Compose con Material 3, stessa architettura di
riferimento di [ThePatientGamerHelper](https://github.com/Marcogn/ThePatientGamerHelper)
(vedi [`docs/decisioni.md`](docs/decisioni.md), sezione "Toolchain",
per il dettaglio dell'allineamento):

- **Room** come fonte di verità per i dati seed e lo stato utente
- **Hilt** per la dependency injection
- **Navigation Compose** con rotte type-safe (`kotlinx.serialization`)
- **ViewModel + StateFlow**, flusso dati unidirezionale
- `minSdk 26`, `targetSdk 36`, `compileSdk 37`

## Struttura del progetto

```
SPEC.md                  specifica funzionale e tecnica completa
CLAUDE.md                guida operativa per chi (o cosa) sviluppa il progetto
docs/                    roadmap, decisioni di progetto, CI
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
- [`SPEC.md`](SPEC.md) — specifica funzionale e tecnica dell'app
- [`docs/roadmap.md`](docs/roadmap.md) — le prossime fasi di lavoro
- [`docs/decisioni.md`](docs/decisioni.md) — le scelte di progetto e il
  loro perché (fonti dei dati, grafica, toolchain, allineamento a
  ThePatientGamerHelper)
- [`docs/ci.md`](docs/ci.md) — i workflow di GitHub Actions (CI, Build
  APK, Release, pulizia delle esecuzioni), condivisi con gli altri progetti
  Android dell'autore (in inglese, come il file di riferimento)
- [`CLAUDE.md`](CLAUDE.md) — regole operative, comandi e stato per chi
  sviluppa il progetto
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

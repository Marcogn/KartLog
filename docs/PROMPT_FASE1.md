# Prompt di partenza — KartLog, Fase 1 (Scaffold)

## Contesto
Stai iniziando **KartLog**, app Android offline per tracciare i collectibles di Mario Kart World. Il repository contiene già:

- `CLAUDE.md`: regole operative, comandi, stato e decisioni. **Leggilo per primo** e tienilo aggiornato (sezione "Manutenzione").
- `SPEC.md`: specifica completa. Leggila tutta prima di scrivere codice, a partire da §0.
- `tools/seedgen/` e `seed/`: lo script Python che genera i dati di gioco e il seed iniziale. **Esistono già e non vanno riscritti** in questa fase.

Nel contesto di questa sessione trovi anche il progetto **ThePatientGamerHelper**. È il riferimento per stack, struttura Gradle, signing, varianti di build, pipeline di release e formato di README e CHANGELOG. Usa quello che hai nel contesto: non serve cercare nulla online.

## Passo 0 — Analisi del progetto di riferimento
Dal progetto di riferimento ricava almeno:
- `settings.gradle(.kts)`, `build.gradle(.kts)` di root e modulo app, version catalog se presente
- signing: keystore fisso per lo SHA1 stabile e come vengono passati i segreti
- build types, varianti, naming degli APK, versionCode/versionName
- script o workflow di release; struttura e convenzioni di README e CHANGELOG
- struttura dei package, setup di Hilt, Room, Navigation, tema, i18n se presente
- eventuale file di memoria per l'agente e come viene mantenuto

Poi scrivi `docs/ALIGNMENT.md`: una tabella breve **elemento → come lo fa il riferimento → come lo replichi in KartLog**. Se qualcosa contrasta con `SPEC.md`, segnalalo lì e segui `SPEC.md`. Se un'informazione non è nel progetto di riferimento, **chiedimela**: non inventare convenzioni.

## Obiettivo della sessione: Fase 1 di SPEC §8
- Progetto Android con la stessa struttura Gradle, le stesse versioni di toolchain e librerie condivise e lo stesso schema di signing e release del riferimento.
- Stack di SPEC §1: Kotlin, Compose, Material 3, Navigation Compose, Room, Hilt, kotlinx.serialization, MVVM con StateFlow.
- `ModalNavigationDrawer` con le voci di SPEC §2.1 e schermate placeholder per ciascuna.
- Home con griglia 2×2 di pulsanti quadrati (SPEC §2.2) e contatori fittizi.
- Tema Material 3 con palette custom vivace. Nessun asset Nintendo.
- Nessun permesso INTERNET nel manifest.
- **README.md** e **CHANGELOG.md** creati ricalcando struttura, sezioni e formato di quelli del riferimento. Il README include una sezione "Dati di gioco" che rimanda a `SPEC.md` §5 e a `tools/seedgen/README.md` e dichiara l'attribuzione CC BY-SA 4.0 a Super Mario Wiki.
- Se il riferimento ha altri file di progetto standard (es. `.gitignore`, `.editorconfig`, template di issue/PR, configurazione di lint o formatter), replicali adattandoli.
- `CLAUDE.md` aggiornato: sezioni "Comandi" e "Convenzioni" completate con quelle reali, "Stato attuale" e "Decisioni prese" aggiornate.

## Fuori scope in questa sessione
- Modifiche a `tools/seedgen/` e a `seed/`
- Task Gradle `generateSeed` e caricamento del seed nell'app (fase 3)
- Entità Room definitive
- Qualsiasi logica di Skin, collectibles, Consigliami o risultati

## Vincoli
- Non inventare dati di gioco, neanche come esempio visibile in UI: usa etichette generiche ("Personaggio 1").
- Il build debug funziona senza rete e senza Python.
- Il build release è firmato con lo stesso meccanismo del riferimento. I segreti si leggono dagli stessi posti (file locale non versionato o variabili d'ambiente), mai hardcoded.

## Fine sessione
- `./gradlew assembleDebug` e i test passano; anche `cd tools/seedgen && python -m pytest` continua a passare.
- `CLAUDE.md`, `README.md`, `CHANGELOG.md` e `docs/ALIGNMENT.md` aggiornati.
- Riepilogo finale: cosa hai fatto, deviazioni dal riferimento e perché, cosa resta aperto per la fase 2.

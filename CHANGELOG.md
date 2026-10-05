# Changelog

Tutte le modifiche rilevanti a KartLog sono documentate in questo file.
Il formato ricalca liberamente [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
il versionamento segue il `versionName` dell'app in `app/build.gradle.kts`.

## [Unreleased]

- **Nuova sezione Cibi.** Sottovoce di Personaggi nel drawer: una tessera per cibo con quanti outfit ti
  mancano; toccandola vedi le varianti, dove trovarlo (sui percorsi e sulle strade, il sushi diviso per
  variante) e gli outfit che sblocca per ogni personaggio, grigi se non li hai. Luoghi e nomi italiani
  sono una traduzione non ufficiale; gli stand dei percorsi sono nell'area, non per forza sul tracciato.
  I personaggi a cui l'outfit manca vengono per primi.
- **Consigliami parla di possibilità.** Gli stand Yoshi's di un percorso sono nell'area del
  percorso, non per forza sul tracciato di gara: ora i consigli dicono "fino a +N outfit" e
  "potrebbero esserci (occhi aperti)", con un avviso in cima al dettaglio e il promemoria che il
  cambio d'abito si può disattivare nella scelta del veicolo.
- **Tolto "Includi cibi nei dintorni".** I cibi di un evento vengono solo dagli stand dei suoi
  percorsi; gli stand sulle strade tra i percorsi arriveranno nella sezione Cibi.
- **Tutti gli stand Yoshi's e i cibi uno per uno nei dati (seedVersion 9).** 109 stand (70 nell'area
  dei percorsi, 39 sulle strade) con luogo in inglese e italiano (traduzione non ufficiale) e i 60
  cibi della pagina Dash Food con livello di boost e immagine, scaricata a runtime come le altre.
  Database aggiornato senza perdere nulla dei dati dell'utente.

## [1.1.2] - 2026-10-04

- **Gesto indietro di nuovo con lo scorrimento laterale.** Dalla 1.1.1, tornando indietro la
  schermata si rimpiccioliva e spariva: l'animazione predefinita del gesto indietro predittivo di
  Navigation 2.10. Ora il gesto usa la stessa animazione del tasto indietro, come nella 1.1.0.

## [1.1.1] - 2026-10-04

- **Fonte del Boomerang Rally aggiornata (seedVersion 8).** Il 03/10/2026 mariowiki ha rinominato la
  pagina "Boomerang Rally (Mario Kart World)" in "Boomerang Rally (rally)" e la build di release si
  fermava; dati di gioco invariati, cambiano solo il titolo della fonte e le revisioni registrate.
- **Librerie e strumenti di build aggiornati.** Gradle 9.8, Android Gradle
  Plugin 9.4, Kotlin 2.4, Compose BOM 2026.09, Room 2.8, Hilt 2.60, Coil 3.6
  e le altre librerie AndroidX alle ultime versioni stabili; nessuna
  funzione cambia (dettagli in `docs/decisioni.md`, sezione "Toolchain").
- **Workflow di GitHub Actions condivisi con gli altri progetti.** La CI ora
  verifica anche la build di release e non carica più l'APK debug (andava
  disinstallata l'app per provarlo); l'APK da provare è quello di *Build
  APK*, che ora controlla la firma, come la release (`docs/ci.md`).

## [1.1.0] - 2026-09-29

- **Mappa dei collezionabili.** Nuova schermata "Mappa" (menu laterale, e
  pulsantone rosso in Monete Peach e Pulsanti P) con le posizioni di
  Monete Peach, Pulsanti P e pannelli "?": zoom, filtri per tipo e per
  "fatti", popup con istruzioni (in inglese), spunta e video YouTube.
  Dati e mappa dal progetto mkworld-checklist (mktools.io) di
  BamisWasTaken; l'immagine si scarica a runtime.
- **Monete Peach una per una.** Le 200 monete diventano punti sulla mappa
  con le loro istruzioni, al posto del contatore per bioma. I conteggi
  segnati prima non si possono convertire: un avviso lo spiega.
- **Home rivista.** Personaggi e Risultati, Pulsanti P e Mappa del mondo,
  Consigliami sotto; Monete Peach resta nel menu. Il contatore di
  Risultati conta i trofei per cilindrata (x/80), non più gli eventi.
- **Pannelli "?".** I 150 pannelli sulla mappa e in una pagina dedicata,
  uno per uno con le istruzioni come le Monete Peach; inclusi nel backup.
- **Menu con i Collezionabili.** Monete Peach, Pulsanti P e Pannelli ?
  raggruppati sotto "Collezionabili", come voci più piccole sempre
  visibili.

## [1.0.0] - 2026-09-27

- **First release!**

## [0.1.x] - beta

Le versioni di prova dalla 0.1.1 alla 0.1.7 (25–27/09/2026), riunite in
una sola voce. Cosa contiene l'app alla fine della beta:

- **Personaggi.** Tutti i 50 piloti, con le immagini di Super Mario Wiki
  in card a polaroid del colore del personaggio. I 24 con outfit hanno
  la pagina degli outfit, con i cibi che li sbloccano. I 32 piloti di
  base sono sempre sbloccati; per i 18 da sbloccare un popup mostra il
  criterio dal wiki e l'interruttore per segnarli. Grigio se ti manca,
  colorato se ce l'hai. Filtro Tutti/Incompleti e ordinamento per
  roster, alfabetico o completamento.
- **Monete Peach e Pulsanti P.** Monete Peach con un contatore − / + per
  bioma; i 394 Pulsanti P divisi per regione e percorso, con ricerca e
  "segna tutti".
- **Consigliami.** Quale Gran Premio o Knockout Tour correre, e con chi,
  per sbloccare più outfit, con i cibi sul percorso o nei dintorni e,
  a scelta, il peso dei risultati già ottenuti.
- **Risultati.** Il miglior trofeo (bronzo … oro ★★★) per ogni evento e
  cilindrata, a schede; una "i" sulla scheda Mirror spiega come
  sbloccare la modalità specchio.
- **Grafica da gioco.** Banner con cielo e pista, tessere colorate in
  Home, cielo con nuvole dietro ogni schermata (notturno nel tema
  scuro), menu laterale rosso, pulsantoni rossi, popup a quadri, font
  Lilita One.
- **Italiano e inglese.** Nomi ufficiali in italiano da mariowiki.com e
  mariowiki.it; nomi dei cibi e condizioni della modalità specchio in
  italiano sono una traduzione non ufficiale. Le missioni dei Pulsanti P
  restano in inglese. Tema e lingua si scelgono in Impostazioni.
- **Backup.** Esportazione e importazione dello stato in JSON da
  Impostazioni, con l'attribuzione dei dati (CC BY-SA 4.0, Super Mario
  Wiki).
- **Dati di gioco da Super Mario Wiki (seedVersion 6).** Estratti da
  `tools/seedgen` e verificati a ogni build di release. Le immagini si
  scaricano a runtime e non sono incluse nell'app.

# Changelog

Tutte le modifiche rilevanti a KartLog sono documentate in questo file.
Il formato ricalca liberamente [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
il versionamento segue il `versionName` dell'app in `app/build.gradle.kts`.

## [Unreleased]

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

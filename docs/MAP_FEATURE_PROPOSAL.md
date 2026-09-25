# Proposta: mappa interattiva per Monete Peach e Pulsanti P

Analisi di [BamisWasTaken/mkworld-checklist](https://github.com/BamisWasTaken/mkworld-checklist)
(sito pubblico: mktools.io) su richiesta dell'autore, per capire cosa possiamo prendere in prestito
per aggiungere una mappa interattiva a KartLog, con le checklist di Monete Peach e Pulsanti P
collegate. **Solo un piano — nessun codice scritto.** La mappa è esplicitamente fuori scope v1
(`SPEC.md` §7, "Mappa interattiva"): serve un aggiornamento della SPEC prima di implementare
qualsiasi cosa qui sotto.

## 1. Cosa fa mktools.io

App Angular 20 con tre sezioni: un "album di figurine" (sticker album) delle skin, un elenco
automatico di cose da fare, e una **mappa interattiva pan/zoom** con un'icona per ogni collezionabile
(Pulsanti P, Monete Peach, "? Panel") posizionata sulla mappa di gioco; toccando un'icona si apre un
tooltip con istruzioni e un video YouTube di riferimento.

Repo clonato e ispezionato (non modificato, non pubblicato da nessuna parte).

## 2. Il problema: niente qui è riutilizzabile così com'è

**Nessuna licenza.** Il repository non ha un file `LICENSE` e l'API di GitHub conferma
`license: null`. Senza una licenza esplicita, per le norme GitHub/il diritto d'autore di default il
codice e i dati restano "tutti i diritti riservati": si può guardare il codice su GitHub, non
copiarlo/modificarlo/ridistribuirlo. Questo vale sia per il codice Angular sia per i file dati.

**La mappa è un asset Nintendo.** `public/imgs/map.webp`/`map-upscaled.webp` è un render/screenshot
diretto della mappa del mondo aperto di Mario Kart World (l'ho controllata a occhio: si riconoscono
tutti i percorsi/diorami del gioco). Anche a prescindere dalla licenza mancante del repo, questo va
**contro la regola non negoziabile #2 di `CLAUDE.md`** ("Nessun asset Nintendo: niente render, loghi,
font, screenshot"). Stesso discorso per le ~500 immagini in `public/imgs/tooltips/` (screenshot per
singolo collezionabile). **Non possiamo usare né quella mappa né quelle immagini, in nessuna forma.**

**I dati di posizione sono un dataset compilato, non un fatto di pubblico dominio.** In
`public/data/checklist-data.json` ogni Pulsante P (394) e ogni Moneta Peach (200) ha
`collectibleModel.xPercentage`/`yPercentage` (posizione in percentuale sulla mappa, indipendente
dalla risoluzione dell'immagine) più un `youtubeId` di riferimento. I conteggi combaciano esattamente
con i nostri (394 e 200) — è un dataset preciso e verosimilmente corretto, ma è un lavoro di
compilazione manuale dell'autore (o della sua community), non un fatto dichiarato da Nintendo: senza
una licenza, copiarlo è lo stesso rischio del codice, e va comunque contro l'abitudine di questo
progetto di usare solo fonti con licenza chiara (CC BY-SA 4.0 da mariowiki, mai scraping automatico
di fonti non verificate — vedi `CLAUDE.md`, "Decisioni prese").

**In sintesi: non possiamo copiare né la mappa, né le immagini, né le coordinate.** Possiamo però
osservare l'architettura e prendere ispirazione — idee e struttura non sono coperte da copyright allo
stesso modo del codice o delle immagini.

## 3. Cosa vale la pena osservare (idee, non codice)

- **Coordinate in percentuale, non in pixel**: `xPercentage`/`yPercentage` (0–100) invece di
  coordinate assolute — la posizione di un'icona resta corretta qualunque sia la risoluzione/scala
  dell'immagine mostrata. Pattern semplice e giusto da replicare se mai avessimo una nostra mappa.
- **Pan/zoom con libreria dedicata** (`panzoom`, JS): per Compose esiste l'equivalente nativo
  (`Modifier.pointerInput` + `detectTransformGestures`, o `graphicsLayer` con `scale`/`offset`
  animati) — nessuna dipendenza esterna necessaria, coerente con lo stile "poche dipendenze" già
  seguito in questo progetto.
- **Un solo componente tooltip riusabile**, posizionato in base alle coordinate dell'icona toccata e
  alla scala corrente dello zoom — stesso problema che avremmo noi.
- **Ottimizzazione con un quad-tree** (`map-section/models/quad-tree-node.ts`) per non renderizzare
  centinaia di icone fuori dallo schermo durante lo zoom — utile solo se la nostra mappa avesse
  centinaia di pin visibili contemporaneamente; con liste per regione già esistenti (10 regioni,
  max ~40 pulsanti P l'una) probabilmente non serve neanche.
- **Checklist e mappa condividono lo stesso stato** (uno "spuntato" aggiorna sia la lista sia l'icona
  sulla mappa) — esattamente il pattern che useremmo noi con `UserStateDao` come unica fonte di
  verità, già il nostro pattern attuale per Skin/Monete/Pulsanti P.

## 4. Il vero problema da risolvere: i dati di posizione

Senza le coordinate (nostre, con licenza pulita), non c'è mappa. Tre strade, in ordine di
preferenza:

1. **Chiedere il permesso all'autore.** L'opzione più semplice se funziona: aprire una issue/discussione
   sul repo chiedendo se è disposto a rilasciare `checklist-data.json` (o solo le coordinate) con una
   licenza aperta (es. CC0 o CC BY), citandolo come fonte in `seed/meta.json` esattamente come si fa
   già con mariowiki. Probabilmente non ci ha mai pensato — è plausibile dica di sì. **Serve
   un'azione dell'autore (te), non mia: non contatto terzi per conto tuo.**
2. **Raccogliere le coordinate in modo indipendente**, con lo stesso approccio già usato per
   `tools/seedgen/manual/peach_medallions.yaml` (fonte dichiarata, verificata a mano, mai scraping
   automatico): guardare le stesse guide video pubbliche (o mariowiki, se avesse mai una mappa con
   coordinate — da verificare) e trascrivere posizioni a mano in un file manuale simile. Lavoro
   enorme (594 punti) e via via che la trascrizione random di un dataset già esistente rischia
   comunque di essere "troppo simile" per essere davvero indipendente se fatta guardando gli stessi
   video sorgente uno per uno.
3. **Rinunciare ai pin precisi**, e fare una mappa "a regioni" invece che "a coordinate esatte":
   riusare la struttura già esistente (10 regioni, `RegionSection.kt` già condiviso da Monete
   Peach/Pulsanti P) sopra una mappa **stilizzata originale** (non uno screenshot del gioco) con le
   10 zone disegnate come forme astratte cliccabili, non come mappa realistica. Zero problemi di
   licenza/asset, ma molto meno preciso e più lavoro di design (serve un'illustrazione originale).

**Raccomandazione**: provare la strada 1 per prima (basso sforzo, alto valore se va a buon fine); se
non risponde o rifiuta, la strada 3 è l'unica pulita senza un lavoro di trascrizione enorme. La
strada 2 la eviterei: è tanto lavoro per un risultato la cui indipendenza dal dataset originale
resterebbe comunque discutibile.

## 5. Cosa NON cambia, qualunque strada si scelga

Anche con coordinate proprie, la mappa **resta senza l'artwork del gioco** (regola #2, non
negoziabile): servirebbe un'illustrazione originale (stile libero, non un tentativo di imitare la
mappa di Nintendo) — lavoro di design a parte, non di programmazione.

## 6. Piano a fasi, SE si procede (dopo aver risolto il punto 4)

Numerazione indicativa, da confermare in `docs/PHASES.md` come fase dedicata dopo un aggiornamento
di `SPEC.md` (rimuovere "Mappa interattiva" da §7 "Fuori scope v1", aggiungere una sezione §2.x con
UI/interazione attesa, come per le altre schermate).

1. **Dati**: se strada 1 riesce, un nuovo script/step in `tools/seedgen` per normalizzare le
   coordinate ricevute sugli id stabili già esistenti (`medallion_<regione>_NN`,
   `pswitch_NNN`) — stesso principio di `manual/peach_medallions.yaml`, con licenza e fonte annotate
   in `meta.json`. Se strada 3, non serve seedgen: le "zone" sulla mappa sarebbero le 10 regioni già
   in `regions.json`.
2. **Schema**: `PeachMedallionEntity`/`PSwitchEntity` guadagnano `xPercentage`/`yPercentage`
   nullable (migrazione Room additiva, stesso pattern di `MIGRATION_1_2` per `nameIt` — mai
   `fallbackToDestructiveMigration`, l'app ha utenti reali).
3. **UI**: nuova schermata `ui/map/MapScreen.kt`, voce di drawer propria o azione dalle schermate
   Monete Peach/Pulsanti P esistenti (da decidere: mappa come quarta vista sullo stesso stato, non
   una raccolta dati separata — stesso `UserStateDao`, stesso "segnato" ovunque). Pan/zoom nativo
   Compose, tooltip al tap su un pin, stesso "spunta" delle liste esistenti.
4. **Nessuna funzione nuova su Consigliami**: la mappa è un modo diverso di vedere/segnare Monete
   Peach e Pulsanti P, non tocca l'algoritmo.

## 7. Domande aperte per l'autore (Marco)

- Vuoi che scriva io il testo di una issue su GitHub per chiedere la licenza dei dati a BamisWasTaken,
  o preferisci scriverla/mandarla tu?
- Se l'autore non risponde o rifiuta: vai avanti con la mappa "a regioni" (punto 4.3), o preferisci
  lasciare la mappa fuori scope e concentrarti su altro?
- Vuoi includere anche i "? Panel" (150, visti nei dati dell'altro repo) come nuova categoria di
  collezionabile? Non sono nel nostro `SPEC.md` attuale (citati in §7 come "possibile v2, con la
  stessa struttura di medaglioni e pulsanti P") — servirebbe comunque una fonte con licenza pulita
  per nome/conteggio/posizione, stesso problema del punto 4.

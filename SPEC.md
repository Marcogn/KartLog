# KartLog — Specifica tecnica (v0.6, allineata all'app 1.1.2 più la fase C1)

Tracker Android offline per i collectibles di Mario Kart World: outfit (skin), Peach Medallions, P Switch, con un modulo "Consigliami" che suggerisce quale Gran Premio o Knockout Tour correre e con quale personaggio per sbloccare più outfit mancanti.

> Nome di lavoro: "KartLog". Non usare "Mario", "Mario Kart" o "Nintendo" nel nome dell'app, nell'icona o nel package.

---

## 0. Regole per chi implementa (leggere prima di tutto)

1. **Non inventare mai dati di gioco.** Personaggi, outfit, cibi, cup, rally, percorsi, medaglie e P Switch arrivano **solo** dai file seed JSON (§5), generati da `tools/seedgen`. Non modificare `seed/` a mano. Se un dato manca, fermati e chiedi all'autore: non usare valori plausibili al suo posto.
2. **Nessun asset Nintendo nel repository**: niente render, loghi, font o screenshot, né nel repository né nell'APK. Le immagini di personaggi, outfit ed eventi si scaricano **a runtime** dal CDN di Super Mario Wiki (URL `imageUrl` nel seed, estratti da seedgen dalla pagina "Mario Kart World"); finché non ci sono si vede un placeholder con le iniziali. Decisione dell'autore del 26/09/2026, vedi `docs/decisioni.md`.
3. **Offline tranne le immagini**: il permesso INTERNET serve solo a scaricare le immagini del punto 2; nessuna analytics, nessun altro traffico, e l'app funziona interamente anche senza rete. I dati di gioco si scaricano **solo a build time**, nello script di estrazione dati (§5).
4. Lavora per fasi (`docs/roadmap.md`). Alla fine di ogni fase il progetto deve compilare e i test devono passare. Le scelte non ovvie e il loro perché sono in `docs/decisioni.md`.

---

## 1. Stack

> **Allineamento:** stack, struttura Gradle, signing e pipeline di release devono ricalcare **ThePatientGamerHelper** e le app gemelle. Dove questa sezione e il progetto di riferimento divergono, vince il progetto di riferimento, salvo per quanto richiesto esplicitamente da questa spec.

- Kotlin, Jetpack Compose, Material 3
- Navigation Compose
- Room per lo stato utente e i dati seed
- Hilt per la DI
- kotlinx.serialization per il parsing dei seed JSON
- Architettura MVVM: ViewModel + StateFlow, repository, UseCase per la logica di Consigliami
- minSdk 26, `compileSdk` 37, `targetSdk` 36 (resta 36 finché l'app non è provata su Android 17, `docs/decisioni.md`)
- Test: JUnit per l'algoritmo di raccomandazione e la validazione seed, test Compose UI solo per i flussi principali
- Keystore fisso per mantenere sempre lo stesso SHA1, varianti debug e release, README e CHANGELOG sempre aggiornati (come nel progetto di riferimento)
- Python 3 (solo tooling di build, §5) con dipendenze fissate in `tools/seedgen/requirements.txt`

---

## 2. Navigazione e UI

### 2.1 Struttura
- `ModalNavigationDrawer` con hamburger in alto a sinistra, sempre disponibile.
- Voci del drawer: **Home**, **Personaggi** con la sottovoce **Cibi** (§2.3.1, rientrata e più piccola come quelle di Collezionabili), la sezione **Collezionabili** (intestazione non cliccabile con tre voci più piccole e rientrate, sempre visibili: **Monete Peach**, **Pulsanti P**, **Pannelli ?**), **Mappa del mondo** (§2.4), **Consigliami**, **Risultati**, più in fondo **Impostazioni / Info** (crediti e licenze dei dati, §5.5).

### 2.2 Home
- Griglia 2×2 di pulsanti grandi (disposizione dell'autore, 28/09/2026): **Personaggi, Risultati** / **Pulsanti P, Mappa del mondo** (§2.4). Monete Peach non ha più una tessera: resta nel menu laterale e sulla mappa.
- Ogni pulsante mostra icona, titolo e un contatore di progresso (es. `87 / 127`); per Risultati (§2.6) è il numero di trofei registrati, uno per coppia evento + cilindrata di qualsiasi livello, su eventi × 4 cilindrate (`12 / 80`). La Mappa del mondo non ha contatore; la sua icona è disegnata (mappa bianca su disco rosso).
- Sotto la griglia, un pulsante largo a tutta riga **Consigliami** (§2.5) con una riga di spiegazione ("Quale Gran Premio o Knockout Tour correre, e con chi, per sbloccare più outfit") e nessun contatore.
- Il look deve essere colorato e "da gioco": palette vivace, angoli arrotondati generosi, tipografia bold. Non deve imitare la UI ufficiale. Dal 26/09/2026 segue il mockup dell'autore: banner con cielo e pista (logo in Home, titolo altrove) in alto su ogni schermata, tessere a gradiente con scacchi tenui, icona tonda, titolo bianco con contorno scuro e contatore in una pillola, pista in fondo alla Home. Font dei titoli Lilita One.

### 2.3 Personaggi
**Schermata lista personaggi** (si chiamava "Skin")
- `LazyVerticalGrid` adattiva (**3 colonne** su telefono): le immagini sono verticali, nelle proporzioni della schermata di selezione del gioco. Ci sono **tutti i 50 piloti**: i 24 con outfit alternativi e i 26 che ne hanno solo uno (Goomba, Mucca…).
- Ogni cella mostra l'immagine del personaggio (placeholder con le iniziali finché non è scaricata), il nome e un contatore `ottenuti/totali`; per i piloti senza outfit, "Sbloccato" / "Da sbloccare". Un lucchetto sull'immagine segna chi non è ancora sbloccato.
- Tap: pilota bloccato → popup con il criterio di sblocco (mariowiki) e l'interruttore; sbloccato con outfit → dettaglio; sbloccabile senza outfit → di nuovo il popup (per poterlo ribloccare); di base senza outfit → niente.
- Stato iniziale di sblocco dal wiki: i 32 piloti "di base" partono sbloccati, i 18 "sbloccabili" (compresi DK, Daisy, Rosalinda, Lakitu, Bowser Jr., Strutzi, Re Boo) no. Consigliami considera solo i personaggi sbloccati (§6).
- Il dettaglio mostra gli outfit come griglia di card con la loro immagine (2 per riga su telefono); l'outfit di default usa l'immagine del personaggio.
- Se il personaggio ha **tutti** gli outfit (per i piloti da sbloccare senza outfit: se è sbloccato) si ordina in fondo; i piloti di base senza outfit non hanno nulla da completare e si ordinano insieme agli altri (il filtro "Incompleti" non li mostra). Colori (regola dell'autore, 27/09/2026): **grigio = ti manca, colorato = ce l'hai**, quindi la cella è grigia solo se il pilota è da sbloccare (con lucchetto e popup del criterio); nel dettaglio sono grigi gli outfit non ancora ottenuti. I piloti di base sono sempre sbloccati. L'ordinamento è configurabile: roster / alfabetico / % completamento. "Roster" è l'ordine di `aliases.yaml` di seedgen (prima i 24 con outfit, poi gli altri), **non** quello della schermata di selezione del gioco: scelta dell'autore, che lo preferisce.
- Filtro in alto: Tutti / Incompleti.

**Schermata dettaglio personaggio**
- Header con nome, contatore e, per i piloti da sbloccare, criterio di sblocco e switch **"Personaggio sbloccato"**, necessario per Consigliami (§6). I piloti di base sono sempre sbloccati e non hanno lo switch.
- Lista outfit: ogni riga ha checkbox, nome outfit e **tutti** i gruppi di cibo che lo sbloccano (es. Mario Touring: "Hamburger · Barbecue · Moo Moo Milk"). Se non ci sono regole note, mostra "cibo sconosciuto".
- L'outfit di default non è una riga spuntabile: è sempre posseduto e non si conta nei mancanti.

### 2.3.1 Cibi (fase C2)
Sottosezione di Personaggi, raggiungibile dal drawer; non è un collezionabile e non ha stato utente. Da Personaggi non c'è ancora un accesso diretto (da proporre all'autore).

**Griglia** (`ui/food/`, `Destination.Food`): una tessera `KartPanel` per gruppo di cibo (20, nell'ordine del seed), colonne adattive (min 150 dp), con l'immagine della prima variante (segnaposto con le iniziali finché non è caricata), il nome nella lingua dell'app (traduzione non ufficiale) e una pillola "Ne mancano N" con gli outfit che il cibo dà e che non hai (`outfit_food_rules` senza `owned_outfits`), oppure "Nessun outfit" (lunchbox). I cibi non si ingrigiscono mai.

**Dettaglio** (`Destination.FoodDetail(foodGroupId)`): in cima le varianti (immagine in un riquadro uniforme, perché quelle del wiki hanno o no una cornice incorporata; nome, boost); "Dove si trova": una sezione per gruppo, divisa per variante solo nel sushi (la fonte nomina il cibo preciso), in pannelli "Sui percorsi" (bioma › percorso e i luoghi) e "Sulle strade" (bioma se la fonte lo dice, poi il luogo); "Outfit che sblocca": un pannello per personaggio, due per riga (un cibo dà al massimo un outfit per personaggio, verificato sul seed), prima i personaggi a cui l'outfit manca e poi gli altri, a parità in ordine di roster, con avatar e polaroid piccola dell'outfit, **grigie se non li hai, colorate se li hai** (non spuntabili da qui); in fondo i personaggi con outfit a cui questo cibo non ne dà. Gli stand dei percorsi sono nell'area, non per forza sul tracciato (§6.1). Logica di raggruppamento pura in `FoodDetailModel.kt`.

### 2.4 Monete Peach e Pulsanti P
In alto c'è il contatore globale (`x/200`, `x/394`). In basso a destra, in sovrimpressione, un pulsantone tondo **rosso con l'icona bianca della mappa** apre la Mappa con i soli punti di quella schermata; ogni riga ha anche un'icona "Mostra sulla mappa" che la apre centrata su quel punto.

**Monete Peach** (dal 28/09/2026, dati di mkworld-checklist, §5.2)
- Le **200 monete una per una**, in ordine, ciascuna con checkbox e le istruzioni per trovarla (solo in inglese). Niente più biomi: la fonte non li dà.
- I conteggi per bioma della versione precedente non si possono convertire (non dicono quali monete): la migrazione Room v6→v7 li cancella e l'app mostra una volta un avviso con quante monete erano segnate (scelta dell'autore).

**Pulsanti P**
- Sezioni collassabili per regione; dentro ogni regione, sottogruppi per percorso (o luogo, es. Chain Chomp Desert).
- Ogni riga mostra il nome della missione (testo in-game dal wiki) e il percorso. I nomi delle missioni restano in inglese: la lista italiana di mariowiki.it è incompleta e senza una chiave per abbinarla a quella inglese (vedi `tools/seedgen/seedgen/it_wiki.py`).
- Ricerca testuale sul nome della missione.
- Le **10 regioni** (`regions.json`, nomi italiani da mariowiki.it) hanno ciascuna `x/y` e un'azione "segna tutti" che chiede conferma.

**Mappa** (voce del drawer, e pulsantone delle due schermate sopra)
- L'immagine della mappa del mondo di mkworld-checklist, scaricata a runtime come le immagini del wiki (URL in `map.json`, mai nell'APK); senza rete e senza cache resta il mare, e i punti si vedono e si toccano lo stesso.
- Zoom con il pizzico o il doppio tap (fino a ×8), trascinamento; i marker crescono con lo zoom (piccoli con la mappa intera, pieni da ×4) ma non seguono l'ingrandimento dell'immagine. Forma e simbolo per tipo: Pulsante P = cerchio blu con "P", Moneta Peach = moneta rosa con corona, pannello "?" = quadrato giallo con "?"; i punti già fatti restano riconoscibili, attenuati, con una spunta verde.
- **Filtri visivi** in alto (pulsantoni rossi in griglia 2×2): un interruttore per tipo, con il marker come legenda e il contatore `fatti/totali`, e "Mostra fatti" per mostrare o nascondere i punti già fatti. Aperta da una schermata, mostra solo il suo tipo.
- Tap su un punto: popup (§ grafica) con il nome della missione e il luogo (Pulsanti P), le istruzioni (inglese), l'interruttore "Presa/Completata/Attivato" e "Guarda il video" (YouTube, Intent `ACTION_VIEW`). Lo stato è lo stesso delle liste.
- I **pannelli "?"** (150) hanno anche una lista uno per uno come le Monete Peach (stessa schermata condivisa, `MapPointListScreen`), senza contatore in Home.

### 2.5 Consigliami
Vedi §6 per l'algoritmo. UI:
- Toggle: **Gran Premi** / **Knockout Tour** / **Entrambi**.
- Lista ordinata per punteggio (decrescente), con **posizioni a pari merito** (1, 1, 1, 4…), come da §6.4.
- Gli eventi a pari merito si raggruppano in **una card espandibile**. Se hanno in comune i corsi che generano il guadagno, il titolo li nomina: "4 eventi passano da Crown City · stesso guadagno". All'interno del gruppo restano ordinati con gli spareggi §6.4.
- Ogni card evento mostra:
  - nome evento, tipo (Cup/Rally) e posizione in classifica
  - **personaggio consigliato** e numero di outfit nuovi che potrebbe ottenere ("fino a +N")
  - i 2 personaggi alternativi successivi con i rispettivi numeri
  - i cibi rilevanti che **potrebbero esserci** sull'evento, ciascuno con il percorso: lo stand Yoshi's è nell'area del percorso, non per forza sul tracciato di gara ("occhi aperti", §5.2.1)
  - se i risultati sono attivi, il trofeo registrato alla cilindrata di riferimento (§2.6)
- Tap sulla card apre il dettaglio: un avviso in cima (outfit che *potrebbero* sbloccarsi), tutti i personaggi con gain > 0, outfit specifici ottenibili per ciascuno (il dominio dà anche cibo e percorso di ogni outfit, per le righe "outfit – cibo – percorso" della fase C4), e il miglior trofeo per ogni cilindrata **in sola lettura**. Consigliami non registra risultati: si fa solo dalla schermata Risultati (§2.6).
- Sezione "Pesa i risultati": switch on/off più slider del peso (§6.3).
- Banner fisso in basso: gli stand indicati sono nell'area dei percorsi, non per forza sul tracciato ("occhi aperti"); se nella scelta del veicolo è disattivato il cambio d'abito degli snack scatto, non si sblocca nulla (pagina Dash Food).
- Non c'è più lo switch "Includi cibi nei dintorni" (tolto con la fase C1: la sezione Cibi della fase C2 mostra tutti i luoghi).

### 2.6 Risultati
Schermata dedicata, raggiunta dal pulsante largo in Home e dal drawer. Registra il **miglior risultato** di ogni Gran Premio e Knockout Tour, non uno storico.
- Il risultato è un **trofeo** su un'unica scala crescente: bronzo, argento, oro, oro ★, oro ★★, oro ★★★. Le stelle esistono solo con l'oro; dal 4° posto in giù non c'è trofeo, quindi equivale a "nessun risultato". Fonti: [Game Rant](https://gamerant.com/mario-kart-world-grand-prix-how-get-three-stars-rank-gold-trophy/) (scala), [TheGamer](https://www.thegamer.com/mario-kart-world-grand-prix-knockout-tour-three-star-guide-how-to/) (Knockout Tour: 1° oro, 2° argento, 3° bronzo, dal 4° nessun trofeo).
- Un solo trofeo per coppia (evento, cilindrata): sceglierne un altro **sostituisce** il precedente, e "Nessun trofeo" lo cancella.
- Selettore di cilindrata (50/100/150/Specchio, default 150cc) e lista di tutti gli eventi, divisi in Gran Premi e Knockout Tour, mai filtrata da Consigliami.
- Un trofeo vale **solo** per la cilindrata in cui è registrato: nessun riporto automatico verso le cilindrate inferiori, perché il comportamento del gioco non è confermato con certezza (decisione dell'autore).

---

## 3. Modello dati (Room)

Dati seed (read-only, ricaricati a ogni cambio di `seedVersion`):

Le entità ricalcano i JSON di `seed/` (§5.1), che esistono già nel repository.

```
Character(id: String PK, name: String, nameIt: String?, rosterOrder: Int, starter: Boolean,
          unlockCriteria: String?, unlockCriteriaIt: String?, imageUrl: String?)   // 50 piloti; c'è anche imageRes, residuo dello scaffold, sempre null
Outfit(id: String PK, characterId: FK, name: String?, nameIt: String?, isDefault: Boolean, imageUrl: String?)
    // id = "<character>__<outfit>": i nomi NON sono unici tra personaggi ("Pro Racer").
    // Il default ha id "<character>__default" e name null (il wiki non lo nomina): in UI "Standard".
FoodGroup(id: String PK, name: String, nameIt: String?, foods: List<String>, revertsToDefault: Boolean)   // nameIt: traduzione non ufficiale
    // Un gruppo = una cella Outfits distinta della tabella Dash Food (20 gruppi).
    // foods: nomi dei cibi che lo compongono, es. ["Takoyaki","Candy apple","Cheep Cheep taiyaki","Sushi"].
OutfitFoodRule(outfitId: FK, foodGroupId: FK)              // N:M: lo stesso outfit si ottiene da più gruppi (es. Mario Touring)
FoodVariant(id: String PK, foodGroupId: FK, order: Int, name: String, nameIt: String?, boost: List<SMALL|MEDIUM|LARGE>, imageUrl: String?)
    // Una riga della tabella "List of food" di Dash Food (60): in genere 3 per gruppo; il sushi ha 4 cibi
    // (takoyaki, mela caramellata, taiyaki, sushi), il piatto triplo ha boost [SMALL, MEDIUM, LARGE].
    // id "<gruppo>_<n>"; nameIt: traduzione NON ufficiale.
YoshiStand(id: String PK, order: Int, courseId: FK?, regionId: FK?, establishment: String?, establishmentIt: String?,
           location: String?, locationIt: String?)
    // Uno stand di "List of Yoshi's locations" (una riga = una foto = uno stand): 70 nell'area di 21 percorsi,
    // 39 sulle strade (courseId null). regionId = quello del corso; null sulle strade (la fonte non lo dice).
    // location null = il wiki non lo indica. *It: traduzione NON ufficiale. id "stand_<corso>_<nn>" / "stand_route_<nn>".
YoshiStandFood(standId: FK, foodGroupId: FK, food: String?)    // PK (standId, foodGroupId)
    // food = cibo preciso solo dove la fonte lo nomina (gruppo sushi, = FoodVariant.name); null = tutto il gruppo.
Course(id: String PK, name: String, nameIt: String?, regionId: FK?)   // regionId null solo per Rainbow Road
Region(id: String PK, name: String, nameIt: String?, order: Int)     // 10 regioni/biomi
Area(id: String PK, name: String, regionId: FK)               // luoghi delle missioni che non sono uno dei 30 corsi
PeachMedallion(id: String PK, index: Int, x: Double, y: Double, hint: String?, youtubeId: String?)   // 200, mkworld-checklist (§5.2)
QuestionPanel(id: String PK, index: Int, x: Double, y: Double, hint: String?, youtubeId: String?)    // 150, mkworld-checklist
PSwitch(id: String PK, index: Int, regionId: FK, courseId: FK?, areaId: FK?, name: String, x, y, hint, youtubeId)  // 394, esattamente uno tra courseId e areaId
    // x, y: percentuale dell'immagine della mappa (map.json: imageUrl, width, height, letto dagli asset, nessuna tabella)
Event(id: String PK, type: CUP|RALLY, name: String, nameIt: String?, order: Int, imageUrl: String?)
EventStop(eventId: FK, position: Int, courseId: FK)         // nel JSON è l'array "stops" dell'evento, in ordine
```

Le condizioni della modalità specchio (`mirror_mode.json`) e l'immagine della mappa (`map.json`) si leggono dagli asset, senza tabella. Room v8: `food_group_courses` (fino alla v7) è sostituita dagli stand (`MIGRATION_7_8`).

Stato utente (mai toccato dal reseed):

```
OwnedOutfit(outfitId PK)
CharacterUnlock(characterId PK, unlocked: Boolean)
CollectedMedallion(medallionId PK)
CompletedPSwitch(pSwitchId PK)
ActivatedQuestionPanel(panelId PK)
PendingNotice(id PK, count: Int)        // avvisi da mostrare una volta, scritti da una migrazione (v6 -> v7)
BestResult(eventId, cc, rank: BRONZE|SILVER|GOLD|GOLD_1_STAR|GOLD_2_STARS|GOLD_3_STARS)   // PK (eventId, cc), §2.6
    // Schema v3. Fino alla v2 era RaceResult (storico con stelle e posizione indipendenti): la migrazione
    // tiene il livello più alto per (evento, cilindrata) con 1° -> oro + stelle, 2° -> argento, 3° -> bronzo.
```

Il reseed usa upsert sui dati statici e **non** cancella lo stato utente. Gli ID seed sono stringhe stabili (slug), mai autoincrement.

---

## 4. Persistenza extra
- Export/import JSON dello **stato utente** (non dei seed) tramite Storage Access Framework, per backup manuale.

---

## 5. Seed data

### 5.1 File
In `seed/` alla root del repository (versionati, **già presenti**), copiati negli asset dell'APK dal build. Ogni file è `{"source": <URL>, "items": [...]}`:

| File | Contenuto | Righe attuali |
|---|---|---|
| `characters.json` | `id`, `name`, `nameIt`, `rosterOrder`, `starter`, `unlockCriteria`, `unlockCriteriaIt`, `imageUrl` | 50 (24 con outfit, 32 di base) |
| `outfits.json` | `id`, `characterId`, `name` (null per il default), `nameIt`, `isDefault`, `imageUrl` | 127 (103 + 24 default) |
| `food_groups.json` | `id`, `name`, `nameIt`, `foods`, `revertsToDefault` | 20 |
| `outfit_food_rules.json` | `outfitId`, `foodGroupId` | — |
| `food_variants.json` | `id`, `foodGroupId`, `order`, `name`, `nameIt` (traduzione non ufficiale), `boost`, `imageUrl` | 60 |
| `yoshi_stands.json` | `id`, `courseId` (null = strada), `regionId`, `establishment`, `establishmentIt`, `location`, `locationIt`, `foods` (`foodGroupId`, `food`) | 70 sui percorsi + 39 sulle strade |
| `courses.json` | `id`, `name`, `nameIt`, `regionId` | 30 |
| `regions.json` | `id`, `name`, `nameIt`, `order` | 10 |
| `areas.json` | `id`, `name`, `regionId` | 2 (cresce se le missioni citano altri luoghi) |
| `events.json` | `id`, `type` (CUP/RALLY), `name`, `nameIt`, `order`, `stops` (courseId in ordine), `imageUrl` | 8 cup + 12 rally |
| `mirror_mode.json` | `order`, `text`, `textIt` (traduzione non ufficiale): condizioni di sblocco della modalità specchio | 6 |
| `peach_medallions.json` | `id`, `index`, `x`, `y`, `hint`, `youtubeId` (mkworld-checklist) | 200 |
| `question_panels.json` | come `peach_medallions.json` | 150 |
| `map.json` | un elemento: `imageUrl`, `width`, `height` dell'immagine della mappa | 1 |
| `p_switches.json` | `id`, `index`, `regionId`, `courseId`, `areaId`, `name`; `x`, `y`, `hint`, `youtubeId` da mkworld-checklist | 394 |
| `meta.json` | `seedVersion`, `gameVersion`, `extractedOn`, `origin`, `warnings`, `license`, `sources` (titolo, URL, `revid`) | — |

`meta.json.origin` vale `api` (estrazione via script); `manual-transcription` era il seed iniziale, trascritto a mano prima della fase 2.

### 5.2 Fonti
Tutte da **Super Mario Wiki** (mariowiki.com), contenuti testuali in CC BY-SA 4.0. Le pagine sono fissate per **titolo esatto** in `tools/seedgen/sources.yaml`: lo script non cerca pagine, e se un titolo cambia l'estrazione fallisce.

| Titolo pagina | Cosa se ne estrae |
|---|---|
| `Dash Food` | tabella "List of food": gruppi di cibo → outfit per personaggio; ogni cibo (riga) con livello di boost e immagine; colonna Locations (corsi **e strade vicine**) solo come controllo incrociato |
| `List of Yoshi's locations` | tutti gli stand: "Course locations" (nell'area di un percorso: locale, cibo, luogo) e "Route locations" (sulle strade: cibo, luogo) |
| `List of Mario Kart World missions` | pulsanti P: bioma (intestazione), nome della missione ("In-game text"), percorso ("Location") |
| `Template:Mario Kart World` | navbox del gioco: righe "<Nome> Cup" con i 4 corsi; riga "Knockout Tour rallies" (per accorgersi di rally nuovi) |
| `Golden Rally`, `Ice Rally`, `Moon Rally`, `Spiny Rally`, `Cherry Rally`, `Acorn Rally`, `Cloud Rally`, `Heart Rally`, `Drill Rally`, `Boomerang Rally (rally)`, `Propeller Rally`, `Turnip Rally` | tappe del rally, dalla tabella "Starting point / Checkpoint N / Final course" |

**Mappa, Peach Medallions e pannelli "?": mkworld-checklist.** Dal 28/09/2026, per scelta dell'autore, le posizioni sulla mappa di Monete Peach, Pulsanti P e pannelli "?", le istruzioni (solo inglese), i video e l'immagine della mappa vengono dal progetto pubblico su GitHub [mkworld-checklist](https://github.com/BamisWasTaken/mkworld-checklist) (mktools.io), letto da `seedgen/checklist.py` a un **commit fissato** in `sources.yaml`, con i crediti in README, `seed/LICENSE` e Impostazioni/Info. Le monete e i pannelli vengono solo da lì; i Pulsanti P restano quelli di mariowiki e prendono la posizione per nome della missione (3 grafie diverse in `manual/checklist_mission_names.yaml`, un nome non abbinato ferma l'estrazione). Supera la fonte precedente dei medaglioni (conteggi per bioma di Nintendo Life, file manuale): le monete di mkworld-checklist non hanno un bioma, e un tentativo di ricavarlo dai Pulsanti P vicini non riproduceva i conteggi di Nintendo Life. Niente scraping automatico di siti commerciali (IGN, Game8, Nintendo Life): contenuti protetti, termini d'uso restrittivi e struttura instabile.

- Le guide commerciali (IGN, Game8, Nintendo Life, Gamer Guides) servono **solo per verificare** i dati, non per copiarne i contenuti. IGN è il riferimento dell'autore per il controllo a campione degli outfit (manuale, non automatizzato).
- Le immagini di mariowiki **non** sono CC BY-SA: sono asset Nintendo. seedgen ne registra solo l'URL (`imageUrl` su personaggi, outfit, eventi dalla pagina `Mario Kart World`, sempre dal vivo come le pagine dei nomi in italiano; sui cibi dalla pagina `Dash Food`); l'app le scarica a runtime e non entrano mai nel repository o nell'APK.

### 5.2.1 Regole di dominio verificate sulla pagina Dash Food
- **Tutti i cibi di uno stesso gruppo danno lo stesso outfit**: gli snack non sono combinazioni. Il wiki parla di 18 gruppi ufficiali; per l'app conta la cella Outfits distinta, che dà **20 gruppi** (19 con outfit + Lunchbox).
- Takoyaki, candy apple e Cheep Cheep taiyaki condividono la cella Outfits del sushi: sono un solo gruppo.
- Snacks 1, 2 e 3 hanno outfit diversi ma la stessa colonna Locations: un chiosco di snack può dare uno qualsiasi dei tre.
- Il **lunchbox** non dà outfit: riporta al default (`revertsToDefault = true`, nessuna regola).
- Un personaggio **non elencato** per un gruppo non riceve outfit da quel gruppo.
- Baby Peach, Baby Daisy e Baby Rosalina ricevono sempre gli stessi outfit tra loro; così anche Toad e Toadette (Burger Bud ≡ Soft Server, Engineer ≡ Conductor). Non serve logica speciale in app: è un controllo di coerenza dello script.
- Solo i 24 personaggi del roster principale hanno outfit; gli NPC no.

**Dove si trova un cibo: gli stand Yoshi's** (fase C1, verifiche del 05/10/2026).
- La colonna Locations di Dash Food elenca i percorsi *più le strade vicine* ("courses or surrounding routes"): non basta per dire dove sta uno stand.
- `List of Yoshi's locations` elenca ogni stand: "Course locations" dà quelli **dell'area** di un percorso, anche fuori dal tracciato di gara (Crown City ne ha nove, alcuni "in an alley next to Bank Coin Coffer"), e nessuna fonte dice quali si incontrano in gara; "Route locations" dà quelli sulle strade tra i percorsi. Per questo Consigliami parla di possibilità e gli stand sulle strade non si associano mai a un evento (collegarli a un tratto di rally vorrebbe dire indovinare da una descrizione testuale).
- Controllo incrociato: ogni cibo di uno stand su un percorso compare anche nella colonna Locations di Dash Food per quel corso (vale per tutte le coppie), altrimenti seedgen si ferma.
- Le etichette di cibo della pagina Yoshi's ("Kebabs", "Fish and chips", "Chips, soft drinks, chocolate bars"…) si mappano ai gruppi con `yoshi_food_labels` in `aliases.yaml` (verificate sui nomi dei file immagine di Dash Food, es. MKWorld_Kebab_1 = Barbecue). Uno stand di snack vale per Snacks 1, 2 e 3. Le etichette del gruppo sushi nominano il cibo preciso (takoyaki, mela caramellata, taiyaki, sushi) e lo stand va a quel cibo; le altre nominano il gruppo, non la taglia.
- Per 9 percorsi (Desert Hills, DK Pass, Faraway Oasis, Dino Dino Jungle, Dandelion Depths, Boo Cinema, Choco Mountain, Toad's Factory, Rainbow Road) la pagina non elenca stand: per l'app valgono come "nessuno stand noto", non come "nessun cibo". Carne con l'osso e popcorn hanno stand solo sulle strade, quindi Consigliami non li suggerisce mai.
- Dalla selezione del veicolo si può **disattivare** il cambio d'abito da Dash Food: in quel caso non si sblocca nessun outfit (pagina Dash Food). Lo dice il banner di Consigliami.

### 5.3 Script di estrazione (`tools/seedgen/`, già nel repository)
Lo script **esiste già** ed è coperto da test: non va riscritto, solo esteso quando servono nuove fonti. Dettagli d'uso in `tools/seedgen/README.md`.

**Pipeline:** download (MediaWiki API) → parse → dati grezzi → normalizzazione con `aliases.yaml` → validazione con `expected_counts.yaml` → JSON deterministici.

**Accesso ai dati**
- `https://www.mariowiki.com/api.php?action=parse&page=<titolo>&prop=text|revid&formatversion=2`. Si usa l'HTML restituito dall'API perché la tabella Dash Food ha rowspan/colspan e link-immagine che nel wikitext sono scomodi da interpretare; nell'HTML diventano celle normali e `<a title="Corso">`.
- Richieste sequenziali, `User-Agent` con contatto (da impostare in `sources.yaml`, sostituendo `<REPO_URL>`), `maxlag=5`, retry con backoff su 5xx/429/maxlag, timeout per richiesta e totale.

**Parsing**
- Ogni tabella si cerca per **intestazioni** (es. Name/Outfits/Locations), mai per posizione. Struttura inattesa = errore.
- Nomi di personaggi e corsi normalizzati tramite `aliases.yaml`. Un nome sconosciuto fa **fallire** l'estrazione. I link da ignorare (es. "Mario Circuit 1/2/3" nella Special Cup) sono elencati esplicitamente.
- Se la navbox elenca un rally non presente in `sources.yaml`, l'estrazione fallisce: i rally nuovi si aggiungono consapevolmente.

**Validazione (exit code 2 se una fallisce)**
- Conteggi in `expected_counts.yaml`: 24 personaggi con outfit, 103 outfit alternativi, 127 totali, 20 gruppi di cibo (1 che riporta al default), 60 cibi, 70 stand sui percorsi e 39 sulle strade, 30 corsi, 8 cup da 4 corsi, 12 rally da 6 tappe. Se un update del gioco cambia i numeri, si aggiornano **a mano** in un commit dedicato, mai rilassati per far passare la build.
- Integrità referenziale, ID unici, nessun outfit senza cibo, ogni gruppo con almeno uno stand e una variante, le 8 cup coprono tutti i 30 corsi.
- Equivalenze di §5.2.1 e controllo incrociato stand dei percorsi ⊆ colonna Locations di Dash Food (nel build).
- Stand: bioma uguale a quello del corso, nessun bioma sulle strade, ogni luogo e tipo di locale con la sua traduzione (`manual/stand_locations_it.yaml`: un testo senza voce o una voce inutilizzata fermano seedgen). Cibi: livello di boost riconosciuto, immagine sul CDN del wiki (file originale).
- 10 regioni; ogni corso tranne Rainbow Road in esattamente una regione.
- Mappa (se presente, tutti o nessuno tra `peach_medallions.json`, `question_panels.json`, `map.json`): 200 monete, 150 pannelli, ID unici, ogni punto con `x`/`y` tra 0 e 100, immagine solo su `raw.githubusercontent.com/BamisWasTaken/mkworld-checklist/`.
- Pulsanti P (se presenti): esattamente 394, e ogni missione sta nella regione del proprio percorso. Questo controllo verifica anche l'assegnazione corso→regione in `aliases.yaml`, che per 7 regioni su 10 è ricavata dalla guida Nintendo Life.

**Comandi ed exit code**

| Comando | Uso |
|---|---|
| `validate --seed DIR` | valida un seed esistente, senza rete |
| `fetch-fixtures` | salva le pagine reali in `tests/fixtures/real/` |
| `generate --out DIR [--from-fixtures DIR \| --from-raw FILE]` | genera e valida |
| `check --seed DIR --candidate DIR` | confronta, stampa il diff |
| `accept --seed DIR --candidate DIR` | copia il candidato, incrementa `seedVersion`, stampa la riga per il CHANGELOG |
| `release --seed DIR --work DIR [--offline]` | entry point per Gradle: scarica, genera, valida, confronta |

Exit code: `0` ok/identico · `1` uso o configurazione · `2` dati non validi · `3` validi ma diversi da `seed/` · `4` rete.

**Stato**
- Parser testato su HTML sintetici e sulle pagine reali (`tests/fixtures/real/`, committate; `test_real_fixtures.py`). Le pagine dei nomi in altre lingue e la pagina delle immagini si leggono sempre dal vivo (test live saltati senza rete).
- `seed/` estratto via API (`origin: api`), `seedVersion` 9.

### 5.4 Integrazione nel build Gradle
Lo script gira durante la build, con regole diverse per variante:

| Variante | Comportamento |
|---|---|
| **debug** | Nessuna rete, nessun Python. Usa i JSON versionati in `seed/`. |
| **release** | Task `generateSeed` prima di `mergeReleaseAssets`: esegue `python -m seedgen release --seed <root>/seed --work app/build/seedgen/candidate`. |

Comportamento in base all'exit code di `release`:
- **0**: dati identici a `seed/`, la build prosegue.
- **3**: dati validi ma diversi. La build **si ferma** mostrando il diff stampato dallo script. Con `-PacceptSeedChanges` il task esegue `accept`, inserisce nel CHANGELOG la riga stampata da `accept` (nel formato del CHANGELOG del progetto) e prosegue. Così ogni release è riproducibile dal commit e nessun cambio di dati entra senza essere visto.
- **2**: dati non validi, la build **fallisce**. Non si usano mai dati non validati.
- **4** (rete) o **1** (configurazione): la build **fallisce**. Con `-PofflineSeed` il task passa `--offline`: valida `seed/` versionato, scrive un warning e prosegue.

Requisiti:
- Il task individua `python3` dal PATH o da `-PpythonExec`; se manca, errore chiaro con istruzioni.
- Dipendenze installate da `tools/seedgen/requirements.txt` in un venv sotto `build/` (non nel sistema).
- La parte di rete non è mai cacheable (`outputs.upToDateWhen { false }` o equivalente).
- Il task copia `seed/*.json` negli asset (es. `app/src/main/assets/seed/` generato, non versionato) sia in debug sia in release.
- Documentare nel README: prerequisiti Python, flag disponibili, come aggiornare `expected_counts.yaml` e `sources.yaml`.

### 5.5 Attribuzione
La schermata Info deve riportare: "Dati di gioco tratti da Super Mario Wiki (mariowiki.com) e Super Mario Wiki italiana (mariowiki.it), licenza CC BY-SA 4.0" con link e i `revid` usati (letti da `meta.json`), più la nota che i nomi italiani dei cibi e i luoghi degli stand Yoshi's sono una traduzione non ufficiale e il credito a mkworld-checklist (mktools.io) di BamisWasTaken per la mappa. I file in `seed/` restano sotto CC BY-SA, separati dalla licenza del codice (file `seed/LICENSE`).

### 5.6 Validazione lato app (unit test Kotlin)
Doppio controllo sui JSON effettivamente impacchettati, indipendente dallo script:
- Conteggi letti da `expected_counts.yaml` (stessa fonte di verità dello script).
- Ogni `OutfitFoodRule` punta a outfit e gruppo esistenti; ogni `EventStop` e ogni stand di un percorso a un corso esistente; stand sulle strade senza bioma; i cibi precisi degli stand esistono tra le varianti del gruppo.
- Nessun ID duplicato.
- Se una sorgente non è ancora implementata (es. medaglie), il test relativo viene saltato con un messaggio esplicito, non passa in silenzio.

---

## 6. Algoritmo Consigliami

### 6.1 Cibi che potrebbero esserci in un evento
```
foods(E) = { f.foodGroupId | f ∈ YoshiStandFood, stand(f).courseId ∈ corsi di EventStop(E) }
```
`foods(E)` è un insieme di **gruppi** di cibo, presi dagli stand Yoshi's nell'area dei percorsi toccati (fase C1). Lo stand può non essere sul tracciato di gara, quindi ogni guadagno è una **possibilità** e i testi lo dicono ("potrebbe esserci", "occhi aperti"). Gli stand sulle strade (`courseId` null) non entrano mai in `foods(E)`.

### 6.2 Guadagno per personaggio
Per ogni personaggio C **sbloccato** e ogni evento E:
```
missing(C) = outfit di C non default e non posseduti
gain(E, C) = | { o ∈ missing(C) : ∃ rule(o, f) con f ∈ foods(E) } |
best(E)    = argmax_C gain(E, C)   // tie-break: C con più outfit mancanti totali, poi rosterOrder
total(E)   = Σ_C gain(E, C)
```
Un outfit senza regole cibo note non entra in nessun gain e compare come "cibo sconosciuto" nel dettaglio.

### 6.3 Punteggio
Il criterio primario è **sempre** il gain del miglior singolo personaggio, perché in una run se ne usa uno solo. Non è configurabile.

- **Risultati disattivati:** `score(E) = gain(E, best(E))`.
- **Risultati attivati** (peso `w ∈ [0, 1]`, default 0.3):
  ```
  normGain(E)    = gain(E, best(E)) / max_E gain(E, best(E))    // 0 se max = 0
  improvement(E) = 1 - level(bestRank(E, ccSelezionata)) / 6     // bronzo=1 … oro ★★★=6; nessun trofeo → 1
  // bestRank(E, cc) = trofeo registrato esattamente a cc (§2.6), nessun riporto da altre cilindrate
  score(E)       = (1 - w) * normGain(E) + w * improvement(E)
  ```
  La cilindrata di riferimento si seleziona in Consigliami (default 150cc).
- Eventi con `gain = 0` finiscono in fondo, oppure vengono nascosti con il filtro "Solo utili" (default on).

### 6.4 Pari merito e spareggi
Metriche di supporto:
```
relevantStops(E) = | { s ∈ EventStop(E) : ∃ f ∈ foods(corso di s) (§6.1) che sblocca un outfit in missing(best(E)) } |
worstFirst(E)    = improvement(E)            // solo se i risultati sono attivi
typeRank(E)      = 0 se CUP, 1 se RALLY      // un GP corre un giro intero sul corso; un rally spesso lo attraversa solo in parte
```

**Pari merito:** due eventi condividono la posizione quando hanno lo stesso `score(E)` **e** lo stesso `total(E)`. Numerazione "competition ranking": 1, 1, 1, 4. Il confronto sugli score continui usa una tolleranza di 1e-9.

**Ordinamento completo** (decrescente dove non indicato):
1. `score(E)`
2. `total(E)`
3. `relevantStops(E)`
4. `worstFirst(E)` (saltato se i risultati sono disattivati)
5. `typeRank(E)` crescente (Cup prima di Rally)
6. `Event.order` crescente

I criteri 3–6 ordinano **dentro** il gruppo a pari merito ma non cambiano il numero di posizione.

**Raggruppamento UI:** eventi con la stessa posizione formano un gruppo. `corsiComuni = ∩ dei corsi di E che contengono cibi utili per best(E)`. Se non è vuoto, il titolo del gruppo li elenca. Un gruppo di un solo evento si mostra come card normale.

### 6.5 Test richiesti
- Nessun outfit mancante: tutti i gain valgono 0 e la lista è vuota con "Solo utili".
- Un personaggio bloccato non viene mai consigliato.
- Un outfit posseduto non conta.
- Un cibo presente in due corsi dello stesso evento conta una sola volta per outfit.
- Con `w = 1`, l'ordinamento dipende solo da `improvement`.
- Tie-break deterministico.
- Tre eventi che toccano lo stesso corso "ricco" e nessun altro cibo utile ottengono la stessa posizione e finiscono nello stesso gruppo, con quel corso nel titolo.
- A parità di tutto, una Cup precede un Rally.
- Un evento con più corsi utili (`relevantStops` maggiore) precede uno con meno, a parità di posizione.
- Numerazione competition ranking corretta: 1, 1, 3.
- Uno stand su una strada non dà gain a nessun evento (né cibi rilevanti né dettaglio).
- Più stand con lo stesso cibo sullo stesso percorso contano una volta.
- Il dettaglio dà, per ogni outfit, i cibi e i percorsi dell'evento da cui potrebbe arrivare, in ordine di tappa.

---

## 7. Fuori scope v1
- Sticker (la mappa e i ? Panels sono arrivati dopo la 1.0, §2.4)
- Stato di sblocco di cup e rally (es. Special Cup, rally aggiuntivi)
- Qualsiasi connessione di rete **nell'app** oltre al download delle immagini, mappa compresa (§0, punto 2)

---

## 8. Fasi di implementazione
Il roadmap originale è chiuso; alcuni commenti nel codice citano ancora queste fasi. Le prossime sono in `docs/roadmap.md`.

1. Scaffold · 2. Verifica di seedgen sulle pagine reali · 3. Seed nell'app e build (`generateSeed`, reseed, validazione) · 4. Personaggi (allora "Skin") · 5. Monete Peach e Pulsanti P · 6. Consigliami · 7. Risultati · 8. Backup.

## 9. Criteri di accettazione
- L'app parte e funziona offline; l'unico uso della rete è il download delle immagini.
- Spuntare l'ultimo outfit di un personaggio lo sposta subito in fondo alla griglia e aggiorna il contatore in Home.
- Consigliami si aggiorna in tempo reale quando cambia lo stato degli outfit.
- Un aggiornamento dei seed non perde lo stato utente (test di migrazione).
- Nessun asset Nintendo nel repository.

## 10. Questioni sui dati

**Risolte**
- Snack combinati: non esistono. Tutti i cibi di un gruppo danno lo stesso outfit (§5.2.1).
- Totale outfit: 127 con i default, 103 alternativi, su 24 personaggi. Conteggio sulla pagina Dash Food, coerente con Nintendo Life.
- Update 1.8.0: nessun outfit o personaggio nuovo (confermato dall'autore); ha aggiunto 2 rally, già presenti tra i 12.
- Tappe dei rally: estratte dalle pagine dei singoli rally.
- Stato di sblocco iniziale dei personaggi: dalle gallerie del wiki (§2.3); poi lo gestisce l'utente.
- Cibo per percorso: dagli stand di List of Yoshi's locations, con il loro luogo (§5.2.1, fase C1). Quali stand si incontrano in gara non lo dice nessuna fonte: Consigliami parla di possibilità.
- Pulsanti P: da `List of Mario Kart World missions` (mariowiki).
- Peach Medallions e ? Panels: da mkworld-checklist, con la mappa (§5.2).

**Aperte (non bloccano l'implementazione)**
- **9 percorsi senza stand noti** (§5.2.1): se giocando se ne trova uno, va segnalato su mariowiki; poi la prossima estrazione lo porta nel seed.
- **Controllo a campione degli outfit su IGN**: manuale, a cura dell'autore. Nintendo Life dà abbinamenti cibo→outfit diversi da mariowiki (es. Mario Aviator), ma mariowiki cita la guida ufficiale giapponese: in caso di dubbio fa fede la prova in gioco.
- **Stand sulle strade tra i percorsi**: nel seed dalla fase C1; si vedranno nella sezione Cibi (fase C2, `docs/roadmap.md`), mai associati a un evento.

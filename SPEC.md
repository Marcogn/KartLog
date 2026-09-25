# KartLog — Specifica tecnica (v0.4)

Tracker Android offline per i collectibles di Mario Kart World: outfit (skin), Peach Medallions, P Switch, con un modulo "Consigliami" che suggerisce quale Gran Premio o Knockout Tour correre e con quale personaggio per sbloccare più outfit mancanti.

> Nome di lavoro: "KartLog". Non usare "Mario", "Mario Kart" o "Nintendo" nel nome dell'app, nell'icona o nel package.

---

## 0. Regole per chi implementa (leggere prima di tutto)

1. **Non inventare mai dati di gioco.** Personaggi, outfit, cibi, cup, rally, percorsi, medaglie e P Switch arrivano **solo** dai file seed JSON (§5), generati da `tools/seedgen`. Non modificare `seed/` a mano. Se un dato manca, lascialo vuoto o `null` e aggiungi un `TODO` nel JSON. Non usare valori plausibili al suo posto.
2. **Nessun asset Nintendo nel repository**: niente render, loghi, font o screenshot. Le immagini dei personaggi sono placeholder (iniziali su sfondo colorato) finché non viene deciso diversamente. Il codice deve prevedere uno slot immagine opzionale (`imageRes: String?`).
3. **App completamente offline**: nessun permesso INTERNET, nessuna analytics. La rete si usa **solo a build time**, nello script di estrazione dati (§5).
4. Lavora per fasi (§8). Alla fine di ogni fase il progetto deve compilare e i test devono passare.

---

## 1. Stack

> **Allineamento:** stack, struttura Gradle, signing e pipeline di release devono ricalcare **ThePatientGamerHelper** e le app gemelle. Dove questa sezione e il progetto di riferimento divergono, vince il progetto di riferimento, salvo per quanto richiesto esplicitamente da questa spec.

- Kotlin, Jetpack Compose, Material 3
- Navigation Compose
- Room per lo stato utente e i dati seed
- Hilt per la DI
- kotlinx.serialization per il parsing dei seed JSON
- Architettura MVVM: ViewModel + StateFlow, repository, UseCase per la logica di Consigliami
- minSdk 26, targetSdk ultimo stabile
- Test: JUnit per l'algoritmo di raccomandazione e la validazione seed, test Compose UI solo per i flussi principali
- Keystore fisso per mantenere sempre lo stesso SHA1, varianti debug e release, README e CHANGELOG sempre aggiornati (come nel progetto di riferimento)
- Python 3 (solo tooling di build, §5) con dipendenze fissate in `tools/seedgen/requirements.txt`

---

## 2. Navigazione e UI

### 2.1 Struttura
- `ModalNavigationDrawer` con hamburger in alto a sinistra, sempre disponibile.
- Voci del drawer: **Home**, **Skin**, **Monete Peach**, **Pulsanti P**, **Consigliami**, più in fondo **Impostazioni / Info** (crediti e licenze dei dati, §5.5).

### 2.2 Home
- Griglia 2×2 di pulsanti **quadrati** e grandi: Skin, Monete Peach, Pulsanti P, Consigliami.
- Ogni pulsante mostra icona, titolo e un contatore di progresso (es. `87 / 127`, `143 / 200`). Consigliami non ha contatore.
- Il look deve essere colorato e "da gioco": palette vivace, angoli arrotondati generosi, tipografia bold. Non deve imitare la UI ufficiale.

### 2.3 Skin
**Schermata lista personaggi**
- `LazyVerticalGrid` a **2 colonne**. Le righe dipendono dal numero di personaggi **che hanno almeno un outfit alternativo**.
- Ogni cella mostra lo slot immagine (placeholder), il nome e un contatore `ottenuti/totali`.
- Se il personaggio ha **tutti** gli outfit, la cella è attenuata (nome e card con alpha ridotto o colori desaturati) e si ordina in fondo. L'ordinamento è configurabile: roster ufficiale / alfabetico / % completamento.
- Filtro in alto: Tutti / Incompleti.

**Schermata dettaglio personaggio**
- Header con nome, contatore e switch **"Personaggio sbloccato"**, necessario per Consigliami (§6). Default: sbloccato. Il seed non contiene lo stato di sblocco (il wiki non lo espone in forma estraibile), quindi è l'utente a disattivare i personaggi che non ha.
- Lista outfit: ogni riga ha checkbox, nome outfit e **tutti** i gruppi di cibo che lo sbloccano (es. Mario Touring: "Hamburger · Barbecue · Moo Moo Milk"). Se non ci sono regole note, mostra "cibo sconosciuto".
- L'outfit di default non è una riga spuntabile: è sempre posseduto e non si conta nei mancanti.

### 2.4 Monete Peach e Pulsanti P
Entrambe le liste usano le **10 regioni** del mondo di gioco (`regions.json`), in sezioni collassabili. Ogni intestazione mostra il nome della regione e `x/y`; l'azione "segna tutti" di una sezione chiede conferma. In alto c'è il contatore globale (`x/200`, `x/394`).

**Monete Peach**
- Per ogni regione, checkbox numerate ("Medaglione 1…N"), senza descrizione: il seed contiene solo i conteggi per regione (§5.2).
- Pulsante "Apri guida" che apre nel browser l'URL `source` di `peach_medallions.json` (Intent `ACTION_VIEW`: non richiede il permesso INTERNET).

**Pulsanti P**
- Dentro ogni regione, sottogruppi per percorso (o luogo, es. Chain Chomp Desert).
- Ogni riga mostra il nome della missione (testo in-game dal wiki) e il percorso.
- Ricerca testuale sul nome della missione.
- Finché `p_switches.json` non esiste (seed iniziale, §5.3 "Stato") la schermata mostra "Dati non ancora disponibili" invece di una lista vuota.

### 2.5 Consigliami
Vedi §6 per l'algoritmo. UI:
- Toggle: **Gran Premi** / **Knockout Tour** / **Entrambi**.
- Lista ordinata per punteggio (decrescente), con **posizioni a pari merito** (1, 1, 1, 4…), come da §6.4.
- Gli eventi a pari merito si raggruppano in **una card espandibile**. Se hanno in comune i corsi che generano il guadagno, il titolo li nomina: "4 eventi passano da Crown City · stesso guadagno". All'interno del gruppo restano ordinati con gli spareggi §6.4.
- Ogni card evento mostra:
  - nome evento, tipo (Cup/Rally) e posizione in classifica
  - **personaggio consigliato** e numero di outfit nuovi ottenibili con lui
  - i 2 personaggi alternativi successivi con i rispettivi numeri
  - i cibi rilevanti disponibili sull'evento, ciascuno con il percorso e l'indicazione **sul percorso** / **nei dintorni** (§5.2.1)
  - se i risultati sono attivi, il miglior risultato registrato (stelle e posizione)
- Tap sulla card apre il dettaglio: tutti i personaggi con gain > 0, outfit specifici ottenibili per ciascuno, e il pulsante **"Registra risultato"**.
- Sezione "Pesa i risultati": switch on/off più slider del peso (§6.3).
- Switch **"Includi cibi nei dintorni"** (default off): se attivo, il calcolo usa anche la presenza `NEARBY` (§6.1).
- Banner fisso in basso: "Stima basata sugli stand Yoshi's dei percorsi: non garantisce che il bag compaia o che tu riesca a prenderlo."

### 2.6 Registrazione risultati
Form (bottom sheet) con evento, cilindrata (50/100/150/Mirror), **stelle 0–3**, posizione finale (GP: 1–24; KO: 1–24 oppure "eliminato al checkpoint N"), personaggio usato (opzionale) e data (auto).
Storico risultati per evento con possibilità di cancellare una voce.

---

## 3. Modello dati (Room)

Dati seed (read-only, ricaricati a ogni cambio di `seedVersion`):

Le entità ricalcano i JSON di `seed/` (§5.1), che esistono già nel repository.

```
Character(id: String PK, name: String, rosterOrder: Int, imageRes: String?)
Outfit(id: String PK, characterId: FK, name: String?, isDefault: Boolean)
    // id = "<character>__<outfit>": i nomi NON sono unici tra personaggi ("Pro Racer").
    // Il default ha id "<character>__default" e name null (il wiki non lo nomina): in UI "Standard".
FoodGroup(id: String PK, name: String, foods: List<String>, revertsToDefault: Boolean)
    // Un gruppo = una cella Outfits distinta della tabella Dash Food (20 gruppi).
    // foods: nomi dei cibi che lo compongono, es. ["Takoyaki","Candy apple","Cheep Cheep taiyaki","Sushi"].
OutfitFoodRule(outfitId: FK, foodGroupId: FK)              // N:M: lo stesso outfit si ottiene da più gruppi (es. Mario Touring)
FoodGroupCourse(foodGroupId: FK, courseId: FK, presence: ON_COURSE|NEARBY, listedInDashFood: Boolean)
    // ON_COURSE: stand sul tracciato (List of Yoshi's locations). NEARBY: solo sulle strade vicine (Dash Food). §5.2.1
Course(id: String PK, name: String, regionId: FK?)            // null solo per Rainbow Road
Region(id: String PK, name: String, order: Int)               // 10 regioni/biomi
Area(id: String PK, name: String, regionId: FK)               // luoghi delle missioni che non sono uno dei 30 corsi
PeachMedallion(id: String PK, regionId: FK, index: Int)       // 200, da file manuale (§5.2)
PSwitch(id: String PK, index: Int, regionId: FK, courseId: FK?, areaId: FK?, name: String)  // 394, esattamente uno tra courseId e areaId
Event(id: String PK, type: CUP|RALLY, name: String, order: Int)
EventStop(eventId: FK, position: Int, courseId: FK)         // nel JSON è l'array "stops" dell'evento, in ordine
```

Previsti per la v2, **non nel seed**: gli stand Yoshi's sulle strade tra i percorsi (`YoshiSpot`, `EventRouteSpot`).

Stato utente (mai toccato dal reseed):

```
OwnedOutfit(outfitId PK)
CharacterUnlock(characterId PK, unlocked: Boolean)
CollectedMedallion(medallionId PK)
CompletedPSwitch(pSwitchId PK)
RaceResult(id PK auto, eventId, cc, stars: Int, placement: Int?, eliminatedAt: Int?, characterId: String?, timestamp: Long)
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
| `characters.json` | `id`, `name`, `rosterOrder` | 24 |
| `outfits.json` | `id`, `characterId`, `name` (null per il default), `isDefault` | 127 (103 + 24 default) |
| `food_groups.json` | `id`, `name`, `foods`, `revertsToDefault` | 20 |
| `outfit_food_rules.json` | `outfitId`, `foodGroupId` | — |
| `food_group_courses.json` | `foodGroupId`, `courseId`, `presence` (ON_COURSE/NEARBY), `listedInDashFood` | 54 sul percorso + 52 nei dintorni |
| `courses.json` | `id`, `name`, `regionId` | 30 |
| `regions.json` | `id`, `name`, `order` | 10 |
| `areas.json` | `id`, `name`, `regionId` | 1 (cresce se le missioni citano altri luoghi) |
| `events.json` | `id`, `type` (CUP/RALLY), `name`, `order`, `stops` (courseId in ordine) | 8 cup + 12 rally |
| `peach_medallions.json` | `id`, `regionId`, `index`; `manual: true` a livello di file | 200 |
| `p_switches.json` | `id`, `index`, `regionId`, `courseId`, `areaId`, `name` | 394 (**assente nel seed iniziale**, arriva con la prima estrazione) |
| `meta.json` | `seedVersion`, `gameVersion`, `extractedOn`, `origin`, `warnings`, `license`, `sources` (titolo, URL, `revid`) | — |

`meta.json.origin` vale `manual-transcription` per il seed iniziale e `api` dopo la prima estrazione via script (§5.3, "Stato").

### 5.2 Fonti
Tutte da **Super Mario Wiki** (mariowiki.com), contenuti testuali in CC BY-SA 4.0. Le pagine sono fissate per **titolo esatto** in `tools/seedgen/sources.yaml`: lo script non cerca pagine, e se un titolo cambia l'estrazione fallisce.

| Titolo pagina | Cosa se ne estrae |
|---|---|
| `Dash Food` | tabella "List of food": gruppi di cibo → outfit per personaggio; gruppi → corsi **e strade vicine** (colonna Locations) |
| `List of Yoshi's locations` | sezione "Course locations": quali cibi hanno uno stand **sul tracciato** di ogni percorso |
| `List of Mario Kart World missions` | pulsanti P: bioma (intestazione), nome della missione ("In-game text"), percorso ("Location") |
| `Template:Mario Kart World` | navbox del gioco: righe "<Nome> Cup" con i 4 corsi; riga "Knockout Tour rallies" (per accorgersi di rally nuovi) |
| `Golden Rally`, `Ice Rally`, `Moon Rally`, `Spiny Rally`, `Cherry Rally`, `Acorn Rally`, `Cloud Rally`, `Heart Rally`, `Drill Rally`, `Boomerang Rally (Mario Kart World)`, `Propeller Rally`, `Turnip Rally` | tappe del rally, dalla tabella "Starting point / Checkpoint N / Final course" |

**Peach Medallions: fonte manuale.** Nessuna fonte con licenza libera li elenca (la pagina mariowiki dà solo il totale). Poiché stanno nel mondo aperto, ogni guida li raggruppa a modo suo: Gamer Guides per percorso (somma 199), Nintendo Life per le 10 regioni (somma 200). Si usano i **conteggi per regione di Nintendo Life**, in `tools/seedgen/manual/peach_medallions.yaml`, copiando solo i numeri. Niente scraping automatico di siti commerciali (IGN, Game8, Nintendo Life): contenuti protetti, termini d'uso restrittivi e struttura instabile. Per cambiare raggruppamento si modifica quel file a mano.

- Le guide commerciali (IGN, Game8, Nintendo Life, Gamer Guides) servono **solo per verificare** i dati, non per copiarne i contenuti. IGN è il riferimento dell'autore per il controllo a campione degli outfit (manuale, non automatizzato).
- Le immagini di mariowiki **non** sono CC BY-SA: sono asset Nintendo e non vengono mai scaricate.

### 5.2.1 Regole di dominio verificate sulla pagina Dash Food
- **Tutti i cibi di uno stesso gruppo danno lo stesso outfit**: gli snack non sono combinazioni. Il wiki parla di 18 gruppi ufficiali; per l'app conta la cella Outfits distinta, che dà **20 gruppi** (19 con outfit + Lunchbox).
- Takoyaki, candy apple e Cheep Cheep taiyaki condividono la cella Outfits del sushi: sono un solo gruppo.
- Snacks 1, 2 e 3 hanno outfit diversi ma la stessa colonna Locations: un chiosco di snack può dare uno qualsiasi dei tre.
- Il **lunchbox** non dà outfit: riporta al default (`revertsToDefault = true`, nessuna regola).
- Un personaggio **non elencato** per un gruppo non riceve outfit da quel gruppo.
- Baby Peach, Baby Daisy e Baby Rosalina ricevono sempre gli stessi outfit tra loro; così anche Toad e Toadette (Burger Bud ≡ Soft Server, Engineer ≡ Conductor). Non serve logica speciale in app: è un controllo di coerenza dello script.
- Solo i 24 personaggi del roster principale hanno outfit; gli NPC no.

**Dove si trova un cibo: "sul percorso" o "nei dintorni".** La colonna Locations di Dash Food elenca i percorsi *più le strade vicine* (la pagina dice "courses or surrounding routes"). Esempio: lo Spicy curry risulta a Mario Bros. Circuit, ma lo stand è alla stazione a nord del circuito. Per affermare "il sushi sta su Great ? Block Ruins" serve la pagina `List of Yoshi's locations`, che separa gli stand sul tracciato da quelli sulle strade. Quindi:
- `ON_COURSE`: il gruppo ha uno stand sul tracciato del percorso (sezione Course locations).
- `NEARBY`: il percorso compare solo nella colonna Locations di Dash Food.
- Controllo incrociato: ogni coppia `ON_COURSE` deve comparire anche in Dash Food. Sul seed iniziale lo è per tutte le 54 coppie.
- Le etichette di cibo della pagina Yoshi's ("Kebabs", "Fish and chips", "Chips, soft drinks, chocolate bars"…) si mappano ai gruppi con `yoshi_food_labels` in `aliases.yaml`. Uno stand di snack vale per Snacks 1, 2 e 3.
- Per 9 percorsi (Desert Hills, DK Pass, Faraway Oasis, Dino Dino Jungle, Dandelion Depths, Boo Cinema, Choco Mountain, Toad's Factory, Rainbow Road) la pagina non elenca stand sul tracciato: per l'app valgono come "nessuno stand noto sul percorso", non come "nessun cibo". La UI lo dice esplicitamente (§10).

### 5.3 Script di estrazione (`tools/seedgen/`, già nel repository)
Lo script **esiste già** ed è coperto da test: non va riscritto, solo verificato sulle pagine reali (fase 2) ed esteso quando servono nuove fonti. Dettagli d'uso in `tools/seedgen/README.md`.

**Pipeline:** download (MediaWiki API) → parse → dati grezzi → normalizzazione con `aliases.yaml` → validazione con `expected_counts.yaml` → JSON deterministici.

**Accesso ai dati**
- `https://www.mariowiki.com/api.php?action=parse&page=<titolo>&prop=text|revid&formatversion=2`. Si usa l'HTML restituito dall'API perché la tabella Dash Food ha rowspan/colspan e link-immagine che nel wikitext sono scomodi da interpretare; nell'HTML diventano celle normali e `<a title="Corso">`.
- Richieste sequenziali, `User-Agent` con contatto (da impostare in `sources.yaml`, sostituendo `<REPO_URL>`), `maxlag=5`, retry con backoff su 5xx/429/maxlag, timeout per richiesta e totale.

**Parsing**
- Ogni tabella si cerca per **intestazioni** (es. Name/Outfits/Locations), mai per posizione. Struttura inattesa = errore.
- Nomi di personaggi e corsi normalizzati tramite `aliases.yaml`. Un nome sconosciuto fa **fallire** l'estrazione. I link da ignorare (es. "Mario Circuit 1/2/3" nella Special Cup) sono elencati esplicitamente.
- Se la navbox elenca un rally non presente in `sources.yaml`, l'estrazione fallisce: i rally nuovi si aggiungono consapevolmente.

**Validazione (exit code 2 se una fallisce)**
- Conteggi in `expected_counts.yaml`: 24 personaggi con outfit, 103 outfit alternativi, 127 totali, 20 gruppi di cibo (1 che riporta al default), 30 corsi, 8 cup da 4 corsi, 12 rally da 6 tappe. Se un update del gioco cambia i numeri, si aggiornano **a mano** in un commit dedicato, mai rilassati per far passare la build.
- Integrità referenziale, ID unici, nessun outfit senza cibo, ogni gruppo con almeno un corso, le 8 cup coprono tutti i 30 corsi.
- Equivalenze di §5.2.1 e controllo incrociato `ON_COURSE` ⊆ Dash Food.
- 10 regioni; ogni corso tranne Rainbow Road in esattamente una regione; 200 medaglioni che coprono tutte le regioni.
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
- Parser testato su HTML **sintetici** costruiti sulla struttura delle pagine; non ancora sulle pagine reali (il wiki non era raggiungibile dall'ambiente di scrittura).
- `seed/` iniziale generato con la stessa pipeline da una trascrizione manuale delle pagine (`tests/golden/raw_manual_2026-09-25.yaml`). La trascrizione non include i 394 pulsanti P: arrivano con la prima estrazione via API.
- `tests/test_real_fixtures.py` confronta parser e trascrizione non appena esistono le fixture reali: è il test di accettazione della fase 2.

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
La schermata Info deve riportare: "Dati di gioco tratti da Super Mario Wiki (mariowiki.com), licenza CC BY-SA 4.0" con link e i `revid` usati (letti da `meta.json`). I file in `seed/` restano sotto CC BY-SA, separati dalla licenza del codice (file `seed/LICENSE`).

### 5.6 Validazione lato app (unit test Kotlin)
Doppio controllo sui JSON effettivamente impacchettati, indipendente dallo script:
- Conteggi letti da `expected_counts.yaml` (stessa fonte di verità dello script).
- Ogni `OutfitFoodRule` punta a outfit e gruppo esistenti; ogni `EventStop` e `FoodGroupCourse` a un corso esistente.
- Nessun ID duplicato.
- Se una sorgente non è ancora implementata (es. medaglie), il test relativo viene saltato con un messaggio esplicito, non passa in silenzio.

---

## 6. Algoritmo Consigliami

### 6.1 Cibi disponibili per evento
```
presenze = {ON_COURSE}                       // default
presenze = {ON_COURSE, NEARBY}               // se "Includi cibi nei dintorni" è attivo
foods(E) = { fgc.foodGroupId | fgc ∈ FoodGroupCourse, fgc.courseId ∈ corsi di EventStop(E), fgc.presence ∈ presenze }
```
`foods(E)` è un insieme di **gruppi** di cibo. Di default conta solo ciò che sta sul tracciato dei percorsi toccati, così un consiglio come "il sushi sta su Great ? Block Ruins" è sempre vero. Gli stand sulle strade tra i percorsi sono v2.

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
  improvement(E) = 1 - bestStars(E, ccSelezionata) / 3           // nessun risultato → 1
  score(E)       = (1 - w) * normGain(E) + w * improvement(E)
  ```
  La cilindrata di riferimento si seleziona in Consigliami (default 150cc).
- Eventi con `gain = 0` finiscono in fondo, oppure vengono nascosti con il filtro "Solo utili" (default on).

### 6.4 Pari merito e spareggi
Metriche di supporto:
```
relevantStops(E) = | { s ∈ EventStop(E) : ∃ f ∈ foods(corso di s), con le stesse presenze di §6.1, che sblocca un outfit in missing(best(E)) } |
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
- Un cibo presente solo `NEARBY` su un percorso non genera gain con "Includi cibi nei dintorni" spento, e lo genera se acceso.

---

## 7. Fuori scope v1
- Immagini ufficiali dei personaggi
- Mappa interattiva
- Sticker e ? Panels (possibile v2, con la stessa struttura di medaglioni e pulsanti P)
- Stato di sblocco di cup e rally (es. Special Cup, rally aggiuntivi)
- Qualsiasi connessione di rete **nell'app** (la rete serve solo al build release, §5.4)

---

## 8. Fasi di implementazione

1. **Scaffold**: progetto allineato a ThePatientGamerHelper (Gradle, signing, varianti, README, CHANGELOG), Hilt, Room, navigation, drawer, Home 2×2 con contatori fittizi. Tema Material 3 con palette custom.
2. **Verifica di seedgen sulle pagine reali**: lo script e il seed iniziale esistono già (§5.3). Impostare lo User-Agent, eseguire `fetch-fixtures`, far passare `test_real_fixtures.py` correggendo il parser se legge male, poi `generate` + `accept` per passare a `origin: api`. Richiede rete verso mariowiki.com: se l'ambiente non ce l'ha, la esegue l'autore in locale.
3. **Seed nell'app e build**: modelli serializzabili sui JSON reali di `seed/`, loader da assets, reseed con `seedVersion`, test di validazione (§5.6), task Gradle `generateSeed` con le regole di §5.4. Seed fittizio per i test solo in `src/test`, marcato `"source": "FAKE_FOR_TESTS"`.
4. **Skin**: griglia 2 colonne, dettaglio con checkbox e switch di sblocco, attenuazione dei completati, filtri.
5. **Monete Peach e Pulsanti P**: liste per regione (§2.4), contatori, ricerca, "segna tutti", link alla guida. I dati sono già nel seed (pulsanti P dopo la fase 2).
6. **Consigliami**: UseCase con l'algoritmo §6 e test, UI lista e dettaglio.
7. **Risultati**: form, storico, integrazione del peso.
8. **Backup**: export/import dello stato utente.

## 9. Criteri di accettazione
- L'app parte offline e senza permessi di rete.
- Spuntare l'ultimo outfit di un personaggio lo attenua subito nella griglia e aggiorna il contatore in Home.
- Consigliami si aggiorna in tempo reale quando cambia lo stato degli outfit.
- Un aggiornamento dei seed non perde lo stato utente (test di migrazione).
- Nessun asset Nintendo nel repository.

## 10. Questioni sui dati

**Risolte**
- Snack combinati: non esistono. Tutti i cibi di un gruppo danno lo stesso outfit (§5.2.1).
- Totale outfit: 127 con i default, 103 alternativi, su 24 personaggi. Conteggio sulla pagina Dash Food, coerente con Nintendo Life.
- Update 1.8.0: nessun outfit o personaggio nuovo (confermato dall'autore); ha aggiunto 2 rally, già presenti tra i 12.
- Tappe dei rally: estratte dalle pagine dei singoli rally.
- Stato di sblocco dei personaggi: non estraibile; default sbloccato, lo gestisce l'utente (§2.3).
- Cibo per percorso: distinzione `ON_COURSE` / `NEARBY` (§5.2.1). La posizione esatta dello stand sul percorso non serve per ora.
- Pulsanti P: da `List of Mario Kart World missions` (mariowiki).
- Peach Medallions: conteggi per regione da file manuale (§5.2).

**Aperte (non bloccano l'implementazione)**
- **9 percorsi senza stand noti sul tracciato** (§5.2.1): se giocando se ne trova uno, va segnalato su mariowiki; poi la prossima estrazione lo porta nel seed.
- **Controllo a campione degli outfit su IGN**: manuale, a cura dell'autore. Nintendo Life dà abbinamenti cibo→outfit diversi da mariowiki (es. Mario Aviator), ma mariowiki cita la guida ufficiale giapponese: in caso di dubbio fa fede la prova in gioco.
- **Stand sulle strade tra i percorsi** (v2).

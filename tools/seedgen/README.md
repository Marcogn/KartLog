# seedgen

Estrae i dati di gioco di KartLog da **Super Mario Wiki** e genera i JSON in `seed/`. Specifica completa: `SPEC.md` §5.

## Da dove prende i dati

Le pagine sono elencate **per titolo esatto** in `sources.yaml`: lo script non cerca nulla sul wiki.

| Pagina | Cosa se ne ricava |
|---|---|
| `Dash Food` | tabella "List of food": gruppi di cibo → outfit per personaggio; ogni cibo (una riga = una variante, con livello di boost e URL dell'immagine); colonna Locations solo come controllo incrociato degli stand |
| `List of Yoshi's locations` | tutti gli stand Yoshi's, uno per riga: "Course locations" (percorso, tipo di locale, cibo, luogo) e "Route locations" (strade tra i percorsi: cibo e luogo) |
| `List of Mario Kart World missions` | pulsanti P: bioma, nome della missione, percorso |
| `Template:Mario Kart World` | navbox: gli 8 cup con i loro 4 corsi, e l'elenco dei rally (per accorgersi di rally nuovi) |
| `Golden Rally`, `Ice Rally`, … (12 pagine) | tappe di ogni Knockout Tour, dalla tabella "Starting point … Final course" |
| `Mario Kart World` | solo gli **URL** delle immagini di personaggi, outfit, cup e rally, quali piloti sono disponibili dall'inizio, la tabella "Unlock criteria" e le condizioni della modalità specchio, senza note tra parentesi (`seedgen/images.py`); scaricata sempre dal vivo |
| mkworld-checklist (GitHub, commit fissato in `sources.yaml`): `checklist-data.json`, `i18n/en.json`, intestazione di `map.webp` | posizioni di Monete Peach, Pulsanti P e pannelli "?", istruzioni in inglese, video YouTube, URL e misure della mappa (`seedgen/checklist.py`); sempre dal vivo. I Pulsanti P si abbinano a quelli di mariowiki per nome della missione |
| mariowiki.it: `Mario Kart World`, `Lista delle missioni di Mario Kart World` | nomi italiani che mariowiki.com non ha: piloti senza outfit, biomi, Trofei e rally, e la tabella "Criteri di sblocco" (`seedgen/it_wiki.py`); sempre dal vivo |

`manual/food_names_it.yaml` contiene i nomi italiani dei cibi (gruppi e singoli cibi): **traduzione non ufficiale**, scelta dell'autore perché nessuna fonte li riporta. `manual/stand_locations_it.yaml` traduce allo stesso modo luoghi e tipi di locale degli stand Yoshi's. `manual/mirror_mode_it.yaml` traduce allo stesso modo le condizioni della modalità specchio (mariowiki.it ne ha una versione meno aggiornata). Ogni voce riporta anche il testo inglese esatto: se sul wiki cambia, seedgen si ferma finché la traduzione non viene aggiornata.

Le pagine si scaricano con la MediaWiki API (`https://www.mariowiki.com/api.php?action=parse&prop=text|revid`). Ogni estrazione registra il `revid` di ogni pagina in `seed/meta.json`. Le immagini non vengono mai scaricate: seedgen ne salva solo l'URL (`imageUrl`), l'app le scarica a runtime.

Le pagine dei nomi in italiano e la pagina `Mario Kart World` si scaricano solo in una `generate`/`release` dal vivo, mai da fixture (sono grandi e se ne usa una piccola parte): con `--from-fixtures` o `--from-raw` i campi `nameIt` e `imageUrl` restano `null`. Lo stesso per mkworld-checklist: senza, mancano `peach_medallions.json`, `question_panels.json`, `map.json` e le posizioni dei Pulsanti P (la validazione accetta tutti o nessuno dei tre file).

## File

| File | Ruolo |
|---|---|
| `sources.yaml` | titoli delle pagine, URL dell'API, User-Agent, timeout |
| `aliases.yaml` | nome del wiki → slug stabile per personaggi e corsi; equivalenze da validare. Un nome sconosciuto fa fallire l'estrazione |
| `expected_counts.yaml` | conteggi attesi e versione del gioco. Fonte di verità anche per i test Kotlin |
| `manual/checklist_mission_names.yaml` | le 3 missioni scritte in modo diverso da mkworld-checklist e da mariowiki, verificate a mano |
| `tests/golden/raw_manual_2026-09-25.yaml` | trascrizione manuale delle pagine, usata per generare il primo `seed/` e come controllo incrociato del parser |
| `tests/fixtures/synthetic/` | HTML sintetici che riproducono la struttura delle pagine, per i test del parser |
| `tests/fixtures/real/` | pagine reali salvate con `fetch-fixtures` (da creare, vedi sotto) |

## Uso

```bash
cd tools/seedgen
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt

python -m pytest                                    # test (senza rete)
python -m seedgen validate --seed ../../seed        # valida il seed versionato
python -m seedgen fetch-fixtures                    # scarica le pagine reali in tests/fixtures/real/
python -m seedgen generate --from-fixtures tests/fixtures/real --out /tmp/cand --dump-raw /tmp/raw.yaml
python -m seedgen check --seed ../../seed --candidate /tmp/cand
python -m seedgen accept --seed ../../seed --candidate /tmp/cand
python -m seedgen release --seed ../../seed --work ../../app/build/seedgen/candidate [--offline]
```

`release` è l'entry point del task Gradle `generateSeed`. Exit code: `0` ok, `1` uso/configurazione, `2` dati non validi, `3` dati validi ma diversi da `seed/`, `4` rete.

## Prima di usarlo con la rete

1. In `sources.yaml` sostituire `<REPO_URL>` nello `user_agent` con l'URL del repository o un contatto.
2. Eseguire `fetch-fixtures` e poi `pytest`. Si attiva `test_real_fixtures.py`, che confronta il parser con la trascrizione manuale:
   - se passa, il parser legge correttamente le pagine reali;
   - se fallisce, il messaggio mostra le differenze: vanno capite (parser sbagliato o wiki aggiornato) prima di accettare qualsiasi cosa.

## Stato

- Il parser è stato verificato sia su HTML sintetici (`tests/fixtures/synthetic/`) sia sulle 16 pagine reali del wiki (`tests/fixtures/real/`, committate — vedi `test_real_fixtures.py`).
- `seed/` viene da un'estrazione reale via API (`origin: api` in `meta.json`, `seedVersion 2`). La trascrizione manuale (`tests/golden/raw_manual_2026-09-25.yaml`) resta come riferimento storico e come fixture per `test_seed.py` (esclusi i pulsanti P, che non copriva).
- `seed/p_switches.json` esiste con le 394 voci.
- Stand Yoshi's (`yoshi_stands.json`, anche sulle strade) e cibi uno per uno (`food_variants.json`) dalla fase C1 (`seedVersion 9`): sostituiscono `food_group_courses.json`, che `accept` cancella da `seed/`.

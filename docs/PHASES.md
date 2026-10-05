# Fasi di implementazione — definizione operativa

Dettaglio di SPEC §8. Per ogni fase: obiettivo, prerequisiti, cosa è fuori scope e quando la fase è finita.
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.
I punti "Fatto quando" si aggiungono sempre a quelli comuni in fondo al file.

---

## Fase 1 — Scaffold
Descritta in `PROMPT_FASE1.md`.

---

## Fase 2 — Verifica di seedgen sulle pagine reali
**Obiettivo:** passare da un seed trascritto a mano (`origin: manual-transcription`) a uno estratto dall'API (`origin: api`), con i 394 pulsanti P.

**Prerequisiti:**
- Rete verso `www.mariowiki.com`. **Verificalo come prima cosa** con `python -m seedgen fetch-fixtures`. Se l'ambiente non la ha, fermati e scrivi all'autore i comandi da lanciare in locale (vedi sotto): non aggirare il blocco.
- In `tools/seedgen/sources.yaml`, `<REPO_URL>` sostituito con l'URL del repository. Se non lo conosci, chiedilo.

**Passi:**
1. `python -m seedgen fetch-fixtures`, poi `python -m pytest`.
2. Se `test_real_fixtures.py` fallisce, classifica ogni differenza:
   - **il parser legge male la pagina** → correggi il parser e aggiungi un caso alle fixture sintetiche che riproduca il problema;
   - **il wiki è cambiato o la trascrizione era incompleta** (es. stand con cibo che nella trascrizione mancava) → non toccare il parser, annota la differenza.
3. Se l'estrazione fallisce per un nome sconosciuto (personaggio, corso, luogo di una missione, etichetta di cibo), aggiungilo ad `aliases.yaml` **solo dopo averlo verificato sulla pagina**. Per un luogo nuovo, indica la regione giusta.
4. Se la validazione segnala una missione nella regione sbagliata, correggi l'assegnazione corso→regione in `aliases.yaml` (per 7 regioni su 10 era ricavata da Nintendo Life).
5. `generate --from-fixtures tests/fixtures/real --out <tmp>`, poi `check`: presenta all'autore il diff completo e **aspetta la sua conferma** prima di `accept`.

**Fuori scope:** codice Android, task Gradle.

**Fatto quando:**
- `seed/meta.json` ha `origin: api` e i `revid` di tutte le pagine.
- `seed/p_switches.json` esiste con 394 voci.
- `pytest` passa, incluso `test_real_fixtures.py`, eventualmente con la trascrizione golden aggiornata e il motivo annotato.
- Il riepilogo elenca ogni differenza tra trascrizione ed estrazione, con la sua classificazione.

**Se serve eseguirla in locale (comandi per l'autore):**
```bash
cd tools/seedgen && python3 -m venv .venv && . .venv/bin/activate && pip install -r requirements.txt
python -m seedgen fetch-fixtures && python -m pytest
```
Poi l'autore riporta l'output all'agente e si riprende dal passo 2.

---

## Fase 3 — Seed nell'app e build
**Obiettivo:** l'app carica i dati reali da `seed/` e il build release esegue seedgen.

**Prerequisiti:** fase 1 completata. La fase 2 non è bloccante: il seed trascritto a mano è valido. Se la fase 2 non è ancora fatta, `p_switches.json` può mancare e il codice deve gestirlo.

**Include:**
- Entità Room definitive (SPEC §3) e DAO. Modelli kotlinx.serialization sui JSON reali.
- Copia di `seed/*.json` negli asset in debug e release.
- Reseed a ogni cambio di `seedVersion`, senza toccare lo stato utente. Serve un test di migrazione: stato utente presente → reseed → stato intatto.
- Test di validazione lato app (SPEC §5.6), con i conteggi letti da `tools/seedgen/expected_counts.yaml`.
- Task Gradle `generateSeed` con le regole di SPEC §5.4 (exit code, `-PacceptSeedChanges`, `-PofflineSeed`, `-PpythonExec`, venv sotto `build/`).
- Contatori della Home collegati ai dati reali (conteggi totali; lo stato utente è ancora vuoto).

**Fuori scope:** schermate Skin, Monete, Pulsanti P, Consigliami (solo placeholder).

**Fatto quando:**
- `./gradlew assembleDebug` funziona senza rete e senza Python.
- `./gradlew assembleRelease -PofflineSeed` funziona senza rete.
- Una modifica simulata ai dati fa fermare la build release con il diff (test del task o verifica documentata).
- La Home mostra `0 / 127`, `0 / 200` e `0 / 394` (oppure "n/d" se mancano i pulsanti P).

---

## Fase 4 — Skin
**Obiettivo:** SPEC §2.3 completa.

**Include:** griglia a 2 colonne, dettaglio con checkbox, switch "Personaggio sbloccato" (default sbloccato), attenuazione e ordinamento dei completati, filtri, ordinamenti, contatore in Home aggiornato in tempo reale. Per ogni outfit, tutti i gruppi di cibo che lo sbloccano.

**Fuori scope:** Consigliami, anche se i dati dei cibi sono già a disposizione.

**Fatto quando:**
- Spuntare l'ultimo outfit di un personaggio lo attenua subito e aggiorna la Home (criterio di SPEC §9).
- Test UI del flusso "spunta outfit → contatore aggiornato".

---

## Fase 5 — Monete Peach e Pulsanti P
**Obiettivo:** SPEC §2.4 completa.

**Include:** liste per regione, sottogruppi per percorso per i pulsanti P, contatori, "segna tutti" con conferma, ricerca sui nomi delle missioni, pulsante "Apri guida" per i medaglioni (Intent `ACTION_VIEW`, senza permesso INTERNET). Stato "Dati non ancora disponibili" se `p_switches.json` manca.

**Fuori scope:** descrizioni dei medaglioni (non esistono nel seed, e non vanno aggiunte).

**Fatto quando:** i contatori di Home e schermata coincidono; lo stato sopravvive a riavvio dell'app e a reseed.

---

## Fase 6 — Consigliami
**Obiettivo:** SPEC §2.5 e §6, senza la registrazione dei risultati.

**Include:** UseCase puro, senza dipendenze Android, con l'algoritmo §6.1–6.4. Tutti i test di §6.5 scritti **prima** della UI. Lista con pari merito e raggruppamento, dettaglio evento, toggle GP/KT/Entrambi, switch "Includi cibi nei dintorni", indicazione "sul percorso"/"nei dintorni" per ogni cibo, banner.

**Fuori scope:** registrazione dei risultati e peso `w` (fase 7). Il codice prevede `w = 0` finché non esistono risultati.

**Fatto quando:** i test §6.5 passano; Consigliami si aggiorna in tempo reale quando cambiano outfit o sblocchi (criterio di SPEC §9).

---

## Fase 7 — Risultati
**Obiettivo:** SPEC §2.6 e il peso dei risultati in §6.3.

**Include:** bottom sheet di registrazione, storico per evento con cancellazione, switch e slider del peso, selezione della cilindrata di riferimento, miglior risultato mostrato sulle card.

**Fatto quando:** il test "con `w = 1` l'ordinamento dipende solo da `improvement`" passa sull'app reale, non solo sullo UseCase.

---

## Fase 8 — Backup
**Obiettivo:** SPEC §4.

**Include:** export e import JSON dello **stato utente** tramite Storage Access Framework. Formato versionato. L'import valida gli ID contro il seed corrente e riporta quelli sconosciuti invece di scartarli in silenzio.

**Fatto quando:** export → reinstallazione → import ripristina tutto; test su un file con un ID non più esistente.

---

## Revisione di Consigliami e sezione Cibi (fasi C1–C4, dal 05/10/2026)
Richiesta dell'autore dopo una prova in gioco: i consigli non corrispondono agli outfit che si sbloccano davvero correndo i Gran Premi e i Knockout Tour. Le fasi C vengono dopo il roadmap 1–8 e hanno un **modello assegnato**: per prima cosa controllalo (passo 0 del protocollo in `CLAUDE.md`).

| Fase | Modello | In breve |
|---|---|---|
| C1 Dati degli stand | **Opus** | seedgen estrae ogni stand Yoshi's (percorso o strada, cibo, luogo EN + traduzione IT manuale); tabella Room e migrazione; Consigliami solo sugli stand dei percorsi, con testi "potrebbe esserci", senza "Includi cibi nei dintorni" |
| C2 Sezione Cibi | Sonnet | Griglia di cibi con la grafica dell'app (sottosezione di Personaggi); toccando un cibo: dove si trova e outfit sbloccati per personaggio |
| C3 Grafica della lista | Sonnet | Lista di Consigliami e controlli con i componenti `Kart*` |
| C4 Dettaglio evento | Sonnet | Dettaglio con grafica `Kart*` e righe "outfit – cibo – percorso" |

### Diagnosi (05/10/2026)
Il calcolo fa quello che dice SPEC §6 (letto in `ConsigliamiUseCase`); il problema è che §6.1 presenta come certo ciò che i dati non sanno. Fatti verificati sul wiki il 05/10/2026:
- Il cibo → outfit è deterministico: "Outfits the player obtains are based on the food item that was consumed" (pagina [Dash Food](https://www.mariowiki.com/Dash_Food)). Le regole in `outfit_food_rules.json` non sono il sospettato principale (C1 le ricontrolla a campione).
- La stessa pagina: dalla selezione del veicolo si può **disattivare** il cambio d'abito da Dash Food; in quel caso nessun outfit si sblocca.
- In un Gran Premio solo la prima gara è a giri; le altre tre partono dalla strada (route) dal percorso precedente. Il Knockout Tour "focuses primarily on the routes between the courses" (pagina [Mario Kart World](https://www.mariowiki.com/Mario_Kart_World), sezioni Grand Prix e Knockout Tour).
- Gli stand di "Course locations" ([List of Yoshi's locations](https://www.mariowiki.com/List_of_Yoshi%27s_locations)) sono quelli **dell'area** del percorso, anche fuori dal tracciato di gara (Crown City: dieci, alcuni "in an alley next to Bank Coin Coffer"). La pagina non dice quali si incontrano in gara (todo "Add descriptions of exact course locations"), e lo stesso percorso ha tracciati diversi a seconda dell'evento (pagina [Crown City](https://www.mariowiki.com/Crown_City)).
- Il gruppo `wild_bone` (Carne con l'osso) ha un solo stand, su una strada ("East gate of Crown Bridge"): con l'impostazione di default non viene mai consigliato.

### Decisioni dell'autore (05/10/2026)
- Consigliami non promette: "su questo percorso c'è un Yoshi's con l'hamburger, occhi aperti: potrebbe essere sul tracciato". Gli stand si prendono soprattutto in corsa libera, quindi serve una **sezione Cibi** che metta in relazione ogni cibo con i luoghi dove si trova.
- Fonte: List of Yoshi's locations di mariowiki (CC BY-SA), **non** IGN (vedi "mai scraping automatico di IGN…" in Decisioni prese; IGN resta solo per il controllo a mano dell'autore).
- Gli stand sulle strade compaiono solo nella sezione Cibi, mai associati a un evento: collegarli a un rally richiederebbe di indovinare il tratto da una descrizione testuale (regola 5).
- Descrizioni dei luoghi in doppia lingua: inglese dal wiki, italiano tradotto a mano e dichiarato **non ufficiale**, come i nomi dei cibi.
- "Includi cibi nei dintorni" si toglie (con la sezione Cibi sarebbe ridondante).
- La sezione Cibi non è un collezionabile: è una sottosezione di Personaggi.

### C1 — Dati degli stand (Opus)
**Obiettivo:** il seed conosce ogni stand Yoshi's con il suo luogo; Consigliami dice solo ciò che i dati sanno.

**Prerequisiti:** PR del piano unita; rete verso mariowiki.com (se manca, fermati).

**Include:**
- Confermare la diagnosi e ricontrollare a campione `outfit_food_rules.json` contro la pagina Dash Food (equivalenze Toad/Toadette e bebè, cibi che riportano all'abito base, mappatura delle etichette degli stand ai gruppi, p. es. "Kebabs" → `barbecue`, "Fish and chips" → `fritters`, "Baked goods" → `bread`; i "Meat" → `wild_bone` e "Popcorn" vanno verificati, non dedotti).
- seedgen: estrazione di **tutti** gli stand di List of Yoshi's locations, sia "Course locations" sia "Route locations": id stabile, gruppo di cibo, percorso (`courseId`) oppure strada, tipo di locale (snack bar, drive-through, food truck…), luogo in inglese, fonte. Le celle con `rowspan` e le celle Location vuote vanno gestite (luogo assente ≠ inventato). Fixture reale aggiornata e test sintetici.
- `manual/stand_locations_it.yaml`: traduzione italiana non ufficiale di ogni luogo (e del tipo di locale), con il testo EN esatto come chiave, così seedgen si ferma se il wiki lo cambia (stesso schema di `manual/mirror_mode_it.yaml`). Dichiarata in README, `seed/LICENSE` e Impostazioni/Info.
- Conteggio atteso degli stand in `expected_counts.yaml`, contato sulla pagina (commit dedicato che cita la revisione).
- Seed rigenerato e accettato dall'autore (`check` → diff → conferma → `accept`), `seedVersion` +1, entità e migrazione Room (mai distruttiva) con test; backup invariato (nessuno stato utente nuovo).
- Consigliami: `foods(E)` dagli stand dei percorsi dell'evento (niente più `NEARBY`); tolti switch, preferenza e stringhe di "Includi cibi nei dintorni"; SPEC §6 e §6.5 aggiornati (via il test `NEARBY`, nuovo test: uno stand su una strada non dà gain a nessun evento); testi di lista e dettaglio riformulati come possibilità ("potrebbe esserci", "occhi aperti"), senza rifare la grafica.
- Immagini dei cibi per la griglia di C2: le pagine Dash Food ha un'immagine per ogni cibo, ma la decisione del 26/09/2026 copre solo le immagini della pagina "Mario Kart World". **Chiedi all'autore** se estenderla (solo URL nel seed, validati sul prefisso `https://mario.wiki.gallery/images/`, scaricati a runtime); se no, nessuna immagine.
- Bioma degli stand: per i percorsi da `courses.regionId`; per le strade solo se la fonte lo dice, mai dedotto dalla descrizione.
- API per C2 e C4: stand per cibo (percorsi e strade, con bioma) e, per evento e personaggio, gli outfit con cibo e percorso.

**Fuori scope:** la schermata Cibi (C2) e la grafica (C3, C4).

**Fatto quando:** pytest, test JVM, lint, `assembleDebug` e `assembleRelease -PofflineSeed` verdi; **handoff** (max 15 righe) per C2–C4 in "Stato attuale". **Controlli a schermo:** il testo di Consigliami parla di possibilità; lo switch dei dintorni non c'è più; un utente con dati vecchi apre l'app senza perdere nulla.

### C2 — Sezione Cibi (Sonnet)
**Obiettivo:** una griglia di cibi; toccando un cibo si vede dove trovarlo e quali outfit sblocca per ciascun personaggio. Stessa grafica del resto dell'app (`Kart*`, cielo, banner).

**Prerequisiti:** C1 unita (usa la sua API).

**Include:**
- `ui/food/`, `Destination.Food` (griglia) e `Destination.FoodDetail(foodGroupId)` (dettaglio); nel drawer come sottovoce di Personaggi (stile `SubItem` di "Collezionabili"; un accesso anche dalla schermata Personaggi va proposto all'autore, non aggiunto da sé).
- **Griglia:** una tessera per gruppo di cibo, come le tessere della Home/le polaroid di Personaggi (`KartPanel`, `OutlinedTitle`, colonne adattive come Personaggi), nome nella lingua dell'app (traduzione non ufficiale) e quanti outfit ti mancano tra quelli che dà (`KartCounterPill`). I cibi **non** si ingrigiscono mai. Immagine del cibo solo se C1 l'ha messa nel seed (vedi C1); altrimenti un segnaposto disegnato, mai file immagine.
- **Dettaglio:** in cima i luoghi, divisi in "Sui percorsi" (bioma › percorso › luogo dello stand) e "Sulle strade" (bioma se ricavabile dalla fonte, poi luogo), luogo nella lingua dell'app (`localizedName`, `relocalizing()`); sotto, un `KartPanel` per personaggio con avatar e gli outfit che quel cibo gli sblocca (polaroid piccole di `ui/skin/Polaroid.kt`): solo gli **outfit** seguono la regola dei colori dell'app (grigio = non ce l'hai, colorato = ce l'hai), come nel dettaglio di Personaggi. I personaggi a cui il cibo non dà outfit (o che riporta all'abito base) in fondo, in una riga di testo.
- IT/EN, tema chiaro e scuro.

**Fatto quando:** lint, test JVM e `assembleDebug` verdi; test Robolectric di griglia e dettaglio. **Controlli a schermo:** griglia in tema chiaro e scuro e su schermo stretto; un cibo con tanti luoghi (hamburger), uno solo su strada (carne con l'osso); outfit grigi/colorati coerenti con Personaggi; IT/EN; in corsa libera, uno stand indicato si trova dove dice la descrizione.

### C3 — Grafica della lista (Sonnet)
**Obiettivo:** la schermata Consigliami ha la stessa grafica del resto dell'app.

**Include:** card degli eventi come `KartPanel` (icona dell'evento, posizione con `KartBadge`, miglior personaggio con avatar, cibi utili), gruppi a pari merito, switch con `KartSwitchRow`, scelta GP/KO e cilindrata con `KartChoiceButton` (mai in `horizontalScroll`), slider del peso dentro un `KartPanel`, testi informativi con `KartInfoButton`/`KartPopup` (che spiega anche "occhi aperti: gli stand sono nell'area del percorso, non per forza sul tracciato"), tema chiaro e scuro, IT/EN. Nessun cambio di logica.

**Fuori scope:** il dettaglio evento (C4).

**Fatto quando:** lint, test JVM e `assembleDebug` verdi; test Robolectric della lista (niente `BoxWithConstraints` dove servono misure intrinseche, vedi `KartDropdownTest`). **Controlli a schermo:** lista in tema chiaro e scuro, schermo stretto, gruppi a pari merito, switch e slider.

### C4 — Dettaglio evento (Sonnet)
**Obiettivo:** toccando un GP o un rally si vede, per ogni personaggio, quali outfit potrebbero sbloccarsi e dove: "Filibustiere – Barbecue – Spiaggia di Peach".

**Prerequisiti:** C2 e C3 unite (riusa i loro componenti).

**Include:** `KartPanel` per personaggio con avatar; una riga "outfit – cibo – percorso" per outfit (più percorsi per lo stesso outfit nella stessa riga); avviso "occhi aperti" una volta in cima, non su ogni riga; tocco sul cibo → sezione Cibi su quel cibo; sezione risultati per cilindrata con la grafica di Risultati. IT/EN.

**Fatto quando:** lint, test JVM e `assembleDebug` verdi; test Robolectric del dettaglio. **Controlli a schermo:** un GP e un rally contro il gioco, tema scuro, nomi lunghi, IT/EN, salto alla sezione Cibi e ritorno.

---

## Comuni a tutte le fasi
- `./gradlew assembleDebug`, i test Android e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano comandi o prerequisiti.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, aperti) e nuove voci in "Decisioni prese".
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni dalla SPEC e perché, cosa resta aperto.

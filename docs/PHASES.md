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

## Revisione di Consigliami (fasi C1–C4, dal 05/10/2026)
Richiesta dell'autore dopo una prova in gioco: i consigli non corrispondono agli outfit che si sbloccano davvero correndo i Gran Premi e i Knockout Tour. Le fasi C sono successive al roadmap 1–8 e hanno un **modello assegnato**: prima di tutto controllalo (passo 0 del protocollo in `CLAUDE.md`).

| Fase | Modello | In breve |
|---|---|---|
| C1 Verifica e modello dei dati | **Opus** | Perché i consigli sono sbagliati, da quale fonte prendere "che cosa incontri in gara", nuovo modello dati. Nessun codice dell'app |
| C2 Dati e algoritmo | **Opus** | seedgen, seed, Room, `ConsigliamiUseCase` e test sul nuovo modello |
| C3 Grafica della lista | Sonnet | Lista di Consigliami e controlli con i componenti `Kart*` |
| C4 Dettaglio evento | Sonnet | Dettaglio con grafica `Kart*` e righe "outfit – cibo – dove" |

### Diagnosi preliminare (05/10/2026, da confermare in C1)
Il calcolo fa quello che dice SPEC §6 (letto in `ConsigliamiUseCase`); il problema è il **modello** di §6.1, che considera un evento come l'elenco dei suoi percorsi e gli assegna tutti i cibi degli stand Yoshi's di quei percorsi. Fatti verificati sul wiki il 05/10/2026:
- Il cibo → outfit è deterministico: "Outfits the player obtains are based on the food item that was consumed" (pagina [Dash Food](https://www.mariowiki.com/Dash_Food)). Le regole in `outfit_food_rules.json` quindi non sono il sospettato principale (C1 le ricontrolla comunque a campione).
- La stessa pagina: dalla selezione del veicolo si può **disattivare** il cambio d'abito da Dash Food; in quel caso nessun outfit si sblocca. Da escludere con l'autore prima di tutto.
- In un Gran Premio solo la prima gara è a giri; le altre tre sono "a sezioni" con la strada (route) dal percorso precedente. Il Knockout Tour "focuses primarily on the routes between the courses" e solo l'ultima gara è un giro sul percorso finale (pagina [Mario Kart World](https://www.mariowiki.com/Mario_Kart_World), sezioni Grand Prix e Knockout Tour). Ma `foods(E)` ignora del tutto gli stand sulle strade ("Route locations" di [List of Yoshi's locations](https://www.mariowiki.com/List_of_Yoshi%27s_locations), oggi esclusi come "v2").
- Gli stand di "Course locations" sono quelli **dell'area** del percorso, anche fuori dal tracciato di gara: per Crown City ne risultano dieci, alcuni "in an alley next to Bank Coin Coffer" o "West exit of Crown City across West Greens Park". La pagina ha ancora il todo "Add descriptions of exact course locations": non dice quali stand si incontrano in gara.
- Lo stesso percorso ha tracciati diversi a seconda dell'evento (Crown City nel Trofeo Fungo parte dal Crown Bridge e percorre la variante del Trofeo Guscio al contrario, pagina [Crown City](https://www.mariowiki.com/Crown_City)): gli stand incontrati dipendono dall'evento e dalla gara, non solo dal percorso.
- Conseguenze nel seed attuale: il gruppo `wild_bone` (Carne con l'osso, unico stand "East gate of Crown Bridge", tra le route) non è `ON_COURSE` da nessuna parte, quindi i suoi outfit non vengono mai consigliati con l'impostazione di default; Crown City (Trofea) "regala" dieci gruppi di cibo a ogni evento che la tocca.

### C1 — Verifica e modello dei dati (Opus)
**Obiettivo:** dire con fonti che cosa è sbagliato e decidere con l'autore come rappresentare "gli stand che incontri correndo l'evento E".

**Prerequisiti:** nessuno di codice. Rete verso mariowiki.com (se manca, fermati).

**Passi:**
1. Confermare o smentire ogni punto della diagnosi qui sopra; aggiungere quello che manca (equivalenze Toad/Toadette e bebè, cibi che riportano all'abito base, `NEARBY`, mappatura stand → gruppo di cibo in seedgen, p. es. "Kebabs" → `barbecue`).
2. Riprodurre almeno 3 casi concreti dell'autore (chiedigli evento, personaggio e che cosa ha visto in gioco) e spiegare ciascuno.
3. Cercare una fonte per "stand sul tracciato di gara per evento/gara": pagine dei singoli percorsi e delle route su mariowiki, pagine dei rally, mariowiki.it, mkworld-checklist (che ha già posizioni sulla mappa, ma va verificato se ha gli stand). Regola 5: se nessuna fonte libera lo dice, **non dedurlo dalla geometria né "a occhio"**: presenta le alternative all'autore (p. es. tabella manuale compilata da lui in gioco, come `manual/food_names_it.yaml`, dichiarata nel README) e aspetta la sua scelta.
4. Progettare il modello: granularità stand (id stabile, gruppo di cibo, percorso o route, descrizione del luogo, fonte) e appartenenza a ogni gara di ogni evento; che cosa resta di `ON_COURSE`/`NEARBY` e dello switch "Includi cibi nei dintorni"; impatto su `seedVersion`, migrazione Room (mai distruttiva), backup.
5. Aggiornare SPEC §6 (e §3 se cambiano le entità) e scrivere `docs/consigliami-verifica.md`: casi, cause, fonti, decisioni.

**Fuori scope:** codice dell'app e di seedgen (salvo script usa-e-getta nello scratchpad per l'analisi), grafica.

**Fatto quando:** `docs/consigliami-verifica.md` e SPEC §6 aggiornati; fonte dei dati scelta dall'autore e annotata in "Decisioni prese"; **handoff** per C2 (max 15 righe) in "Stato attuale" di `CLAUDE.md`.

### C2 — Dati e algoritmo (Opus)
**Obiettivo:** il seed e l'algoritmo seguono il modello di C1.

**Prerequisiti:** C1 chiusa e unita; dati della fonte scelta disponibili (se è la tabella dell'autore, compilata).

**Include:** seedgen (estrazione, validazione, conteggi attesi solo se citano una fonte, test con fixture sintetiche e reali), seed rigenerato e accettato dall'autore (`check` → diff → conferma → `accept`), entità e migrazione Room con test, `ConsigliamiUseCase` aggiornato con i test di SPEC §6.5 rivisti e nuovi casi (uno stand fuori dal tracciato non conta; uno stand su una route dell'evento conta), API per C4: per ogni personaggio e outfit sbloccabile, il cibo e il luogo (percorso o route, gara dell'evento) dove trovarlo.

**Fuori scope:** grafica (C3, C4). La UI attuale deve solo continuare a compilare e funzionare.

**Fatto quando:** pytest, test JVM, lint e `assembleDebug` verdi; `assembleRelease -PofflineSeed` verde; i 3 casi di C1 danno il risultato atteso in un test sul seed reale; handoff per C3/C4 in "Stato attuale".

### C3 — Grafica della lista (Sonnet)
**Obiettivo:** la schermata Consigliami ha la stessa grafica del resto dell'app.

**Include:** card degli eventi come `KartPanel` (icona dell'evento, posizione con `KartBadge`, miglior personaggio con avatar, cibi utili), gruppi a pari merito, switch con `KartSwitchRow`, scelta GP/KO e cilindrata con `KartChoiceButton` (mai in `horizontalScroll`), slider del peso dentro un `KartPanel`, testi informativi con `KartInfoButton`/`KartPopup`, tema chiaro e scuro, stringhe IT/EN. Nessun cambio di logica.

**Fuori scope:** il dettaglio evento (C4).

**Fatto quando:** lint, test JVM e `assembleDebug` verdi; un test Robolectric della lista (anche con `DropdownMenu`/righe `IntrinsicSize`: niente `BoxWithConstraints`, vedi `KartDropdownTest`). **Controlli a schermo:** lista in tema chiaro e scuro, schermo stretto, gruppi a pari merito, switch e slider.

### C4 — Dettaglio evento (Sonnet)
**Obiettivo:** toccando un GP o un rally si vede, per ogni personaggio, quali outfit si sbloccano e dove: "Filibustiere – Barbecue – Spiaggia di Peach" (formato esatto da confermare con l'autore all'inizio della fase: con o senza cibo, con la gara dell'evento).

**Prerequisiti:** C2 unita (usa la sua API), C3 unita (riusa i suoi componenti).

**Include:** dettaglio con `KartPanel` per personaggio, outfit a polaroid piccola (`ui/skin/Polaroid.kt`) o riga con avatar, riga "outfit – cibo – luogo" localizzata (`localizedName`, cibi con traduzione non ufficiale), più luoghi per lo stesso outfit raggruppati, sezione risultati per cilindrata con la grafica di Risultati.

**Fatto quando:** lint, test JVM e `assembleDebug` verdi; test Robolectric del dettaglio. **Controlli a schermo:** un GP e un rally contro il gioco (gli outfit si sbloccano dove indicato), tema scuro, nomi lunghi, IT/EN.

---

## Comuni a tutte le fasi
- `./gradlew assembleDebug`, i test Android e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano comandi o prerequisiti.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, aperti) e nuove voci in "Decisioni prese".
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni dalla SPEC e perché, cosa resta aperto.

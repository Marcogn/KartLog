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

## Comuni a tutte le fasi
- `./gradlew assembleDebug`, i test Android e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano comandi o prerequisiti.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, aperti) e nuove voci in "Decisioni prese".
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni dalla SPEC e perché, cosa resta aperto.

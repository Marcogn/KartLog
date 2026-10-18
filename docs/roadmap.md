# Roadmap

Le prossime fasi di lavoro. Ogni fase ha un **modello assegnato**: una sessione che parte con "vai avanti con la prossima fase" lo controlla per prima cosa (passo 0 del protocollo in `CLAUDE.md`). Le fasi chiuse escono da questo file (restano nella cronologia di git e, per le scelte, in `docs/decisioni.md`): il roadmap originale 1–8 è chiuso dalla 0.1.x.
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

## Repository in inglese (fasi I1–I2, dal 17/10/2026)
Issue #36, richiesta dell'autore dell'11/10/2026 (una issue per app): chi arriva sul repository lo legge in inglese. L'app resta bilingue (IT predefinito, EN) e non cambia. Scelte in `docs/decisioni.md` ("Toolchain, build e CI", "Lingua del repository"). Sono traduzioni senza scelte di progetto: Sonnet.
Ultime fasi chiuse: A1 e P1 (12/10/2026), nella cronologia di git e in `docs/decisioni.md`.

| Fase | Modello | In breve |
|---|---|---|
| I1 | Sonnet | `SPEC.md`, `docs/decisioni.md` → `docs/decisions.md` (con tutti i rimandi), `docs/roadmap.md`, `REVIEW.md`, `tools/seedgen/README.md`, `seed/README.md` in inglese |
| I2 | Sonnet | `README.md`, `CHANGELOG.md` (tutto, anche lo storico), riga del CHANGELOG di seedgen in inglese, `CLAUDE.md` per ultimo; chiude #36 |

### I1 — Specifica e documenti di progetto in inglese (Sonnet)
**Obiettivo:** i documenti di riferimento (specifica, decisioni, roadmap, revisione, seedgen) in inglese, senza perdere né cambiare contenuto.

**Prerequisiti:** questo piano nel `main`.

**Include**
1. `SPEC.md` (~44 KB): traduzione fedele, stessa struttura e stessi numeri di sezione (§…), perché codice e documenti li citano.
2. `docs/decisioni.md` → `docs/decisions.md` con `git mv` (cronologia conservata), tradotto. Aggiornati **tutti** i rimandi al vecchio nome: documenti, `CHANGELOG.md`, `app/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `UpdatePreferences.kt`, `tools/seedgen/seedgen/build.py` (nei commenti si cambia solo il nome del file, il commento resta in italiano). `.claude/skills/next-phase` accetta già entrambi i nomi: non si tocca (è del kit).
3. `docs/roadmap.md` (compresa questa sezione e i "Comuni a tutte le fasi"), `REVIEW.md`, `tools/seedgen/README.md`, `seed/README.md` (una riga, non è un dato di gioco né un file generato: seedgen scrive solo i `*.json`).
4. Nomi di stringhe, comandi, chiavi, nomi propri e citazioni di testi dell'app (es. "Consigliami", "Non ora") restano come sono; i testi della UI citati si possono affiancare dalla versione `values-en/`.

**Fuori scope:** `README.md`, `CHANGELOG.md`, `CLAUDE.md` (I2); commenti nel codice (restano in italiano, decisione del 17/10/2026); stringhe della UI; nomi nel seed (`nameIt`).

**Fatto quando**
- Nessun rimando a `decisioni.md` rimasto (`grep -r decisioni` vuoto fuori da `.claude/` e dalla cronologia); i link relativi funzionano.
- Nessun contenuto perso: stesse sezioni e stessi elenchi del testo italiano (confronto sezione per sezione nella revisione).
- I punti comuni qui sotto; CI verde (solo documenti e commenti).
- **Controlli a schermo:** nessuno (l'app non cambia). Sul sito di GitHub: i documenti si leggono e i link portano dove devono.

### I2 — README, CHANGELOG e CLAUDE.md in inglese (Sonnet)
**Obiettivo:** la pagina del repository, il CHANGELOG (e quindi le note delle release) e la memoria dell'agente in inglese; d'ora in poi commit, PR e issue in inglese.

**Prerequisiti:** I1 nel `main`.

**Include**
1. `README.md` in inglese.
2. `CHANGELOG.md` **tutto** in inglese, storico compreso (scelta dell'autore), stessa convenzione `- **Summary.** detail` che `release.yml` legge. Le note delle release già pubblicate su GitHub restano in italiano.
3. `tools/seedgen/seedgen/cli.py`: la riga pronta per il CHANGELOG ("Dati di gioco aggiornati da…") in inglese, con un test pytest che la controlla; `app/build.gradle.kts` la inserisce senza modifiche.
4. `CLAUDE.md` per ultimo, tradotto: "Convenzioni" diventa "documentation, CHANGELOG, commit, PR and issues in English; UI IT (`values/`) + EN (`values-en/`); code comments may stay Italian"; tolta l'eccezione per `docs/ci.md`/`docs/claude.md`. Resta sotto ~15 KB (oggi 15,4 KB: si accorcia "Stato attuale" spostando i dettagli delle fasi chiuse in `docs/decisions.md` o togliendoli se già lì).
5. `docs/decisions.md`: la voce "Divergenze: README/CHANGELOG in italiano" sostituita dalla decisione del 17/10/2026.

**Fuori scope:** commenti nel codice e messaggi di Gradle/seedgen per lo sviluppatore (a parte la riga del CHANGELOG); note delle release già pubblicate; una terza lingua; dialogo delle novità bilingue (scartato il 17/10/2026).

**Fatto quando**
- Test pytest della riga del CHANGELOG; i punti comuni qui sotto; CI verde.
- `Fixes #36` nella PR.
- **Controlli a schermo** (alla prima release dopo I2): con l'app in italiano il dialogo di aggiornamento mostra i titoli delle novità in inglese, senza testo tagliato o vuoto; "Changelog completo" apre il CHANGELOG in inglese.

**Domande aperte:** nessuna.
**Rischi:** traduzione che cambia il senso di una regola di `CLAUDE.md` o di una decisione (mitigazione: revisione sezione per sezione col testo italiano accanto, nel diff della PR); link interrotti dal cambio di nome (mitigazione: grep nel "Fatto quando"); `CLAUDE.md` che sfora i 15 KB.

---

## Comuni a tutte le fasi
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano funzioni, comandi o prerequisiti; `SPEC.md` aggiornata se cambia il comportamento dell'app.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, handoff se fase Opus); scelte non ovvie in `docs/decisioni.md`; la fase chiusa esce da questo file.
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni e perché, cosa resta aperto, controlli a schermo.

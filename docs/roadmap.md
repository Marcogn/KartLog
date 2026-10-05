# Roadmap

Le prossime fasi di lavoro. Ogni fase ha un **modello assegnato**: una sessione che parte con "vai avanti con la prossima fase" lo controlla per prima cosa (passo 0 del protocollo in `CLAUDE.md`). Le fasi chiuse escono da questo file (restano nella cronologia di git e, per le scelte, in `docs/decisioni.md`): il roadmap originale 1–8 è chiuso dalla 0.1.x.
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

## Prossime fasi
Nessuna fase aperta: R1 Rifiniture (Risultati con i `Kart*`, cibi toccabili nel dettaglio personaggio, il "?" nei nomi dei percorsi) è chiusa il 05/10/2026; le scelte sono in `docs/decisioni.md` (Dati, Personaggi, Risultati). Le prossime le decide l'autore: ogni fase ha un **modello assegnato** da controllare per prima cosa (passo 0 del protocollo in `CLAUDE.md`).
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

---

## Comuni a tutte le fasi
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano funzioni, comandi o prerequisiti; `SPEC.md` aggiornata se cambia il comportamento dell'app.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, handoff se fase Opus); scelte non ovvie in `docs/decisioni.md`; la fase chiusa esce da questo file.
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni e perché, cosa resta aperto, controlli a schermo.

# Roadmap

Le prossime fasi di lavoro. Ogni fase ha un **modello assegnato**: una sessione che parte con "vai avanti con la prossima fase" lo controlla per prima cosa (passo 0 del protocollo in `CLAUDE.md`). Le fasi chiuse escono da questo file (restano nella cronologia di git e, per le scelte, in `docs/decisioni.md`): il roadmap originale 1–8 è chiuso dalla 0.1.x.
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

## Rifiniture (fase R1, dal 05/10/2026)
Ultima fase richiesta dall'autore dopo C1–C4 (chiuse il 05/10/2026: scelte in `docs/decisioni.md`, sezione Consigliami). Ha un **modello assegnato**: per prima cosa controllalo (passo 0 del protocollo in `CLAUDE.md`).

| Fase | Modello | In breve |
|---|---|---|
| R1 Rifiniture | Sonnet | Risultati con la grafica `Kart*`, dal cibo dell'outfit alla sezione Cibi, il "?" nei nomi dei percorsi |

### R1 — Rifiniture (Sonnet)
**Obiettivo:** chiudere le tre cose rimaste aperte dopo C4, senza cambiare dati di gioco né algoritmo.

**Prerequisiti:** PR di C3/C4 unita (#32).

**Include:**
1. **Risultati con la grafica del gioco.** `ui/results/ResultsScreen.kt` usa ancora `ListItem`, `AlertDialog`, `RadioButton` e `TextButton` Material. Al loro posto: righe evento come `KartPanel` (icona `EventIcon(outlined = true)`, nome con `OutlinedTitle`, trofeo in una `KartCounterPill`), intestazioni di sezione con `OutlinedTitle`, scelta del trofeo in un `KartPopup` con un `KartChoiceButton` per trofeo più "Nessun trofeo" (mai in `horizontalScroll`). Le schede `KartTabs` e la "i" dello Specchio ci sono già. Nessun cambio di logica (un solo `TrophyRank` per evento e cilindrata, SPEC §2.6). Stessa grafica per la tessera dei trofei nel dettaglio di Consigliami, se serve per coerenza.
2. **Dal cibo alla sezione Cibi.** Nel dettaglio personaggio (`ui/skin/SkinDetailScreen.kt`) ogni outfit mostra già i cibi che lo sbloccano come testo unico (`localizedFoodGroups`). Ogni cibo diventa toccabile e apre `Destination.FoodDetail(foodGroupId)`, come nel dettaglio di Consigliami; servono gli ID dei cibi oltre ai nomi (oggi una stringa unita: controlla il DAO di Personaggi). Il tocco sulla polaroid continua a spuntare l'outfit: le due azioni non devono sovrapporsi. "cibo sconosciuto" resta non toccabile.
3. **Il "?" in "Rovine del blocco ?".** Causa verificata il 05/10/2026: nel seed `nameIt` è `"Rovine del blocco\u00a0?"` (spazio non separabile) e Lilita One non ha il glifo U+00A0 (controllato con fontTools), quindi Android disegna quel punto con un font di ripiego. Correzione in **seedgen**: normalizzare U+00A0 in spazio normale nei testi che finiscono nel seed (in `it_wiki.py` c'è già la stessa sostituzione, ma solo per il confronto dei nomi), con test pytest; poi rigenerare `seed/` con seedgen (mai a mano, regola 1) e alzare `seedVersion`. Se rigenerare richiede la rete e la rete non c'è, **fermati e chiedi** (regola 5): non correggere a valle nel codice dell'app senza il suo ok.

**Fuori scope:** cambi all'algoritmo di Consigliami, nuove schermate, `targetSdk` 37.

**Fatto quando:** lint, test JVM, `assembleDebug` e `pytest` di seedgen verdi; test Robolectric di Risultati (righe, popup del trofeo che salva e "Nessun trofeo") e del tocco sul cibo nel dettaglio personaggio; `seedgen validate` verde sul seed rigenerato e nessun conteggio di `expected_counts.yaml` toccato. **Controlli a schermo:** Risultati in tema chiaro e scuro, scelta e cancellazione di un trofeo, effetto in Consigliami con "Pesa i risultati"; dal dettaglio di un personaggio al cibo e ritorno; "Rovine del blocco ?" nella lista di Consigliami; aggiornamento dalla versione installata senza perdere outfit, risultati e sblocchi (il `seedVersion` cambia).

---

## Comuni a tutte le fasi
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano funzioni, comandi o prerequisiti; `SPEC.md` aggiornata se cambia il comportamento dell'app.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, handoff se fase Opus); scelte non ovvie in `docs/decisioni.md`; la fase chiusa esce da questo file.
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni e perché, cosa resta aperto, controlli a schermo.

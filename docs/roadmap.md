# Roadmap

Le prossime fasi di lavoro. Ogni fase ha un **modello assegnato**: una sessione che parte con "vai avanti con la prossima fase" lo controlla per prima cosa (passo 0 del protocollo in `CLAUDE.md`). Le fasi chiuse escono da questo file (restano nella cronologia di git e, per le scelte, in `docs/decisioni.md`): il roadmap originale 1–8 è chiuso dalla 0.1.x.
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

## Revisione di Consigliami e sezione Cibi (fasi C1–C4, dal 05/10/2026)
Richiesta dell'autore dopo una prova in gioco: i consigli non corrispondono agli outfit che si sbloccano davvero correndo i Gran Premi e i Knockout Tour. C1 (dati degli stand) e C2 (sezione Cibi) sono chiuse il 05/10/2026: vedi `docs/decisioni.md` e l'handoff in `CLAUDE.md`. Le fasi C vengono dopo il roadmap 1–8 e hanno un **modello assegnato**: per prima cosa controllalo (passo 0 del protocollo in `CLAUDE.md`).

| Fase | Modello | In breve |
|---|---|---|
| C3 Grafica della lista | Sonnet | Lista di Consigliami e controlli con i componenti `Kart*` |
| C4 Dettaglio evento | Sonnet | Dettaglio con grafica `Kart*` e righe "outfit – cibo – percorso" |

### Diagnosi (05/10/2026)
Il calcolo fa quello che dice SPEC §6 (letto in `ConsigliamiUseCase`); il problema è che §6.1 presenta come certo ciò che i dati non sanno. Fatti verificati sul wiki il 05/10/2026:
- Il cibo → outfit è deterministico: "Outfits the player obtains are based on the food item that was consumed" (pagina [Dash Food](https://www.mariowiki.com/Dash_Food)). Le regole in `outfit_food_rules.json` non erano il sospettato principale (ricontrollate a campione in C1: coincidono con la pagina).
- La stessa pagina: dalla selezione del veicolo si può **disattivare** il cambio d'abito da Dash Food; in quel caso nessun outfit si sblocca.
- In un Gran Premio solo la prima gara è a giri; le altre tre partono dalla strada (route) dal percorso precedente. Il Knockout Tour "focuses primarily on the routes between the courses" (pagina [Mario Kart World](https://www.mariowiki.com/Mario_Kart_World), sezioni Grand Prix e Knockout Tour).
- Gli stand di "Course locations" ([List of Yoshi's locations](https://www.mariowiki.com/List_of_Yoshi%27s_locations)) sono quelli **dell'area** del percorso, anche fuori dal tracciato di gara (Crown City: dieci, alcuni "in an alley next to Bank Coin Coffer"). La pagina non dice quali si incontrano in gara (todo "Add descriptions of exact course locations"), e lo stesso percorso ha tracciati diversi a seconda dell'evento (pagina [Crown City](https://www.mariowiki.com/Crown_City)).
- Il gruppo `wild_bone` (Carne con l'osso) ha un solo stand, su una strada ("East gate of Crown Bridge"): con l'impostazione di default non viene mai consigliato.

### Decisioni dell'autore (05/10/2026, riassunte anche in `docs/decisioni.md`)
- Consigliami non promette: "su questo percorso c'è un Yoshi's con l'hamburger, occhi aperti: potrebbe essere sul tracciato". Gli stand si prendono soprattutto in corsa libera, quindi serve una **sezione Cibi** che metta in relazione ogni cibo con i luoghi dove si trova.
- Fonte: List of Yoshi's locations di mariowiki (CC BY-SA), **non** IGN (vedi "mai scraping automatico di IGN…" in `docs/decisioni.md`; IGN resta solo per il controllo a mano dell'autore).
- Gli stand sulle strade compaiono solo nella sezione Cibi, mai associati a un evento: collegarli a un rally richiederebbe di indovinare il tratto da una descrizione testuale (regola 5).
- Descrizioni dei luoghi in doppia lingua: inglese dal wiki, italiano tradotto a mano e dichiarato **non ufficiale**, come i nomi dei cibi.
- "Includi cibi nei dintorni" si toglie (con la sezione Cibi sarebbe ridondante).
- La sezione Cibi non è un collezionabile: è una sottosezione di Personaggi.

### C3 — Grafica della lista (Sonnet)
**Obiettivo:** la schermata Consigliami ha la stessa grafica del resto dell'app.

**Include:** card degli eventi come `KartPanel` (icona dell'evento, posizione con `KartBadge`, miglior personaggio con avatar, cibi utili), gruppi a pari merito, switch con `KartSwitchRow`, scelta GP/KO e cilindrata con `KartChoiceButton` (mai in `horizontalScroll`), slider del peso dentro un `KartPanel`, testi informativi con `KartInfoButton`/`KartPopup` (che spiega anche "occhi aperti: gli stand sono nell'area del percorso, non per forza sul tracciato"), tema chiaro e scuro, IT/EN. Nessun cambio di logica.

**Fuori scope:** il dettaglio evento (C4).

**Fatto quando:** lint, test JVM e `assembleDebug` verdi; test Robolectric della lista (niente `BoxWithConstraints` dove servono misure intrinseche, vedi `KartDropdownTest`). **Controlli a schermo:** lista in tema chiaro e scuro, schermo stretto, gruppi a pari merito, switch e slider.

### C4 — Dettaglio evento (Sonnet)
**Obiettivo:** toccando un GP o un rally si vede, per ogni personaggio, quali outfit potrebbero sbloccarsi e dove: "Filibustiere – Barbecue – Spiaggia di Peach".

**Prerequisiti:** C2 e C3 unite (riusa i loro componenti: `ui/food/`, `FoodDetail`).

**Include:** `KartPanel` per personaggio con avatar; una riga "outfit – cibo – percorso" per outfit (più percorsi per lo stesso outfit nella stessa riga); avviso "occhi aperti" una volta in cima, non su ogni riga; tocco sul cibo → sezione Cibi su quel cibo; sezione risultati per cilindrata con la grafica di Risultati. IT/EN.

**Fatto quando:** lint, test JVM e `assembleDebug` verdi; test Robolectric del dettaglio. **Controlli a schermo:** un GP e un rally contro il gioco, tema scuro, nomi lunghi, IT/EN, salto alla sezione Cibi e ritorno.

---

## Comuni a tutte le fasi
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano funzioni, comandi o prerequisiti; `SPEC.md` aggiornata se cambia il comportamento dell'app.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, handoff se fase Opus); scelte non ovvie in `docs/decisioni.md`; la fase chiusa esce da questo file.
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni e perché, cosa resta aperto, controlli a schermo.

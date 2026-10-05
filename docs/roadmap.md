# Roadmap

Le prossime fasi di lavoro. Ogni fase ha un **modello assegnato**: una sessione che parte con "vai avanti con la prossima fase" lo controlla per prima cosa (passo 0 del protocollo in `CLAUDE.md`). Le fasi chiuse escono da questo file (restano nella cronologia di git e, per le scelte, in `docs/decisioni.md`): il roadmap originale 1–8 è chiuso dalla 0.1.x.
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

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

### Decisioni dell'autore (05/10/2026, riassunte anche in `docs/decisioni.md`)
- Consigliami non promette: "su questo percorso c'è un Yoshi's con l'hamburger, occhi aperti: potrebbe essere sul tracciato". Gli stand si prendono soprattutto in corsa libera, quindi serve una **sezione Cibi** che metta in relazione ogni cibo con i luoghi dove si trova.
- Fonte: List of Yoshi's locations di mariowiki (CC BY-SA), **non** IGN (vedi "mai scraping automatico di IGN…" in `docs/decisioni.md`; IGN resta solo per il controllo a mano dell'autore).
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
- Immagini dei cibi (deciso dall'autore il 05/10/2026, estende la decisione del 26/09/2026): dalla pagina Dash Food di mariowiki, solo URL nel seed (originali, non thumbnail, validati sul prefisso `https://mario.wiki.gallery/images/`), scaricati a runtime come le altre, seedgen si ferma se un'immagine manca o non si risolve. **Tutte le varianti** di ogni gruppo (in genere tre, piccola/media/grande, col livello di boost della tabella; il sushi ne ha quattro), con nome EN e IT (nomi delle varianti: traduzione manuale non ufficiale se servono). Crediti: aggiorna README e disclaimer se nominano solo la pagina "Mario Kart World".
- Luoghi per variante, scelta dell'autore "dividi i luoghi per variante": si può fare **solo dove la fonte nomina il cibo specifico**. Verificato il 05/10/2026: List of Yoshi's locations nomina il gruppo per le varianti di taglia ("Burgers", "Kebabs"), quindi per quelle i luoghi restano del gruppo e nessuno stand si assegna a una taglia; nomina invece i cibi distinti del gruppo sushi ("Takoyaki", "Candy apples", "Taiyaki", "Sushi"), e lì ogni stand va alla sua variante. Gli stand "Chips, soft drinks, chocolate bars" (gruppi `snacks_1..3`) restano su tutti e tre come oggi. Outfit invariati per variante ("All food within the same group provides the same outfit", pagina Dash Food).
- Bioma degli stand: per i percorsi da `courses.regionId`; per le strade solo se la fonte lo dice, mai dedotto dalla descrizione.
- API per C2 e C4: stand per cibo (percorsi e strade, con bioma) e, per evento e personaggio, gli outfit con cibo e percorso.

**Fuori scope:** la schermata Cibi (C2) e la grafica (C3, C4).

**Fatto quando:** pytest, test JVM, lint, `assembleDebug` e `assembleRelease -PofflineSeed` verdi; **handoff** (max 15 righe) per C2–C4 in "Stato attuale". **Controlli a schermo:** il testo di Consigliami parla di possibilità; lo switch dei dintorni non c'è più; un utente con dati vecchi apre l'app senza perdere nulla.

### C2 — Sezione Cibi (Sonnet)
**Obiettivo:** una griglia di cibi; toccando un cibo si vede dove trovarlo e quali outfit sblocca per ciascun personaggio. Stessa grafica del resto dell'app (`Kart*`, cielo, banner).

**Prerequisiti:** C1 unita (usa la sua API).

**Include:**
- `ui/food/`, `Destination.Food` (griglia) e `Destination.FoodDetail(foodGroupId)` (dettaglio); nel drawer come sottovoce di Personaggi (stile `SubItem` di "Collezionabili"; un accesso anche dalla schermata Personaggi va proposto all'autore, non aggiunto da sé).
- **Griglia:** una tessera per gruppo di cibo, come le tessere della Home/le polaroid di Personaggi (`KartPanel`, `OutlinedTitle`, colonne adattive come Personaggi), nome nella lingua dell'app (traduzione non ufficiale) e quanti outfit ti mancano tra quelli che dà (`KartCounterPill`). I cibi **non** si ingrigiscono mai. Immagine della prima variante sulla tessera, caricata con Coil come le altre (`WikiImagePrefetcher`), segnaposto finché non è caricata; mai file immagine.
- **Dettaglio:** in cima tutte le varianti (immagine, nome, boost); poi i luoghi: per il sushi divisi per variante (takoyaki, mela caramellata, taiyaki, sushi), per gli altri gruppi una volta sola perché la fonte non dice quale taglia dà ciascuno stand. I luoghi sono divisi in "Sui percorsi" (bioma › percorso › luogo dello stand) e "Sulle strade" (bioma se ricavabile dalla fonte, poi luogo), luogo nella lingua dell'app (`localizedName`, `relocalizing()`); sotto, un `KartPanel` per personaggio con avatar e gli outfit che quel cibo gli sblocca (polaroid piccole di `ui/skin/Polaroid.kt`): solo gli **outfit** seguono la regola dei colori dell'app (grigio = non ce l'hai, colorato = ce l'hai), come nel dettaglio di Personaggi. I personaggi a cui il cibo non dà outfit (o che riporta all'abito base) in fondo, in una riga di testo.
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
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano funzioni, comandi o prerequisiti; `SPEC.md` aggiornata se cambia il comportamento dell'app.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, handoff se fase Opus); scelte non ovvie in `docs/decisioni.md`; la fase chiusa esce da questo file.
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni e perché, cosa resta aperto, controlli a schermo.

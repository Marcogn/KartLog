# REVIEW.md — cosa deve controllare una revisione di KartLog

Regole complete in `CLAUDE.md`, scelte e motivi in `docs/decisioni.md`. Qui solo ciò che va controllato sempre.

## Cosa vuol dire "Important" qui
- **Important**: viola una regola non negoziabile di `CLAUDE.md`, perde o corrompe lo stato utente (migrazioni Room, backup, reseed), o è un bug dimostrabile.
- **Nit**: tutto il resto (nomi, stile, preferenze).

## Da controllare sempre
- **Dati di gioco**: nessun dato inventato né scritto nel codice o nei test; `seed/` non è modificato a mano (solo da `tools/seedgen`); i test Kotlin usano il seed `FAKE_FOR_TESTS`.
- **Conteggi**: `tools/seedgen/expected_counts.yaml` non è rilassato per far passare build o test; se cambia, è un commit dedicato che cita la fonte.
- **Asset Nintendo**: nessun render, logo, font o screenshot nel repository o nell'APK (eccezione: `res/drawable-nodpi/home_*` e `kart_*`). Le immagini di gioco sono solo URL del seed. Nessun "Mario", "Mario Kart" o "Nintendo" nel nome dell'app o nel package.
- **Rete**: `INTERNET` solo per le immagini; nessuna chiamata di rete per i dati di gioco a runtime (solo asset `assets/seed/*.json`, mai `seed/`).
- **Room**: ogni cambio di schema ha versione, migrazione scritta e testata, schema in `app/schemas/`; mai `fallbackToDestructiveMigration()`. ID dei dati di gioco = slug di seedgen, mai autoincrement; gli ID degli stand non entrano nello stato utente.
- **Strati**: `domain/` senza dipendenze Android (e senza import di `ui/`); la logica di Consigliami sta in `domain/consigliami/`, non nel ViewModel né nel DAO (il DAO espone `Flow` grezzi); un DAO per schermata, un solo `Flow`.
- **Localizzazione**: ogni stringa visibile in `values/` e `values-en/`; i nomi di gioco passano da `localizedName()` e i ViewModel che localizzano da `relocalizing()`.
- **Grafica**: schermate nuove con i componenti `Kart*` (`ui/common/KartChrome.kt`, `Modifier.kartSky`); mai `FilterChip`; mai `BoxWithConstraints`/`SubcomposeLayout` dove il genitore chiede misure intrinseche. Grigio = ti manca, colorato = ce l'hai.
- **Navigazione**: ogni `navigate()`/`popBackStack()` passa dalla guardia `lifecycleIsResumed()`; rotte `@Serializable` in `Destination`.
- **Personaggi**: i 32 `starter` sono sempre sbloccati nelle query (`SkinDao`, `ConsigliamiDao`); l'outfit di default è sempre in `owned_outfits`.
- **Documenti**: CHANGELOG `[Unreleased]` per ogni modifica visibile (`- **Sintesi.** dettaglio`); `CLAUDE.md` "Stato attuale" e `docs/decisioni.md` aggiornati se la modifica li rende falsi.

## Soglia di verifica
Un'affermazione sul comportamento cita `file:riga`. Una regola è violata solo se si indica la riga. Verifica a schermo: qui non c'è emulatore, la fa l'autore.

## Da non segnalare
Formattazione, gusti di nomenclatura, ciò che Android Lint già segnala, avvisi noti (`createComposeRule` deprecato nei test Compose, `targetSdk` 36 voluto).

## Forma
Al massimo 10 segnalazioni, in ordine di gravità: `[Important|Nit] percorso:riga — regola violata (fonte) — cosa fare`.

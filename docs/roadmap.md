# Roadmap

Le prossime fasi di lavoro. Ogni fase ha un **modello assegnato**: una sessione che parte con "vai avanti con la prossima fase" lo controlla per prima cosa (passo 0 del protocollo in `CLAUDE.md`). Le fasi chiuse escono da questo file (restano nella cronologia di git e, per le scelte, in `docs/decisioni.md`): il roadmap originale 1–8 è chiuso dalla 0.1.x.
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

## Aggiornamenti dall'app e Info (fase A1, dall'11/10/2026)
Richiesta dell'autore dell'11/10/2026, per tutte e sei le sue app: controllo degli aggiornamenti su GitHub all'avvio e versione in app con i disclaimer. Il progetto è comune: lo disegna **ThePatientGamerHelper**, l'app pilota (sua Fase 9, nucleo Opus e UI Sonnet); qui si copia il suo pacchetto `update`. Progetto condiviso, fatti verificati e fonti nel piano del pilota: [`ThePatientGamerHelper/docs/plan-phase-9-updates.md`](https://github.com/Marcogn/ThePatientGamerHelper/blob/main/docs/plan-phase-9-updates.md). Scelte in `docs/decisioni.md` ("Backup e Impostazioni", "Toolchain, build e CI").
Regola generale: **fuori scope = tutto ciò che appartiene alle fasi successive**, anche se sembra comodo farlo subito.

| Fase | Modello | In breve |
|---|---|---|
| A1 | Sonnet | Aggiornamenti da GitHub Releases (porting dal pilota), frequenza in Impostazioni, versione e "fatto con ❤ da marcogn" nella sezione Info |

### A1 — Aggiornamenti e Info (Sonnet)
**Obiettivo:** all'avvio, con la frequenza scelta, l'app controlla le release di `Marcogn/KartLog`; se c'è una versione nuova mostra le novità, chiede se scaricarla e poi chiede di installarla col programma di installazione di sistema. La sezione Info mostra la versione installata.

**Prerequisiti:** R1 nel `main` (fatto); **Fase 9 di ThePatientGamerHelper nel `main` e verificata a schermo** (è il codice che si copia).

**Include**
1. `domain/update/` (puro, test JVM copiati): `AppVersion`, parser di `releases/latest`, scelta dell'unico asset `.apk` (le release hanno anche `KartLog-x.y.z-mapping.txt`), pulizia del corpo della release, `UpdateFrequency` e `UpdateCheckPolicy.isDue`.
2. `data/update/`: `GitHubReleaseClient` su `HttpURLConnection` (Coil resta solo per le immagini), `ApkDownloader` con SHA-256 contro il `digest` dell'asset, `ApkVerifier` (stesso pacchetto, `versionCode` più alto, stesso certificato di firma), `ApkInstaller` (sessione `PackageInstaller`, conferma di sistema, permesso "installa app sconosciute"), `UpdateManager`; preferenze nel DataStore di `ThemePreferences`.
3. Manifest: `REQUEST_INSTALL_PACKAGES`; il commento su INTERNET dice anche del controllo aggiornamenti. Nessuna nuova dipendenza, niente WorkManager: controllo solo all'avvio; nelle build debug mai in automatico.
4. Dialogo con la grafica del gioco (`KartPopup`, `KartPopupText`, `KartChoiceButton`): versione nuova e attuale, titoli delle novità, "Changelog completo" (apre GitHub), **Scarica** / **Non ora**; avanzamento con Annulla; errori col motivo. "Non ora" chiude e basta: al prossimo controllo dovuto richiede.
5. Impostazioni: frequenza Mai / A ogni avvio / Ogni giorno / Ogni settimana (**predefinita: ogni settimana**), con `KartDropdown`.
6. Sezione Info (già esistente): in cima nome dell'app, `BuildConfig.VERSION_NAME` (e `VERSION_CODE` in piccolo), "Controlla aggiornamenti" col risultato, "Fatto con ❤ da marcogn" / "Made with ❤ by marcogn", link al repository, licenza MIT; restano i testi già presenti (immagini e Nintendo, mappa, cibi, font, dati). In più (testo da rivedere nella PR): "L'app si collega a GitHub solo per controllare gli aggiornamenti; con 'Mai' non lo fa".
7. Regola 3 di `CLAUDE.md`, `SPEC.md` §0 punto 3 ("nessun altro traffico") e il commento nel manifest aggiornati: INTERNET serve alle immagini **e** al controllo aggiornamenti su `api.github.com`/download delle release. README: paragrafo "Aggiornamenti".

**Fuori scope:** installazione senza conferma, pre-release, controlli in background, nuove lingue.

**Fatto quando**
- Test JVM di `domain/update` (copiati, verdi); test Robolectric del dialogo/ViewModel (stato → dialogo, "Non ora", errore) e della preferenza di frequenza, coi testi letti da `R.string`.
- I punti comuni qui sotto; CI verde.
- **Controlli a schermo:** installata una release firmata più vecchia, all'avvio compare il dialogo; download; la prima volta la schermata "installa app sconosciute"; installazione; l'app si riapre sulla nuova versione con i dati intatti; modalità aereo all'avvio (nessun dialogo, nessun crash); "Mai" (nessun dialogo, "Controlla aggiornamenti" funziona); Info mostra la versione installata; tema scuro e inglese.

**Domande aperte:** testo esatto della riga sul collegamento a GitHub; numero di versione della release che lo porta (proposta: 1.3.0).
**Rischi:** cambi dell'API o degli asset di GitHub (il parser rifiuta, nessun aggiornamento offerto); `digest` assente (resta il controllo della firma, che Android ripete); schermata "app sconosciute" diversa tra produttori.

---

## Comuni a tutte le fasi
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` e `cd tools/seedgen && python -m pytest` passano.
- CHANGELOG aggiornato nel formato del progetto; README aggiornato se cambiano funzioni, comandi o prerequisiti; `SPEC.md` aggiornata se cambia il comportamento dell'app.
- `CLAUDE.md`: "Stato attuale" aggiornato (fase completata, prossima fase, handoff se fase Opus); scelte non ovvie in `docs/decisioni.md`; la fase chiusa esce da questo file.
- Riepilogo finale all'autore: cosa è stato fatto, deviazioni e perché, cosa resta aperto, controlli a schermo.

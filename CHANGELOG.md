# Changelog

All notable changes to KartLog are documented in this file.
The format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning follows the app's `versionName` in `app/build.gradle.kts`.
UI texts are quoted as they appear in the app (Italian is the default language).

## [Unreleased]

## [1.3.0] - 2026-10-18

- **Repository documentation and changelog in English.** README, specification, project decisions and this changelog are now in English (the app itself stays Italian by default, with English available). The news titles shown by the update dialog will therefore be in English.

## [1.2.1] - 2026-10-12

- **Game data updated from Super Mario Wiki and mkworld-checklist (seedVersion 11).** Dash Food@5455370, List of Yoshi's locations@5263331, List of Mario Kart World missions@5514472, Template:Mario Kart World@5513200, Golden Rally@5489159, Ice Rally@5479135, Moon Rally@5479138, Spiny Rally@5479139, Cherry Rally@5507581, Acorn Rally@5479142, Cloud Rally@5479143, Heart Rally@5479144, Drill Rally@5507592, Boomerang Rally (rally)@5507540, Propeller Rally@5514737, Turnip Rally@5488553, Mario Kart World@698015, Lista delle missioni di Mario Kart World@686389, Mario Kart World@5514814, mkworld-checklist@ee86055
- **Ticked P switches safe from wiki changes.** Each P switch is now identified by its mission name, not by its position in the wiki list: the move of one mission ("Gather the blue coins around the control tower!", now at Peach Stadium) would have shifted the ticks of 97 missions. Ticks already saved and backups made before are carried over to the new IDs without losing anything.
- **In-app updates.** At startup KartLog checks GitHub releases; if there is a new one it shows what's new, asks whether to download it and then to install it with the system confirmation.
  - New dialog in the game's style: titles of the news, "Changelog completo", Scarica / Non ora, progress with Annulla and the reason in case of error.
  - The APK is verified before installation (SHA-256, same package, higher version, same signature).
  - New setting "Controllo degli aggiornamenti": Mai / A ogni avvio / Ogni giorno / Ogni settimana (default). Debug builds never check on their own.
  - New "install unknown apps" permission (`REQUEST_INSTALL_PACKAGES`), asked by Android only when you install an update.
- **Version and credits in Info.** The Info section of Impostazioni shows the installed version, "Controlla aggiornamenti" (even with "Mai"), "Fatto con il ❤️ da Marcogn", the link to the repository, the MIT license and the note that the app connects to GitHub only for updates.

## [1.2.0] - 2026-10-05

- **Risultati with the game's graphics.** Each event is a tile (orange for Grand Prix, green for
  Knockout Tour) with a black-outlined icon, name and the trophy in a pill (grey if you don't have
  it, yellow if you do); the trophy choice is a game-style popup with one button per trophy and
  "Nessun trofeo".
- **From the outfit's food to the Cibi section.** In a character's detail every food under an
  outfit is underlined and tappable: it opens that food in Cibi (the back gesture returns to the
  character). Tapping the rest of the polaroid still ticks the outfit.
- **Fixed the "?" of "Rovine del blocco ?".** The Italian name had a non-breaking space that the
  app font doesn't draw: seedgen now turns it into a normal space (game data updated,
  seedVersion 10; no change to the real game data).
- **Consigliami with the game's graphics.** The event cards are coloured tiles (orange for Grand
  Prix, green for Knockout Tour) with icon, position, recommended character and possible foods;
  filters, switch and results weight are in a blue tile and the warning about the stands is behind
  the "i". Event icons and avatars with a black outline.
- **Consigliami ranking numbered 1, 2, 3.** No more shared numbers for ties (1, 1, 3) nor cards
  grouping events: each event has its own card and its own number.
- **Removed "Solo utili".** Events where no outfit can be unlocked never appear, even with
  "Pesa i risultati" on (before, they stayed in the list there).
- **Event detail redone.** One tile per character with a row per outfit and the needed food next
  to it ("Vespone – Barbecue"), with the event's courses where the stand might be; tapping the food
  opens it in the Cibi section. Best trophies in a tile at the bottom.
- **New Cibi section.** Sub-item of Personaggi in the drawer: one tile per food with how many
  outfits you are missing; tapping it shows the variants, where to find it (on courses and on
  roads, sushi split by variant) and the outfits it unlocks for each character, grey if you don't
  have them. Places and Italian names are an unofficial translation; the stands on courses are in
  the area, not necessarily on the track. Characters who are missing the outfit come first.
- **Consigliami speaks of possibilities.** The Yoshi's stands of a course are in the course's
  area, not necessarily on the race track: the advice now says "fino a +N outfit" and "potrebbero
  esserci (occhi aperti)", with a warning at the top of the detail and the reminder that the outfit
  change can be switched off in the vehicle selection.
- **Removed "Includi cibi nei dintorni".** The foods of an event come only from the stands of its
  courses; the stands on the roads between courses will arrive in the Cibi section.
- **All Yoshi's stands and the foods one by one in the data (seedVersion 9).** 109 stands (70 in the
  courses' area, 39 on roads) with place in English and Italian (unofficial translation) and the 60
  foods of the Dash Food page with boost level and image, downloaded at runtime like the others.
  Database updated without losing anything of the user's data.

## [1.1.2] - 2026-10-04

- **Back gesture with the sideways slide again.** Since 1.1.1, going back made the screen shrink
  and disappear: the default animation of Navigation 2.10's predictive back gesture. Now the
  gesture uses the same animation as the back button, as in 1.1.0.

## [1.1.1] - 2026-10-04

- **Boomerang Rally source updated (seedVersion 8).** On 03/10/2026 mariowiki renamed the page
  "Boomerang Rally (Mario Kart World)" to "Boomerang Rally (rally)" and the release build
  stopped; game data unchanged, only the source title and the recorded revisions change.
- **Libraries and build tools updated.** Gradle 9.8, Android Gradle
  Plugin 9.4, Kotlin 2.4, Compose BOM 2026.09, Room 2.8, Hilt 2.60, Coil 3.6
  and the other AndroidX libraries to the latest stable versions; no
  feature changes (details in `docs/decisions.md`, "Toolchain" section).
- **GitHub Actions workflows shared with the other projects.** CI now
  also checks the release build and no longer uploads the debug APK (the app had to be
  uninstalled to try it); the APK to try is the one from *Build
  APK*, which now checks the signature, like the release (`docs/ci.md`).

## [1.1.0] - 2026-09-29

- **Collectibles map.** New "Mappa" screen (side menu, and big red
  button in Monete Peach and Pulsanti P) with the positions of
  Peach Medallions, P switches and "?" panels: zoom, filters by type and by
  "done", popup with instructions (in English), tick and YouTube video.
  Data and map from BamisWasTaken's mkworld-checklist (mktools.io)
  project; the image is downloaded at runtime.
- **Peach Medallions one by one.** The 200 coins become points on the map
  with their instructions, instead of the per-biome counter. The counts
  saved before cannot be converted: a notice explains it.
- **Home revised.** Personaggi and Risultati, Pulsanti P and World map,
  Consigliami below; Monete Peach stays in the menu. The Risultati
  counter counts trophies per engine class (x/80), no longer the events.
- **"?" panels.** The 150 panels on the map and in a dedicated page,
  one by one with instructions like the Peach Medallions; included in the backup.
- **Menu with the Collezionabili.** Monete Peach, Pulsanti P and Pannelli ?
  grouped under "Collezionabili", as smaller, always
  visible entries.

## [1.0.0] - 2026-09-27

- **First release!**

## [0.1.x] - beta

The test versions from 0.1.1 to 0.1.7 (25–27/09/2026), merged into
a single entry. What the app contains at the end of the beta:

- **Personaggi.** All 50 drivers, with Super Mario Wiki images
  in polaroid cards in the character's colour. The 24 with outfits have
  the outfits page, with the foods that unlock them. The 32 starter drivers are always
  unlocked; for the 18 to unlock a popup shows the
  criterion from the wiki and the switch to mark them. Grey if you are missing it,
  coloured if you have it. Tutti/Incompleti filter and ordering by
  roster, alphabetical or completion.
- **Peach Medallions and P switches.** Peach Medallions with a − / + counter per
  biome; the 394 P switches split by region and course, with search and
  "segna tutti".
- **Consigliami.** Which Grand Prix or Knockout Tour to race, and with whom,
  to unlock more outfits, with the foods on the course or nearby and,
  optionally, the weight of the results already obtained.
- **Risultati.** The best trophy (bronze … gold ★★★) for each event and
  engine class, in tabs; an "i" on the Mirror tab explains how to
  unlock mirror mode.
- **Game-style graphics.** Banner with sky and track, coloured tiles on
  Home, sky with clouds behind every screen (night in the dark
  theme), red side menu, big red buttons, checkered popups, Lilita One
  font.
- **Italian and English.** Official Italian names from mariowiki.com and
  mariowiki.it; food names and mirror mode conditions in
  Italian are an unofficial translation. The P switch missions
  stay in English. Theme and language are chosen in Impostazioni.
- **Backup.** Export and import of the state as JSON from
  Impostazioni, with the data attribution (CC BY-SA 4.0, Super Mario
  Wiki).
- **Game data from Super Mario Wiki (seedVersion 6).** Extracted by
  `tools/seedgen` and verified at every release build. Images are
  downloaded at runtime and are not included in the app.

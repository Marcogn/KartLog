# KartLog

[![Latest release](https://img.shields.io/github/v/release/Marcogn/KartLog?label=release)](https://github.com/Marcogn/KartLog/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![minSdk 26](https://img.shields.io/badge/minSdk-26-brightgreen.svg)](app/build.gradle.kts)

Android tracker for the Mario Kart World collectibles: character skins
(outfits), Peach Medallions and P Switches, with a "Consigliami" module
that suggests which Grand Prix or Knockout Tour to race — and with which
character — to unlock more missing outfits.

> Working name "KartLog": no Nintendo asset, name or logo appears in the
> repository, the package or the icon (see [`SPEC.md`](SPEC.md) §0). Game
> images are downloaded at runtime, see [Images](#images).
>
> The app's UI is Italian (default) and English; the repository documentation is in English.

## Why

Mario Kart World outfits are unlocked by feeding the characters foods
found on the courses, in combinations the game never explains in a single
place: KartLog collects them from Super Mario Wiki and turns them into a
tracker with completion counters, checklists for Peach Medallions and
P Switches, and a suggestion of which event to race (and with whom) to get
as close as possible to the complete collection.

## Project status

The app is usable and published (see [`CHANGELOG.md`](CHANGELOG.md)). The
work in progress is in [`docs/roadmap.md`](docs/roadmap.md).

## Features

From the full specification ([`SPEC.md`](SPEC.md)):

- **Personaggi** (characters): all 50 drivers, with an obtained/total outfit
  counter for the 24 that have them; for each one, a checklist of the outfits
  with the food groups that unlock them and a switch for the unlock state.
  Drivers without outfits are marked as unlocked with a tap.
- **Cibi** (foods): sub-section of Personaggi; for each food, the variants,
  where to find it (Yoshi's stands on courses and on roads) and the outfits it
  unlocks per character.
- **Monete Peach** (Peach Medallions): the 200 coins one by one, with the
  instructions to find them and a button to see them on the map.
- **Pulsanti P** (P Switches): checklist by biome and course, with search and
  "segna tutti" (mark all).
- **Pannelli "?"** ("?" panels): the 150 panels one by one, like the Peach
  Medallions. In the side menu the three lists are under "Collezionabili".
- **Mappa** (map): Peach Medallions, P Switches and "?" panels on the world
  map, with zoom, filters by type and by "done", instructions and videos.
  It opens from the menu or from the big red button of Monete Peach and
  Pulsanti P.
- **Consigliami**: ranking of the Grand Prix/Knockout Tours to race based on
  the outfits still missing, with the recommended character, the next two
  alternatives and the foods that might be there: the Yoshi's stands are in
  the courses' area, not necessarily on the race track, so every piece of
  advice is a possibility ("occhi aperti", keep your eyes open).
- **Risultati** (results): dedicated screen, reachable from Home, where the
  best trophy (bronze … gold ★★★) of each Grand Prix and Knockout Tour is
  recorded per engine class; optional weight in Consigliami's score.
- **Backup**: JSON export/import of the user state (not of the game data)
  through the Storage Access Framework, from the Impostazioni (settings) screen.
- **Impostazioni**: theme (system/light/dark) and app language
  (system/Italian/English).

## Game data

The game data (characters, outfits, foods, courses, events, P Switches)
comes from **Super Mario Wiki** (mariowiki.com), text content under the
**CC BY-SA 4.0** license, extracted by the script in
[`tools/seedgen/`](tools/seedgen/README.md) and versioned in
[`seed/`](seed/README.md). No data is invented: details on sources,
extraction rules and attribution in [`SPEC.md`](SPEC.md) §5.

The Italian names come from mariowiki.com and, for what is missing there
(drivers without outfits, biomes, Grand Prix and Knockout Tours), from the
[**Italian Super Mario Wiki**](https://www.mariowiki.it) (also
CC BY-SA 4.0). Two exceptions: the P Switch missions stay in English (the
Italian list is incomplete), and the Italian names of the **foods** are an
**unofficial translation** made by hand, because no source reports them. The
same goes for the Italian **mirror mode conditions**: translated by hand from
the mariowiki.com text, because the Italian wiki's is less up to date, and for
the **places and venue types of the Yoshi's stands** ("List of Yoshi's
locations" page). The drivers' unlock criteria come instead from the two
wikis, each in its own language.

## Collectibles map

The map, the positions of Peach Medallions, P Switches and "?" panels, the
instructions to find them (English only) and the videos come from the
[**mkworld-checklist**](https://github.com/BamisWasTaken/mkworld-checklist)
project by BamisWasTaken, which publishes [mktools.io](https://www.mktools.io).
Thank you! `tools/seedgen` reads its data at a pinned commit
(`sources.yaml`) and matches it with the mariowiki missions; the map image
is not in the repository nor in the APK, the app downloads it at runtime like
the wiki images.

## Images

The images of characters, outfits, Grand Prix and Knockout Tours come from
[**Super Mario Wiki**](https://www.mariowiki.com/Mario_Kart_World)
("Mario Kart World" page), those of the foods from the
[Dash Food](https://www.mariowiki.com/Dash_Food) page, served by its CDN
`mario.wiki.gallery`.

- **They are not in the repository nor in the APK.** `tools/seedgen` records
  only the URL in `seed/` (`imageUrl`); the app downloads them at startup and
  keeps them in its own cache. Without network and without cache the
  placeholders with initials are shown.
- **They are not covered by the CC BY-SA license** of the wiki text: they are
  screenshots and artwork of Mario Kart World. Mario Kart World, its
  characters and the related images are © Nintendo.
- Thanks to Super Mario Wiki and its contributors, who collected and
  uploaded them.

The interface graphics (banner, track at the bottom of Home, logo and Home
icons) come from a mockup by the author; the Home icons recall game objects
and will be replaced. Title font:
[Lilita One](https://fonts.google.com/specimen/Lilita+One) by Juan
Montoreano, SIL Open Font License 1.1 (text in
`app/src/main/assets/licenses/`).

KartLog is an amateur, free project, **not affiliated with or endorsed by
Nintendo**. The same notice is in the app, in Impostazioni / Info.

## How to build

```bash
./gradlew assembleDebug       # debug APK: no network, no Python
./gradlew testDebugUnitTest   # JVM unit tests
./gradlew lint                # Android Lint
```

The **debug** build uses only the JSON already versioned in `seed/`. The
**release** build also runs `tools/seedgen` to regenerate and validate the data
from the wiki (see `SPEC.md` §5.4) — it requires Python 3 and, unless
`-PofflineSeed`, network access to mariowiki.com.

Requires JDK 17 or later, the Android SDK (`compileSdk 37`) and network access
to Google's Maven repository. Gradle 9.8 and AGP 9.4 (see
[`docs/decisions.md`](docs/decisions.md), "Toolchain" section).

**Which APK to install:** the one from the *Build APK* workflow (Actions →
Build APK → Run workflow) or from a release: it is the release build, always
signed with the same key, so it installs over the app already present without
losing data. CI no longer uploads APKs; details in [`docs/ci.md`](docs/ci.md).

## Updates

At startup (at most once per launch, and never in debug builds) the app looks
at the latest release of this repository on GitHub; if it is newer, it shows
what's new and asks whether to download it, then asks to install it with the
system installer (the first time Android asks to enable "install unknown apps"
for KartLog; the installation is always confirmed by you). The APK is verified
(SHA-256, same package, higher version, same signature) before reaching the
installer. In Impostazioni the frequency is Mai / A ogni avvio / Ogni giorno /
Ogni settimana (never / every launch / daily / weekly; default: weekly);
"Mai" makes no request to GitHub, and in Info "Controlla aggiornamenti" (check
for updates) works anyway. The code is copied from
[ThePatientGamerHelper](https://github.com/Marcogn/ThePatientGamerHelper) (`docs/decisions.md`).

## Privacy

No analytics, no account. The INTERNET permission is used **only** to
download the images (see [Images](#images)) from the Super Mario Wiki CDN and
to check for updates (see [Updates](#updates)): no other data leaves the
device, and everything else in the app works offline too. At build time the
network is also used by the `tools/seedgen` script, which regenerates the game
data from the wiki.

## Tech stack

Kotlin and Jetpack Compose with Material 3, same reference architecture as
[ThePatientGamerHelper](https://github.com/Marcogn/ThePatientGamerHelper)
(see [`docs/decisions.md`](docs/decisions.md), "Toolchain" section, for the
details of the alignment):

- **Room** as the source of truth for the seed data and the user state
- **Hilt** for dependency injection
- **Navigation Compose** with type-safe routes (`kotlinx.serialization`)
- **ViewModel + StateFlow**, unidirectional data flow
- `minSdk 26`, `targetSdk 36`, `compileSdk 37`

## Project structure

```
SPEC.md                  complete functional and technical specification
CLAUDE.md                operating guide for whoever (or whatever) develops the project
docs/                    roadmap, project decisions, CI
seed/                    game data JSON (CC BY-SA 4.0), generated
tools/seedgen/           Python script that generates seed/ from Super Mario Wiki
app/src/main/java/com/marcogn/kartlog/
├── ui/                  Compose screens (home, skin, medallions, P switches,
│                        consigliami, settings) + theme + navigation
├── data/                Room (entity/dao), DTOs and seed repository (phase 3)
└── domain/              pure models and the Consigliami algorithm (phase 6), no Android dependency
```

## Documentation

- [`CHANGELOG.md`](CHANGELOG.md) — what changed in each release
- [`SPEC.md`](SPEC.md) — functional and technical specification of the app
- [`docs/roadmap.md`](docs/roadmap.md) — the next work phases
- [`docs/decisions.md`](docs/decisions.md) — the project choices and their
  reasons (data sources, graphics, toolchain, alignment with
  ThePatientGamerHelper)
- [`docs/ci.md`](docs/ci.md) — the GitHub Actions workflows (CI, Build
  APK, Release, run cleanup), shared with the author's other Android projects
- [`docs/claude.md`](docs/claude.md) — how Claude Code is set up (skills,
  reviewer agent, hooks), from the kit shared with the other Android projects
- [`CLAUDE.md`](CLAUDE.md) — operating rules, commands and status for whoever
  develops the project
- [`tools/seedgen/README.md`](tools/seedgen/README.md) — usage of the data
  extraction script
- [`seed/README.md`](seed/README.md) — format of the seed files

## Contributing

Personal single-user project: no public roadmap open to proposals. Bug
reports and small, targeted fix PRs are welcome — open an issue before
working on anything substantial. To report a security vulnerability see
[`SECURITY.md`](SECURITY.md) instead of opening a public issue.

## About this project

KartLog is an independent project developed by marcogn in close
collaboration with Claude, Anthropic's AI coding assistant — Claude writes and
reviews much of the code, under direct human supervision and design at every
step.

## License

MIT, see [`LICENSE`](LICENSE). The data in `seed/` remains under Super Mario
Wiki's CC BY-SA 4.0 license (see [`seed/LICENSE`](seed/LICENSE)), separate from
the code license.

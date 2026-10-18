# seedgen

Extracts KartLog's game data from **Super Mario Wiki** and generates the JSON files in `seed/`. Full specification: `SPEC.md` §5.

## Where the data comes from

Pages are listed **by exact title** in `sources.yaml`: the script searches nothing on the wiki.

| Page | What it provides |
|---|---|
| `Dash Food` | "List of food" table: food groups → outfit per character; each food (one row = one variant, with boost level and image URL); the Locations column only as a cross-check of the stands |
| `List of Yoshi's locations` | all Yoshi's stands, one per row: "Course locations" (course, venue type, food, place) and "Route locations" (roads between courses: food and place) |
| `List of Mario Kart World missions` | P switches: biome, mission name, course |
| `Template:Mario Kart World` | navbox: the 8 cups with their 4 courses, and the list of rallies (to notice new rallies) |
| `Golden Rally`, `Ice Rally`, … (12 pages) | stages of each Knockout Tour, from the "Starting point … Final course" table |
| `Mario Kart World` | only the image **URLs** of characters, outfits, cups and rallies, which drivers are available from the start, the "Unlock criteria" table and the mirror mode conditions, without parenthetical notes (`seedgen/images.py`); always downloaded live |
| mkworld-checklist (GitHub, commit pinned in `sources.yaml`): `checklist-data.json`, `i18n/en.json`, header of `map.webp` | positions of Peach Medallions, P switches and "?" panels, English instructions, YouTube videos, map URL and size (`seedgen/checklist.py`); always live. P switches are matched to the mariowiki ones by mission name |
| mariowiki.it: `Mario Kart World`, `Lista delle missioni di Mario Kart World` | Italian names that mariowiki.com does not have: drivers without outfits, biomes, Trophies and rallies, and the "Criteri di sblocco" table (`seedgen/it_wiki.py`); always live |

`manual/food_names_it.yaml` holds the Italian names of the foods (groups and single foods): **unofficial translation**, chosen by the author because no source reports them. `manual/stand_locations_it.yaml` translates in the same way the places and venue types of the Yoshi's stands. `manual/mirror_mode_it.yaml` translates in the same way the mirror mode conditions (mariowiki.it has a less up-to-date version). Each entry also carries the exact English text: if it changes on the wiki, seedgen stops until the translation is updated.

Pages are downloaded with the MediaWiki API (`https://www.mariowiki.com/api.php?action=parse&prop=text|revid`). Each extraction records the `revid` of every page in `seed/meta.json`. Images are never downloaded: seedgen saves only the URL (`imageUrl`), and the app downloads them at runtime.

The Italian-name pages and the `Mario Kart World` page are downloaded only in a live `generate`/`release`, never from fixtures (they are large and only a small part is used): with `--from-fixtures` or `--from-raw` the `nameIt` and `imageUrl` fields stay `null`. The same goes for mkworld-checklist: without it, `peach_medallions.json`, `question_panels.json`, `map.json` and the P switch positions are missing (validation accepts all or none of the three files).

## Files

| File | Role |
|---|---|
| `sources.yaml` | page titles, API URL, User-Agent, timeout |
| `aliases.yaml` | wiki name → stable slug for characters and courses; equivalences to validate. An unknown name makes the extraction fail |
| `expected_counts.yaml` | expected counts and game version. Source of truth for the Kotlin tests too |
| `manual/checklist_mission_names.yaml` | the 3 missions written differently by mkworld-checklist and by mariowiki, checked by hand |
| `manual/legacy_p_switch_ids.yaml` | old positional P switch IDs (`pswitch_001`…`pswitch_394`, up to seedVersion 10) → mission name, frozen from seed v10 with a script. They become `seed/p_switch_legacy_ids.json`; a mission that disappeared from the wiki stops the extraction. Not to be edited |
| `manual/p_switch_renames.yaml` | missions renamed on the wiki after phase P1: current name → original name, from which the ID stays (empty until needed) |
| `tests/golden/raw_manual_2026-09-25.yaml` | manual transcription of the pages, used to generate the first `seed/` and as a cross-check of the parser |
| `tests/fixtures/synthetic/` | synthetic HTML reproducing the structure of the pages, for the parser tests |
| `tests/fixtures/real/` | real pages saved with `fetch-fixtures` (to be created, see below) |

## Usage

```bash
cd tools/seedgen
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt

python -m pytest                                    # tests (no network)
python -m seedgen validate --seed ../../seed        # validate the versioned seed
python -m seedgen fetch-fixtures                    # download the real pages into tests/fixtures/real/
python -m seedgen generate --from-fixtures tests/fixtures/real --out /tmp/cand --dump-raw /tmp/raw.yaml
python -m seedgen check --seed ../../seed --candidate /tmp/cand
python -m seedgen accept --seed ../../seed --candidate /tmp/cand
python -m seedgen release --seed ../../seed --work ../../app/build/seedgen/candidate [--offline]
```

`release` is the entry point of the Gradle task `generateSeed`. Exit codes: `0` ok, `1` usage/configuration, `2` invalid data, `3` valid data but different from `seed/`, `4` network.

## Before using it with the network

1. In `sources.yaml`, replace `<REPO_URL>` in `user_agent` with the repository URL or a contact.
2. Run `fetch-fixtures` and then `pytest`. This enables `test_real_fixtures.py`, which compares the parser with the manual transcription:
   - if it passes, the parser reads the real pages correctly;
   - if it fails, the message shows the differences: they must be understood (wrong parser or updated wiki) before accepting anything.

## Status

- The parser has been verified both on synthetic HTML (`tests/fixtures/synthetic/`) and on the 16 real wiki pages (`tests/fixtures/real/`, committed — see `test_real_fixtures.py`).
- `seed/` comes from a real extraction via the API (`origin: api` in `meta.json`, `seedVersion 2`). The manual transcription (`tests/golden/raw_manual_2026-09-25.yaml`) remains as a historical reference and as a fixture for `test_seed.py` (excluding the P switches, which it did not cover).
- `seed/p_switches.json` exists with the 394 entries.
- Yoshi's stands (`yoshi_stands.json`, roads too) and foods one by one (`food_variants.json`) since phase C1 (`seedVersion 9`): they replace `food_group_courses.json`, which `accept` deletes from `seed/`.

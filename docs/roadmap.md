# Roadmap

The upcoming work phases. Each phase has an **assigned model**: a session that starts with "vai avanti con la prossima fase" checks it first (step 0 of the protocol in `CLAUDE.md`). Closed phases leave this file (they stay in git history and, for the choices, in `docs/decisions.md`): the original roadmap 1–8 was closed by 0.1.x.
General rule: **out of scope = everything that belongs to later phases**, even if it seems convenient to do it now.

## Repository in English (phases I1–I2, from 17/10/2026)
Issue #36, author's request of 11/10/2026 (one issue per app): whoever arrives at the repository reads it in English. The app stays bilingual (IT default, EN) and does not change. Choices in `docs/decisions.md` ("Toolchain, build and CI", "Repository language"). They are translations without design choices: Sonnet.
Last closed phases: A1 and P1 (12/10/2026), in git history and in `docs/decisions.md`.

| Phase | Model | In brief |
|---|---|---|
| I1 | Sonnet | `SPEC.md`, `docs/decisioni.md` → `docs/decisions.md` (with all references), `docs/roadmap.md`, `REVIEW.md`, `tools/seedgen/README.md`, `seed/README.md` in English |
| I2 | Sonnet | `README.md`, `CHANGELOG.md` (all of it, history too), seedgen's CHANGELOG line in English, `CLAUDE.md` last; closes #36 |

### I1 — Specification and project documents in English (Sonnet)
**Goal:** the reference documents (specification, decisions, roadmap, review, seedgen) in English, without losing or changing content.

**Prerequisites:** this plan in `main`.

**Includes**
1. `SPEC.md` (~44 KB): faithful translation, same structure and same section numbers (§…), because code and documents cite them.
2. `docs/decisioni.md` → `docs/decisions.md` with `git mv` (history preserved), translated. **All** references to the old name updated: documents, `CHANGELOG.md`, `app/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `UpdatePreferences.kt`, `tools/seedgen/seedgen/build.py` (in the comments only the file name changes, the comment stays in Italian). `.claude/skills/next-phase` already accepts both names: not touched (it belongs to the kit).
3. `docs/roadmap.md` (this section and the "Common to all phases" included), `REVIEW.md`, `tools/seedgen/README.md`, `seed/README.md` (one line, it is not game data nor a generated file: seedgen writes only the `*.json`).
4. Names of strings, commands, keys, proper names and quotations of app texts (e.g. "Consigliami", "Non ora") stay as they are; the quoted UI texts may be paired with the `values-en/` version.

**Out of scope:** `README.md`, `CHANGELOG.md`, `CLAUDE.md` (I2); comments in the code (they stay in Italian, decision of 17/10/2026); UI strings; names in the seed (`nameIt`).

**Done when**
- No reference to `decisioni.md` left (`grep -r decisioni` empty outside `.claude/` and history); relative links work.
- No content lost: same sections and same lists as the Italian text (section-by-section comparison in the review).
- The common points below; CI green (documents and comments only).
- **On-screen checks:** none (the app does not change). On the GitHub site: the documents read well and the links go where they should.

### I2 — README, CHANGELOG and CLAUDE.md in English (Sonnet)
**Goal:** the repository page, the CHANGELOG (and therefore the release notes) and the agent's memory in English; from now on commits, PRs and issues in English.

**Prerequisites:** I1 in `main`.

**Includes**
1. `README.md` in English.
2. `CHANGELOG.md` **all** in English, history included (author's choice), same `- **Summary.** detail` convention that `release.yml` reads. The release notes already published on GitHub stay in Italian.
3. `tools/seedgen/seedgen/cli.py`: the line ready for the CHANGELOG ("Dati di gioco aggiornati da…") in English, with a pytest test that checks it; `app/build.gradle.kts` inserts it unchanged.
4. `CLAUDE.md` last, translated: "Convenzioni" becomes "documentation, CHANGELOG, commit, PR and issues in English; UI IT (`values/`) + EN (`values-en/`); code comments may stay Italian"; the exception for `docs/ci.md`/`docs/claude.md` removed. It stays under ~15 KB (today 15.4 KB: "Stato attuale" is shortened by moving the details of closed phases to `docs/decisions.md` or removing them if already there).
5. `docs/decisions.md`: the entry "Divergences: README/CHANGELOG in Italian" replaced by the decision of 17/10/2026.

**Out of scope:** code comments and Gradle/seedgen messages for the developer (apart from the CHANGELOG line); release notes already published; a third language; bilingual news dialog (discarded on 17/10/2026).

**Done when**
- Pytest test of the CHANGELOG line; the common points below; CI green.
- `Fixes #36` in the PR.
- **On-screen checks** (at the first release after I2): with the app in Italian the update dialog shows the news titles in English, with no cut-off or empty text; "Changelog completo" opens the English CHANGELOG.

**Open questions:** none.
**Risks:** a translation that changes the meaning of a `CLAUDE.md` rule or of a decision (mitigation: section-by-section review with the Italian text alongside, in the PR diff); links broken by the rename (mitigation: grep in "Done when"); `CLAUDE.md` exceeding 15 KB.

---

## Common to all phases
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` and `cd tools/seedgen && python -m pytest` pass.
- CHANGELOG updated in the project's format; README updated if functions, commands or prerequisites change; `SPEC.md` updated if the app's behaviour changes.
- `CLAUDE.md`: "Stato attuale" updated (phase completed, next phase, handoff if an Opus phase); non-obvious choices in `docs/decisions.md`; the closed phase leaves this file.
- Final summary to the author: what was done, deviations and why, what remains open, on-screen checks.

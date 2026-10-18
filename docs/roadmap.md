# Roadmap

The upcoming work phases. Each phase has an **assigned model**: a session that starts with "vai avanti con la prossima fase" checks it first (step 0 of the protocol in `CLAUDE.md`). Closed phases leave this file (they stay in git history and, for the choices, in `docs/decisions.md`): the original roadmap 1–8 was closed by 0.1.x.
General rule: **out of scope = everything that belongs to later phases**, even if it seems convenient to do it now.

## Open phases
None. Phases I1 and I2 (repository in English, issue #36) closed on 18/10/2026; the choices are in `docs/decisions.md` ("Toolchain, build and CI", "Repository language"). Earlier closed phases are in git history and in `docs/decisions.md`. New work: `/new-plan`.

---

## Common to all phases
- `./gradlew lint`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug` and `cd tools/seedgen && python -m pytest` pass.
- CHANGELOG updated in the project's format; README updated if functions, commands or prerequisites change; `SPEC.md` updated if the app's behaviour changes.
- `CLAUDE.md`: "Current status" updated (phase completed, next phase, handoff if an Opus phase); non-obvious choices in `docs/decisions.md`; the closed phase leaves this file.
- Final summary to the author: what was done, deviations and why, what remains open, on-screen checks.

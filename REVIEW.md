# REVIEW.md — what a KartLog review must always check

Full rules in `CLAUDE.md`, choices and reasons in `docs/decisions.md`. Here only what must always be checked.

## What "Important" means here
- **Important**: violates a non-negotiable rule of `CLAUDE.md`, loses or corrupts user state (Room migrations, backup, reseed), or is a demonstrable bug.
- **Nit**: everything else (names, style, preferences).

## Always check
- **Game data**: no invented data, none written in code or tests; `seed/` is not edited by hand (only by `tools/seedgen`); Kotlin tests use the `FAKE_FOR_TESTS` seed.
- **Counts**: `tools/seedgen/expected_counts.yaml` is not relaxed to make a build or test pass; if it changes, it is a dedicated commit citing the source.
- **Nintendo assets**: no renders, logos, fonts or screenshots in the repository or the APK (exception: `res/drawable-nodpi/home_*` and `kart_*`). Game images are only seed URLs. No "Mario", "Mario Kart" or "Nintendo" in the app name or package.
- **Network**: `INTERNET` only for images; no network call for game data at runtime (only `assets/seed/*.json` assets, never `seed/`).
- **Room**: every schema change has a version, a written and tested migration, a schema in `app/schemas/`; never `fallbackToDestructiveMigration()`. Game data IDs = seedgen slugs, never autoincrement; stand IDs do not enter user state.
- **Layers**: `domain/` has no Android dependencies (and no `ui/` imports); the Consigliami logic lives in `domain/consigliami/`, not in the ViewModel or the DAO (the DAO exposes raw `Flow`s); one DAO per screen, a single `Flow`.
- **Localization**: every visible string in `values/` and `values-en/`; game names go through `localizedName()` and ViewModels that localize go through `relocalizing()`.
- **Graphics**: new screens use the `Kart*` components (`ui/common/KartChrome.kt`, `Modifier.kartSky`); never `FilterChip`; never `BoxWithConstraints`/`SubcomposeLayout` where the parent asks for intrinsic measurements. Grey = you are missing it, coloured = you have it.
- **Navigation**: every `navigate()`/`popBackStack()` goes through the `lifecycleIsResumed()` guard; `@Serializable` routes in `Destination`.
- **Characters**: the 32 `starter` drivers are always unlocked in queries (`SkinDao`, `ConsigliamiDao`); the default outfit is always in `owned_outfits`.
- **Documents**: CHANGELOG `[Unreleased]` for every visible change (`- **Summary.** detail`); `CLAUDE.md` "Current status" and `docs/decisions.md` updated if the change makes them false.

## Verification threshold
A claim about behaviour cites `file:line`. A rule is violated only if the line is given. On-screen verification: there is no emulator here, the author does it.

## Do not report
Formatting, naming taste, what Android Lint already reports, known warnings (`createComposeRule` deprecated in Compose tests, `targetSdk` 36 intended).

## Format
At most 10 findings, in order of severity: `[Important|Nit] path:line — rule violated (source) — what to do`.

"""Riga di comando di seedgen. Tutti i percorsi relativi sono rispetto alla directory corrente.

Comandi:
  fetch-fixtures  Scarica le pagine sorgente e le salva come fixture (per sviluppare/testare il parser).
  generate        Parse -> build -> validate -> scrive i JSON in --out.
  validate        Valida un seed esistente (nessuna rete).
  check           Confronta un seed candidato con seed/: exit 0 se identico, 3 se diverso.
  accept          Copia il candidato in seed/ e incrementa seedVersion.
  release         Entry point per Gradle: generate dalla rete in --work, poi check contro --seed.

Exit code: vedi errors.py (0 ok, 1 uso, 2 dati non validi, 3 dati cambiati, 4 rete).
"""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

from . import wiki
from .build import build
from .config import Config
from .errors import EXIT_CHANGED, EXIT_OK, SeedgenError, ValidationError
from .i18n import Translations
from .output import COMPARED_FILES, diff, read_seed, write_seed
from .parse import parse_all, parse_translations
from .raw import RawData
from .validate import validate


def _log(msg: str) -> None:
    print(f"[seedgen] {msg}", file=sys.stderr)


def _current_version(seed_dir: Path | None) -> int:
    if seed_dir and (seed_dir / "meta.json").is_file():
        return int(read_seed(seed_dir)["meta.json"].get("seedVersion", 1))
    return 1


def _raw_from_args(args, cfg: Config) -> tuple[RawData, dict[str, wiki.WikiPage] | None]:
    if args.from_raw:
        _log(f"lettura dati grezzi da {args.from_raw}")
        # Nessuna pagina scaricata in questa modalità (usata per verificare la normalizzazione a
        # partire da una trascrizione, non i nomi in altre lingue): niente traduzioni disponibili.
        return RawData.from_yaml(Path(args.from_raw)), None
    if args.from_fixtures:
        _log(f"parsing delle fixture in {args.from_fixtures}")
        pages = wiki.load_fixtures(cfg, Path(args.from_fixtures))
    else:
        _log("download delle pagine dal wiki")
        pages = wiki.fetch_all(cfg)
    return parse_all(pages, cfg), pages


def cmd_fetch_fixtures(args, cfg: Config) -> int:
    pages = wiki.fetch_all(cfg)
    wiki.save_fixtures(list(pages.values()), Path(args.out))
    for p in pages.values():
        _log(f"salvata {p.title} @ rev {p.revid}")
    return EXIT_OK


def _generate(args, cfg: Config, out: Path, seed_dir: Path | None) -> dict:
    raw, pages = _raw_from_args(args, cfg)
    if getattr(args, "dump_raw", None):
        raw.to_yaml(Path(args.dump_raw))
    # I nomi in altre lingue si scaricano sempre dal vivo (mai da fixture: vedi wiki.i18n_titles),
    # solo per una generate reale — non per --from-raw/--from-fixtures, dev-mode pensate per non
    # toccare la rete.
    live = not args.from_raw and not args.from_fixtures
    translations = parse_translations(wiki.fetch_i18n(cfg), cfg) if live else Translations()
    seed = build(raw, cfg, seed_version=_current_version(seed_dir), translations=translations)
    validate(seed, cfg)
    write_seed(seed, out)
    _log(f"seed valido scritto in {out}")
    return seed


def cmd_generate(args, cfg: Config) -> int:
    _generate(args, cfg, Path(args.out), Path(args.seed) if args.seed else None)
    return EXIT_OK


def cmd_validate(args, cfg: Config) -> int:
    validate(read_seed(Path(args.seed)), cfg)
    _log(f"{args.seed}: valido")
    return EXIT_OK


def _check(seed_dir: Path, candidate_dir: Path) -> int:
    changes = diff(read_seed(seed_dir), read_seed(candidate_dir))
    if not changes:
        _log("dati identici a seed/")
        return EXIT_OK
    print("I dati estratti dal wiki sono DIVERSI da seed/:")
    for line in changes:
        print("  " + line)
    print("Rivedere le differenze. Per accettarle rilanciare la build con -PacceptSeedChanges.")
    return EXIT_CHANGED


def cmd_check(args, cfg: Config) -> int:
    return _check(Path(args.seed), Path(args.candidate))


def cmd_accept(args, cfg: Config) -> int:
    seed_dir, candidate = Path(args.seed), Path(args.candidate)
    new = read_seed(candidate)
    validate(new, cfg)
    changes = diff(read_seed(seed_dir), new) if seed_dir.exists() else ["seed iniziale"]
    new["meta.json"]["seedVersion"] = _current_version(seed_dir) + (1 if changes else 0)
    seed_dir.mkdir(parents=True, exist_ok=True)
    for name in [*COMPARED_FILES, "meta.json"]:
        if (candidate / name).is_file():
            shutil.copyfile(candidate / name, seed_dir / name)
        elif (seed_dir / name).is_file():
            (seed_dir / name).unlink()
    write_seed({"meta.json": new["meta.json"]}, seed_dir)
    revs = ", ".join(f"{s['title']}@{s['revid']}" for s in new["meta.json"]["sources"])
    # Riga pronta per il CHANGELOG: il task Gradle la inserisce nel formato del progetto.
    print(f"Dati di gioco aggiornati da Super Mario Wiki (seedVersion {new['meta.json']['seedVersion']}; {revs})")
    return EXIT_OK


def cmd_release(args, cfg: Config) -> int:
    seed_dir, work = Path(args.seed), Path(args.work)
    if args.offline:
        validate(read_seed(seed_dir), cfg)
        _log("ATTENZIONE: -PofflineSeed attivo, uso seed/ versionato senza verificarlo contro il wiki")
        return EXIT_OK
    if work.exists():
        shutil.rmtree(work)
    args.from_raw = args.from_fixtures = None
    _generate(args, cfg, work, seed_dir)
    return _check(seed_dir, work)


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="python -m seedgen", description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="command", required=True)

    s = sub.add_parser("fetch-fixtures", help="scarica le pagine sorgente come fixture")
    s.add_argument("--out", default="tests/fixtures/real")
    s.set_defaults(func=cmd_fetch_fixtures)

    s = sub.add_parser("generate", help="genera e valida il seed")
    s.add_argument("--out", required=True)
    s.add_argument("--seed", help="seed/ esistente, per conservare seedVersion")
    src = s.add_mutually_exclusive_group()
    src.add_argument("--from-fixtures", help="usa fixture salvate invece della rete")
    src.add_argument("--from-raw", help="usa un file RawData YAML (es. tests/golden/...)")
    s.add_argument("--dump-raw", help="salva anche i dati grezzi estratti in YAML")
    s.set_defaults(func=cmd_generate)

    s = sub.add_parser("validate", help="valida un seed esistente")
    s.add_argument("--seed", required=True)
    s.set_defaults(func=cmd_validate)

    s = sub.add_parser("check", help="confronta un candidato con seed/")
    s.add_argument("--seed", required=True)
    s.add_argument("--candidate", required=True)
    s.set_defaults(func=cmd_check)

    s = sub.add_parser("accept", help="accetta il candidato in seed/")
    s.add_argument("--seed", required=True)
    s.add_argument("--candidate", required=True)
    s.set_defaults(func=cmd_accept)

    s = sub.add_parser("release", help="entry point del task Gradle generateSeed")
    s.add_argument("--seed", required=True)
    s.add_argument("--work", required=True, help="directory di lavoro (es. app/build/seedgen/candidate)")
    s.add_argument("--offline", action="store_true")
    s.add_argument("--dump-raw")
    s.set_defaults(func=cmd_release)
    return p


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        cfg = Config.load()
        return args.func(args, cfg)
    except ValidationError as exc:
        _log(str(exc))
        return exc.exit_code
    except SeedgenError as exc:
        _log(f"ERRORE: {exc}")
        return exc.exit_code

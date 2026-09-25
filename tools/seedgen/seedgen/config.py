"""Caricamento di sources.yaml, aliases.yaml ed expected_counts.yaml."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import yaml

from .errors import SeedgenError, UnknownNameError

TOOL_DIR = Path(__file__).resolve().parent.parent


def _load_yaml(path: Path) -> dict:
    if not path.is_file():
        raise SeedgenError(f"File di configurazione mancante: {path}")
    with path.open(encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


@dataclass(frozen=True)
class Entity:
    id: str
    name: str
    order: int
    wiki_page: str | None = None  # titolo pagina per seedgen/i18n.py, se diverso da `name`

    def i18n_page(self) -> str:
        return self.wiki_page or self.name


class AliasTable:
    """Risolve un nome letto dal wiki in uno slug stabile. Nome sconosciuto = errore."""

    def __init__(self, kind: str, entries: dict[str, dict], ignored: list[str] | None = None):
        self.kind = kind
        self.entities: list[Entity] = []
        self._lookup: dict[str, str] = {}
        self._ignored = {self._key(x) for x in (ignored or [])}
        for order, (slug, spec) in enumerate(entries.items()):
            name = spec["name"]
            self.entities.append(Entity(slug, name, order, spec.get("wikiPage")))
            for alias in [name, *spec.get("aliases", [])]:
                key = self._key(alias)
                if key in self._lookup and self._lookup[key] != slug:
                    raise SeedgenError(f"Alias duplicato in aliases.yaml ({kind}): {alias!r}")
                self._lookup[key] = slug

    @staticmethod
    def _key(name: str) -> str:
        return " ".join(name.replace(" ", " ").split()).casefold()

    def is_ignored(self, name: str) -> bool:
        return self._key(name) in self._ignored

    def knows(self, name: str) -> bool:
        return self._key(name) in self._lookup

    def resolve(self, name: str, context: str = "") -> str:
        key = self._key(name)
        if key not in self._lookup:
            where = f" (in {context})" if context else ""
            raise UnknownNameError(
                f"{self.kind} sconosciuto{where}: {name!r}. "
                f"Aggiungerlo ad aliases.yaml se è un nome legittimo."
            )
        return self._lookup[key]

    def name_of(self, slug: str) -> str:
        return next(e.name for e in self.entities if e.id == slug)


@dataclass
class Config:
    sources: dict
    characters: AliasTable
    courses: AliasTable
    expected: dict
    equivalences: list
    food_groups: AliasTable
    yoshi_labels: dict[str, list[str]]
    regions: AliasTable
    region_of: dict[str, str]          # courseId / areaId -> regionId
    areas: AliasTable
    medallions: dict
    character_genders: dict[str, str]  # characterId -> "M"/"F", solo per seedgen/i18n.py

    def yoshi_label_groups(self, label: str, context: str = "") -> list[str]:
        key = AliasTable._key(label)
        if key not in self.yoshi_labels:
            where = f" (in {context})" if context else ""
            raise UnknownNameError(
                f"Etichetta di cibo sconosciuta{where}: {label!r}. Aggiungerla a yoshi_food_labels in aliases.yaml."
            )
        return self.yoshi_labels[key]

    @classmethod
    def load(cls, tool_dir: Path = TOOL_DIR) -> "Config":
        sources = _load_yaml(tool_dir / "sources.yaml")
        aliases = _load_yaml(tool_dir / "aliases.yaml")
        expected = _load_yaml(tool_dir / "expected_counts.yaml")
        region_of: dict[str, str] = {}
        for rid, spec in aliases["regions"].items():
            for cid in spec["courses"]:
                if cid in region_of:
                    raise SeedgenError(f"aliases.yaml: il corso {cid} è in due regioni")
                region_of[cid] = rid
        for aid, spec in aliases.get("areas", {}).items():
            region_of[aid] = spec["region"]
        return cls(
            sources=sources,
            characters=AliasTable("Personaggio", aliases["characters"]),
            courses=AliasTable("Corso", aliases["courses"], aliases.get("ignored_course_links")),
            expected=expected,
            equivalences=aliases.get("equivalences", []),
            food_groups=AliasTable("Gruppo di cibo", aliases["food_groups"]),
            yoshi_labels={AliasTable._key(k): v for k, v in aliases["yoshi_food_labels"].items()},
            regions=AliasTable("Regione", aliases["regions"]),
            region_of=region_of,
            areas=AliasTable("Luogo", aliases.get("areas", {})),
            medallions=_load_yaml(tool_dir / "manual" / "peach_medallions.yaml"),
            character_genders=_load_yaml(tool_dir / "manual" / "character_genders.yaml"),
        )

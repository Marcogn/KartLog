"""Dati grezzi estratti dalle pagine, prima della normalizzazione.

I nomi sono quelli del wiki (es. "Bowser Jr.", "Mario Circuit (Mario Kart World)"). La RawData si può
salvare in YAML: è il formato del file di riferimento in tests/golden/, usato per il seed iniziale e
come controllo incrociato del parser.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from pathlib import Path

import yaml


@dataclass
class RawOutfit:
    character: str
    outfit: str


@dataclass
class RawFoodVariant:
    name: str                             # seconda colonna Name ("Hamburger", "Canned juice", "Sushi")
    boost: str                            # colonna "Boost level" ("Small", "Small, Medium, and Large (respectively)")
    image_url: str                        # file originale sul CDN del wiki (seedgen/images.py, original_url)


@dataclass
class RawFoodGroup:
    names: list[str]                      # nomi dei cibi nella cella Name (1 o più, es. Takoyaki/.../Sushi)
    outfits: list[RawOutfit]
    courses: list[str]                    # titoli dei link nella colonna Locations
    reverts_to_default: bool = False
    variants: list[RawFoodVariant] = field(default_factory=list)   # una per riga della tabella, in ordine


@dataclass
class RawEvent:
    name: str
    courses: list[str]                    # in ordine di percorrenza


@dataclass
class RawStand:
    """Uno stand Yoshi's = una riga (con la sua foto) di "List of Yoshi's locations"."""
    course: str | None                    # intestazione h3 in "Course locations"; None = "Route locations"
    foods: list[str]                      # etichette della colonna Dash Food ([] = cella vuota)
    establishment: str | None = None      # "Snack bar", "Food truck"…; le strade non hanno la colonna
    location: str | None = None           # colonna Location; None = cella vuota (luogo non indicato)


@dataclass
class RawMission:
    region: str                           # intestazione h2 (bioma)
    name: str                             # colonna "In-game text"
    location: str                         # colonna "Location" (corso o luogo)


@dataclass
class RawSource:
    title: str
    revid: int | None                     # None = trascrizione manuale, non estratta dall'API
    url: str | None = None                # None = pagina di mariowiki.com (URL ricavato dal titolo)


@dataclass
class RawData:
    food_groups: list[RawFoodGroup] = field(default_factory=list)
    cups: list[RawEvent] = field(default_factory=list)
    rallies: list[RawEvent] = field(default_factory=list)
    stands: list[RawStand] = field(default_factory=list)
    missions: list[RawMission] = field(default_factory=list)
    sources: list[RawSource] = field(default_factory=list)
    origin: str = "api"                   # "api" | "manual-transcription"
    note: str = ""

    def to_yaml(self, path: Path) -> None:
        path.write_text(
            yaml.safe_dump(asdict(self), allow_unicode=True, sort_keys=False, width=120),
            encoding="utf-8",
        )

    @classmethod
    def from_yaml(cls, path: Path) -> "RawData":
        data = yaml.safe_load(path.read_text(encoding="utf-8"))
        return cls(
            food_groups=[
                RawFoodGroup(
                    names=g["names"],
                    outfits=[RawOutfit(**o) for o in g["outfits"]],
                    courses=g["courses"],
                    reverts_to_default=g.get("reverts_to_default", False),
                    variants=[RawFoodVariant(**v) for v in g.get("variants", [])],
                )
                for g in data["food_groups"]
            ],
            cups=[RawEvent(**e) for e in data["cups"]],
            rallies=[RawEvent(**e) for e in data["rallies"]],
            # La trascrizione manuale del 25/09/2026 ha `course_stands` nel formato di allora (cibi per
            # corso, senza righe né luoghi): non si converte, gli stand si verificano sulle fixture reali.
            stands=[RawStand(**x) for x in data.get("stands", [])],
            missions=[RawMission(**x) for x in data.get("missions", [])],
            sources=[RawSource(**s) for s in data.get("sources", [])],
            origin=data.get("origin", "api"),
            note=data.get("note", ""),
        )

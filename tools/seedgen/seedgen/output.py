"""Scrittura deterministica dei JSON e confronto con seed/ esistente."""

from __future__ import annotations

import json
from pathlib import Path

# meta.json contiene data di estrazione e revid: cambia a ogni estrazione anche a dati invariati,
# quindi NON entra nel confronto.
COMPARED_FILES = [
    "characters.json", "outfits.json", "food_groups.json", "outfit_food_rules.json",
    "food_group_courses.json", "courses.json", "events.json", "regions.json", "areas.json",
    "peach_medallions.json", "p_switches.json",
]


def dumps(data: dict) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def write_seed(seed: dict[str, dict], directory: Path) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    for name, content in seed.items():
        (directory / name).write_text(dumps(content), encoding="utf-8")


def read_seed(directory: Path) -> dict[str, dict]:
    seed = {}
    for path in sorted(directory.glob("*.json")):
        seed[path.name] = json.loads(path.read_text(encoding="utf-8"))
    return seed


def _ids(seed: dict, name: str, key=lambda i: i["id"]) -> set:
    return {key(i) for i in seed.get(name, {}).get("items", [])}


def diff(old: dict[str, dict], new: dict[str, dict]) -> list[str]:
    """Riepilogo leggibile delle differenze. Lista vuota = dati identici."""
    lines: list[str] = []

    def added_removed(label: str, a: set, b: set) -> None:
        for x in sorted(b - a):
            lines.append(f"+ {label}: {x}")
        for x in sorted(a - b):
            lines.append(f"- {label}: {x}")

    added_removed("personaggio", _ids(old, "characters.json"), _ids(new, "characters.json"))
    added_removed("outfit", _ids(old, "outfits.json"), _ids(new, "outfits.json"))
    added_removed("gruppo di cibo", _ids(old, "food_groups.json"), _ids(new, "food_groups.json"))
    rule = lambda i: f"{i['outfitId']} <- {i['foodGroupId']}"
    added_removed("regola", _ids(old, "outfit_food_rules.json", rule), _ids(new, "outfit_food_rules.json", rule))
    loc = lambda i: f"{i['foodGroupId']} @ {i['courseId']} [{i.get('presence', '?')}]"
    added_removed("cibo sul corso", _ids(old, "food_group_courses.json", loc), _ids(new, "food_group_courses.json", loc))
    added_removed("corso", _ids(old, "courses.json"), _ids(new, "courses.json"))
    added_removed("evento", _ids(old, "events.json"), _ids(new, "events.json"))
    added_removed("regione", _ids(old, "regions.json"), _ids(new, "regions.json"))
    ps = lambda i: f"{i['id']} {i['name']!r} @ {i['courseId'] or i['areaId']}"
    added_removed("pulsante P", _ids(old, "p_switches.json", ps), _ids(new, "p_switches.json", ps))
    med = lambda i: i["regionId"]
    old_med = [med(i) for i in old.get("peach_medallions.json", {}).get("items", [])]
    new_med = [med(i) for i in new.get("peach_medallions.json", {}).get("items", [])]
    for r in sorted(set(old_med) | set(new_med)):
        if old_med.count(r) != new_med.count(r):
            lines.append(f"~ medaglioni in {r}: {old_med.count(r)} -> {new_med.count(r)}")

    old_events = {e["id"]: e for e in old.get("events.json", {}).get("items", [])}
    for e in new.get("events.json", {}).get("items", []):
        prev = old_events.get(e["id"])
        if prev and prev["stops"] != e["stops"]:
            lines.append(f"~ tappe di {e['name']}: {prev['stops']} -> {e['stops']}")

    # Qualsiasi altra differenza (nomi, ordine, attributi) non coperta sopra.
    if not lines:
        for name in COMPARED_FILES:
            if dumps(old.get(name, {})) != dumps(new.get(name, {})):
                lines.append(f"~ {name}: contenuto cambiato (nomi, ordine o attributi)")
    return lines

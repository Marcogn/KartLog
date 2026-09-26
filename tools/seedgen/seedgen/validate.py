"""Validazione del seed generato. Qualsiasi problema -> ValidationError (exit code 2)."""

from __future__ import annotations

from collections import Counter, defaultdict

from .config import Config
from .errors import ValidationError


# CDN di Super Mario Wiki: l'unica origine ammessa per gli URL delle immagini.
IMAGE_URL_PREFIX = "https://mario.wiki.gallery/images/"


def _items(seed: dict, name: str) -> list[dict]:
    return seed[name]["items"]


def validate(seed: dict, cfg: Config) -> None:
    exp = cfg.expected
    problems: list[str] = []

    def check(cond: bool, msg: str) -> None:
        if not cond:
            problems.append(msg)

    characters = _items(seed, "characters.json")
    outfits = _items(seed, "outfits.json")
    groups = _items(seed, "food_groups.json")
    rules = _items(seed, "outfit_food_rules.json")
    group_courses = _items(seed, "food_group_courses.json")
    courses = _items(seed, "courses.json")
    events = _items(seed, "events.json")

    # --- ID unici --------------------------------------------------------------------------
    for name, items, key in [
        ("characters", characters, "id"), ("outfits", outfits, "id"), ("food_groups", groups, "id"),
        ("courses", courses, "id"), ("events", events, "id"),
    ]:
        dup = [k for k, n in Counter(i[key] for i in items).items() if n > 1]
        check(not dup, f"ID duplicati in {name}: {dup}")

    char_ids = {c["id"] for c in characters}
    outfit_ids = {o["id"] for o in outfits}
    group_ids = {g["id"] for g in groups}
    course_ids = {c["id"] for c in courses}

    # --- Conteggi --------------------------------------------------------------------------
    alt = [o for o in outfits if not o["isDefault"]]
    with_outfits = {o["characterId"] for o in alt}
    check(len(with_outfits) == exp["characters_with_outfits"],
          f"personaggi con outfit: {len(with_outfits)}, attesi {exp['characters_with_outfits']}")
    check(len(alt) == exp["alternative_outfits"],
          f"outfit alternativi: {len(alt)}, attesi {exp['alternative_outfits']}")
    check(len(outfits) == exp["total_outfits_including_default"],
          f"outfit totali: {len(outfits)}, attesi {exp['total_outfits_including_default']}")
    check(len(groups) == exp["food_groups"], f"gruppi di cibo: {len(groups)}, attesi {exp['food_groups']}")
    reverting = [g for g in groups if g["revertsToDefault"]]
    check(len(reverting) == exp["food_groups_reverting_to_default"],
          f"gruppi che riportano al default: {len(reverting)}, attesi {exp['food_groups_reverting_to_default']}")
    check(len(courses) == exp["courses"], f"corsi: {len(courses)}, attesi {exp['courses']}")

    cups = [e for e in events if e["type"] == "CUP"]
    rallies = [e for e in events if e["type"] == "RALLY"]
    check(len(cups) == exp["cups"], f"cup: {len(cups)}, attese {exp['cups']}")
    check(len(rallies) == exp["rallies"], f"rally: {len(rallies)}, attesi {exp['rallies']}")
    for e in cups:
        check(len(e["stops"]) == exp["courses_per_cup"], f"{e['name']}: {len(e['stops'])} corsi, attesi {exp['courses_per_cup']}")
    for e in rallies:
        check(len(e["stops"]) == exp["stops_per_rally"], f"{e['name']}: {len(e['stops'])} tappe, attese {exp['stops_per_rally']}")

    # --- Integrità referenziale ----------------------------------------------------------------
    for o in outfits:
        check(o["characterId"] in char_ids, f"outfit {o['id']}: personaggio inesistente")
    for r in rules:
        check(r["outfitId"] in outfit_ids, f"regola: outfit inesistente {r['outfitId']}")
        check(r["foodGroupId"] in group_ids, f"regola: gruppo inesistente {r['foodGroupId']}")
    for gc in group_courses:
        check(gc["foodGroupId"] in group_ids, f"food_group_courses: gruppo inesistente {gc['foodGroupId']}")
        check(gc["courseId"] in course_ids, f"food_group_courses: corso inesistente {gc['courseId']}")
    for e in events:
        for s in e["stops"]:
            check(s in course_ids, f"{e['name']}: corso inesistente {s}")

    # --- Coerenza di dominio -------------------------------------------------------------------
    ruled = {r["outfitId"] for r in rules}
    orphan = sorted(o["id"] for o in alt if o["id"] not in ruled)
    check(not orphan, f"outfit alternativi senza alcun cibo che li sblocca: {orphan}")
    for g in groups:
        has_rules = any(r["foodGroupId"] == g["id"] for r in rules)
        if g["revertsToDefault"]:
            check(not has_rules, f"gruppo {g['id']}: riporta al default ma ha regole outfit")
        else:
            check(has_rules, f"gruppo {g['id']}: nessun outfit")
        check(any(gc["foodGroupId"] == g["id"] for gc in group_courses), f"gruppo {g['id']}: nessun corso in Locations")
    covered = {s for e in cups for s in e["stops"]}
    check(covered == course_ids, f"corsi non coperti da nessuna cup: {sorted(course_ids - covered)}")

    # Equivalenze dichiarate dal wiki (Baby Peach/Daisy/Rosalina; Toad/Toadette).
    names = {o["id"]: o["name"] for o in outfits}
    owner = {o["id"]: o["characterId"] for o in outfits}
    per_group: dict[str, dict[str, str]] = defaultdict(dict)
    for r in rules:
        if r["outfitId"] in names:  # i riferimenti rotti sono già segnalati sopra
            per_group[r["foodGroupId"]][owner[r["outfitId"]]] = names[r["outfitId"]]
    for eq in cfg.equivalences:
        chars, mapping = eq["characters"], eq.get("outfit_map", {})
        for gid, by_char in per_group.items():
            seen = {c: mapping.get(by_char[c], by_char[c]) if c in by_char else None for c in chars}
            check(len(set(seen.values())) == 1, f"gruppo {gid}: equivalenza violata {seen}")

    # --- Cibo sul percorso: la fonte precisa deve essere coerente con Dash Food ------------------
    for gc in group_courses:
        check(gc["presence"] in ("ON_COURSE", "NEARBY"), f"presenza non valida: {gc}")
        if gc["presence"] == "ON_COURSE":
            check(gc["listedInDashFood"],
                  f"{gc['foodGroupId']} su {gc['courseId']}: presente per List of Yoshi's locations ma non per Dash Food")

    # --- Regioni ------------------------------------------------------------------------------
    regions = _items(seed, "regions.json")
    region_ids = {r["id"] for r in regions}
    check(len(regions) == exp["regions"], f"regioni: {len(regions)}, attese {exp['regions']}")
    no_region = sorted(c["id"] for c in courses if c.get("regionId") is None)
    check(no_region == ["rainbow_road"], f"corsi senza regione (atteso solo rainbow_road): {no_region}")
    for c in courses:
        check(c.get("regionId") in region_ids | {None}, f"corso {c['id']}: regione inesistente")
    area_region = {a["id"]: a["regionId"] for a in _items(seed, "areas.json")}
    course_region = {c["id"]: c.get("regionId") for c in courses}

    # --- Peach Medallions (file manuale) ---------------------------------------------------------
    meds = _items(seed, "peach_medallions.json")
    check(len(meds) == exp["peach_medallions"], f"Peach Medallions: {len(meds)}, attesi {exp['peach_medallions']}")
    for m in meds:
        check(m["regionId"] in region_ids, f"medaglione {m['id']}: regione inesistente {m['regionId']}")
    check({m["regionId"] for m in meds} == region_ids, "Peach Medallions: non tutte le regioni hanno medaglioni")

    # --- Pulsanti P (assenti nel seed iniziale trascritto a mano) -------------------------------
    if "p_switches.json" in seed:
        ps = _items(seed, "p_switches.json")
        check(len(ps) == exp["p_switches"], f"pulsanti P: {len(ps)}, attesi {exp['p_switches']}")
        for m in ps:
            place = m["courseId"] or m["areaId"]
            expected_region = course_region.get(m["courseId"]) if m["courseId"] else area_region.get(m["areaId"])
            check(expected_region is not None, f"{m['id']} ({m['name']}): luogo sconosciuto {place}")
            check(m["regionId"] == expected_region,
                  f"{m['id']} ({m['name']}): elencato in {m['regionId']} ma {place} è in {expected_region}")

    # --- Piloti (anche senza outfit) e stato iniziale di sblocco ---------------------------------
    check(len(characters) == exp["drivers"], f"piloti: {len(characters)}, attesi {exp['drivers']}")
    starters = [c.get("starter") for c in characters]
    if any(s is not None for s in starters):
        check(None not in starters, "starter: presente solo su una parte dei piloti")
        check(sum(bool(s) for s in starters) == exp["starter_drivers"],
              f"piloti di base: {sum(bool(s) for s in starters)}, attesi {exp['starter_drivers']}")

    # --- Immagini (seedgen/images.py): solo URL del CDN del wiki, mai file locali ----------------
    for name, items in [("characters", characters), ("outfits", outfits), ("events", events)]:
        for i in items:
            url = i.get("imageUrl")
            check(url is None or url.startswith(IMAGE_URL_PREFIX), f"{name} {i['id']}: imageUrl inatteso {url!r}")

    if problems:
        raise ValidationError(problems)

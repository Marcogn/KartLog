"""Validazione del seed generato. Qualsiasi problema -> ValidationError (exit code 2)."""

from __future__ import annotations

from collections import Counter, defaultdict

from .config import Config
from .errors import ValidationError


# CDN di Super Mario Wiki: l'unica origine ammessa per gli URL delle immagini.
IMAGE_URL_PREFIX = "https://mario.wiki.gallery/images/"
# Mappa di mkworld-checklist (seedgen/checklist.py), a un commit fissato.
MAP_URL_PREFIX = "https://raw.githubusercontent.com/BamisWasTaken/mkworld-checklist/"


def _check_points(name: str, points: list[dict], check) -> None:
    for p in points:
        ok = all(isinstance(p.get(k), (int, float)) and 0 <= p[k] <= 100 for k in ("x", "y"))
        check(ok, f"{name} {p['id']}: posizione sulla mappa mancante o fuori dai limiti")


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

    # --- Mappa, Monete Peach e pannelli "?" (seedgen/checklist.py) ------------------------------
    # Assenti solo in un seed senza mkworld-checklist (trascrizione golden, fixture): o tutti o nessuno.
    map_files = ("peach_medallions.json", "question_panels.json", "map.json")
    present = [name in seed for name in map_files]
    check(all(present) or not any(present), f"mappa: file presenti solo in parte {dict(zip(map_files, present))}")
    if all(present):
        for name, key in [("peach_medallions.json", "peach_medallions"), ("question_panels.json", "question_panels")]:
            points = _items(seed, name)
            check(len(points) == exp[key], f"{name}: {len(points)}, attesi {exp[key]}")
            check(len({p["id"] for p in points}) == len(points), f"{name}: ID duplicati")
            _check_points(name, points, check)
        maps = _items(seed, "map.json")
        check(len(maps) == 1 and maps[0]["imageUrl"].startswith(MAP_URL_PREFIX)
              and maps[0]["width"] > 0 and maps[0]["height"] > 0, f"map.json non valido: {maps}")
        if "p_switches.json" in seed:
            _check_points("p_switches.json", _items(seed, "p_switches.json"), check)

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

        # Criteri di sblocco (seedgen/images.py, seedgen/it_wiki.py): tutti e soli i piloti sbloccabili.
        if any(c.get("unlockCriteria") for c in characters):
            with_it = any(c.get("unlockCriteriaIt") for c in characters)  # mariowiki.it letto
            for c in characters:
                has = bool(c.get("unlockCriteria")) and (bool(c.get("unlockCriteriaIt")) or not with_it)
                check(has != bool(c["starter"]),
                      f"{c['id']}: criterio di sblocco {'inatteso' if c['starter'] else 'mancante'}")

    if "mirror_mode.json" in seed:
        steps = _items(seed, "mirror_mode.json")
        check(bool(steps), "mirror_mode.json: nessuna condizione")
        check(all(s.get("text") and s.get("textIt") for s in steps), "mirror_mode.json: condizione senza testo EN o IT")

    # --- Varianti dei cibi e stand Yoshi's (assenti solo nella trascrizione golden) ---------------
    variant_names: dict[str, set[str]] = defaultdict(set)
    if "food_variants.json" in seed:
        variants = _items(seed, "food_variants.json")
        check(len(variants) == exp["food_variants"], f"varianti dei cibi: {len(variants)}, attese {exp['food_variants']}")
        check(len({v["id"] for v in variants}) == len(variants), "food_variants.json: ID duplicati")
        for v in variants:
            check(v["foodGroupId"] in group_ids, f"variante {v['id']}: gruppo inesistente")
            check(bool(v.get("name")) and bool(v.get("nameIt")), f"variante {v['id']}: nome EN o IT mancante")
            check(bool(v.get("boost")) and set(v["boost"]) <= {"SMALL", "MEDIUM", "LARGE"},
                  f"variante {v['id']}: boost non valido {v.get('boost')!r}")
            url = v.get("imageUrl")
            check(bool(url) and url.startswith(IMAGE_URL_PREFIX), f"variante {v['id']}: imageUrl inatteso {url!r}")
            variant_names[v["foodGroupId"]].add(v["name"])
        check(set(variant_names) == group_ids, f"gruppi senza varianti: {sorted(group_ids - set(variant_names))}")
    if "yoshi_stands.json" in seed:
        stands = _items(seed, "yoshi_stands.json")
        on_courses = [st for st in stands if st["courseId"]]
        check(len(on_courses) == exp["yoshi_course_stands"],
              f"stand Yoshi's sui percorsi: {len(on_courses)}, attesi {exp['yoshi_course_stands']}")
        check(len(stands) - len(on_courses) == exp["yoshi_route_stands"],
              f"stand Yoshi's sulle strade: {len(stands) - len(on_courses)}, attesi {exp['yoshi_route_stands']}")
        check(len({st["id"] for st in stands}) == len(stands), "yoshi_stands.json: ID duplicati")
        region_by_course = {c["id"]: c.get("regionId") for c in courses}
        with_stand = set()
        for st in stands:
            if st["courseId"]:
                check(st["courseId"] in course_ids, f"stand {st['id']}: corso inesistente {st['courseId']}")
                check(st["regionId"] == region_by_course.get(st["courseId"]), f"stand {st['id']}: bioma diverso dal corso")
            else:
                # Le strade non hanno un bioma nella fonte: mai dedotto dalla descrizione.
                check(st["regionId"] is None, f"stand {st['id']} su una strada con bioma {st['regionId']!r}")
                check(bool(st["location"]), f"stand {st['id']} su una strada senza luogo")
            for text, it in (("establishment", "establishmentIt"), ("location", "locationIt")):
                check((st.get(text) is None) == (st.get(it) is None), f"stand {st['id']}: {text} senza traduzione o viceversa")
            for f in st["foods"]:
                check(f["foodGroupId"] in group_ids, f"stand {st['id']}: gruppo inesistente {f['foodGroupId']}")
                if f["food"] is not None and variant_names:
                    check(f["food"] in variant_names[f["foodGroupId"]],
                          f"stand {st['id']}: {f['food']!r} non è un cibo del gruppo {f['foodGroupId']}")
                with_stand.add(f["foodGroupId"])
        check(with_stand == group_ids, f"gruppi di cibo senza stand Yoshi's: {sorted(group_ids - with_stand)}")

    # --- Immagini (seedgen/images.py): solo URL del CDN del wiki, mai file locali ----------------
    for name, items in [("characters", characters), ("outfits", outfits), ("events", events)]:
        for i in items:
            url = i.get("imageUrl")
            check(url is None or url.startswith(IMAGE_URL_PREFIX), f"{name} {i['id']}: imageUrl inatteso {url!r}")

    if problems:
        raise ValidationError(problems)

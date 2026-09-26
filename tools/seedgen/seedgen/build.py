"""Normalizzazione: RawData (nomi del wiki) -> contenuto dei file JSON di seed/ (slug stabili)."""

from __future__ import annotations

import re
import unicodedata
from datetime import date

from .config import Config
from .errors import ParseError
from .i18n import Translations
from .images import Images
from .it_wiki import ItNames, norm
from .raw import RawData

LICENSE = {
    "name": "CC BY-SA 4.0",
    "url": "https://creativecommons.org/licenses/by-sa/4.0/",
    "attribution": "Dati di gioco tratti da Super Mario Wiki (mariowiki.com) e Super Mario Wiki italiana (mariowiki.it)",
}


def slugify(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    text = text.replace("?", " q ").replace("&", " and ")
    return re.sub(r"[^a-z0-9]+", "_", text.casefold()).strip("_")


def _page_url(cfg: Config, title: str) -> str:
    return cfg.sources["page_url_prefix"] + title.replace(" ", "_")


def _resolve_images(images: Images, cfg: Config) -> tuple[dict[str, str], dict[str, str], dict[str, str]]:
    """Nomi del wiki -> slug. Ogni pilota della pagina deve essere in aliases.yaml (characters o drivers)."""
    by_character = {cfg.resolve_driver(title, "Mario Kart World / drivers"): url for title, url in images.characters.items()}
    by_outfit = {
        f"{cfg.characters.resolve(character, 'Mario Kart World / Character outfits')}__{slugify(outfit)}": url
        for (character, outfit), url in images.outfits.items()
    }
    return by_character, by_outfit, dict(images.events)


def _resolve_it_drivers(it: ItNames, cfg: Config) -> dict[str, str]:
    """Slug -> nome italiano, via langlink (o `itWikiPage` in aliases.yaml se il langlink manca)."""
    by_it_page = {e.it_wiki_page: e.id for e in cfg.all_drivers() if e.it_wiki_page}
    result: dict[str, str] = {}
    for it_title, it_name in it.drivers.items():
        if it_title in by_it_page:
            slug = by_it_page[it_title]
        elif it_title in it.langlinks:
            slug = cfg.resolve_driver(it.langlinks[it_title], f"mariowiki.it / langlink di {it_title!r}")
        else:
            raise ParseError(
                f"mariowiki.it: il pilota {it_title!r} non ha un langlink verso mariowiki.com; "
                f"aggiungere itWikiPage in aliases.yaml"
            )
        result[slug] = it_name
    return result


def _it_course_ids(names: list[str], course_by_it: dict[str, str]) -> list[str | None]:
    return [course_by_it.get(norm(n)) for n in names]


def _resolve_it_events(it: ItNames, events: list[dict], course_by_it: dict[str, str]) -> dict[str, str]:
    """Id evento -> nome italiano, riconoscendo ogni Trofeo/rally dalla sequenza delle sue tappe."""
    by_stops = {tuple(e["stops"]): e["id"] for e in events}
    result: dict[str, str] = {}
    for ev in it.events:
        stops = _it_course_ids(ev.stops, course_by_it)
        if None in stops:
            unknown = [n for n, s in zip(ev.stops, stops) if s is None]
            raise ParseError(f"mariowiki.it: {ev.name!r} ha tappe non riconosciute: {unknown}")
        event_id = by_stops.get(tuple(stops))
        if event_id is None:
            raise ParseError(f"mariowiki.it: nessun evento con le tappe di {ev.name!r}: {stops}")
        if event_id in result:
            raise ParseError(f"mariowiki.it: {event_id} riconosciuto due volte ({result[event_id]!r}, {ev.name!r})")
        result[event_id] = ev.name
    return result


def _resolve_it_biomes(it: ItNames, cfg: Config, course_by_it: dict[str, str]) -> dict[str, str]:
    """Id regione -> nome italiano, dai corsi citati nella sezione del bioma.

    I nomi non riconosciuti (luoghi che non sono corsi, es. "Deserto Categnaccio") si ignorano; quelli
    riconosciuti devono indicare tutti la stessa regione. Se resta un solo bioma senza corsi riconosciuti
    e una sola regione libera, l'abbinamento è per esclusione (deduzione, non posizione).
    """
    result: dict[str, str] = {}
    pending: list[str] = []
    for biome in it.biomes:
        regions = {cfg.region_of.get(cid) for cid in _it_course_ids(biome.courses, course_by_it) if cid}
        regions.discard(None)
        if len(regions) > 1:
            raise ParseError(f"mariowiki.it: il bioma {biome.name!r} cita corsi di regioni diverse: {sorted(regions)}")
        if regions:
            rid = regions.pop()
            if rid in result:
                raise ParseError(f"mariowiki.it: regione {rid} riconosciuta due volte ({result[rid]!r}, {biome.name!r})")
            result[rid] = biome.name
        else:
            pending.append(biome.name)
    free = [e.id for e in cfg.regions.entities if e.id not in result]
    if len(pending) == 1 and len(free) == 1:
        result[free[0]] = pending[0]
    return result


def build(
    raw: RawData,
    cfg: Config,
    seed_version: int = 1,
    translations: Translations | None = None,
    images: Images | None = None,
    it_names: ItNames | None = None,
) -> dict[str, dict]:
    translations = translations or Translations()
    images = images or Images()
    it_names = it_names or ItNames()
    image_of_character, image_of_outfit, image_of_event = _resolve_images(images, cfg)
    driver_it = _resolve_it_drivers(it_names, cfg)
    # Nomi italiani dei corsi (da mariowiki.com, seedgen/i18n.py): la chiave con cui si riconoscono
    # Trofei, rally e biomi sulle pagine di mariowiki.it.
    course_by_it = {norm(name): cid for cid, name in translations.course_it.items()}
    starters = {cfg.resolve_driver(t) for t in images.starters}
    pages = cfg.sources["pages"]
    dash_url = _page_url(cfg, pages["dash_food"])
    navbox_url = _page_url(cfg, pages["navbox"])

    warnings: list[str] = []
    characters = []
    for order, e in enumerate(cfg.all_drivers()):
        # Per i 24 con outfit vale il nome di mariowiki.com (seedgen/i18n.py); mariowiki.it solo per
        # gli altri. Se le due fonti divergono lo si segnala, senza scegliere a caso.
        name_it = translations.character_it.get(e.id) or driver_it.get(e.id)
        if translations.character_it.get(e.id) and driver_it.get(e.id) and \
                translations.character_it[e.id] != driver_it[e.id]:
            warnings.append(f"{e.id}: nome italiano {translations.character_it[e.id]!r} (mariowiki.com) "
                            f"diverso da {driver_it[e.id]!r} (mariowiki.it)")
        characters.append({
            "id": e.id, "name": e.name, "nameIt": name_it, "rosterOrder": order,
            "imageUrl": image_of_character.get(e.id),
            # Galleria "Default drivers" = disponibile dall'inizio; None se la pagina non è stata letta.
            "starter": (e.id in starters) if images.starters else None,
        })
    courses = [
        {"id": e.id, "name": e.name, "nameIt": translations.course_it.get(e.id), "regionId": cfg.region_of.get(e.id)}
        for e in cfg.courses.entities
    ]
    region_it = _resolve_it_biomes(it_names, cfg, course_by_it)
    regions = [
        {"id": e.id, "name": e.name, "nameIt": region_it.get(e.id), "order": e.order} for e in cfg.regions.entities
    ]
    areas = [{"id": e.id, "name": e.name, "regionId": cfg.region_of[e.id]} for e in cfg.areas.entities]

    outfits: dict[str, dict] = {}
    for e in cfg.characters.entities:
        outfits[f"{e.id}__default"] = {
            "id": f"{e.id}__default", "characterId": e.id, "name": None, "nameIt": None, "isDefault": True,
            # L'outfit di default non ha un'immagine propria sul wiki: è quella del personaggio.
            "imageUrl": image_of_character.get(e.id),
        }

    food_groups, rules, dash_pairs = [], set(), set()
    for group in raw.food_groups:
        label = " / ".join(group.names)
        ids = {cfg.food_groups.resolve(n, "Dash Food") for n in group.names}
        if len(ids) != 1:
            raise ParseError(f"Dash Food: la cella Outfits di {label!r} mescola gruppi diversi: {sorted(ids)}")
        gid = ids.pop()
        food_groups.append({
            "id": gid,
            "name": label,
            # Traduzione NON ufficiale (manual/food_names_it.yaml): nessuna fonte ha i nomi italiani.
            "nameIt": " / ".join(cfg.food_names_it.get(n, n) for n in group.names),
            "foods": group.names,
            "revertsToDefault": group.reverts_to_default,
        })
        for o in group.outfits:
            cid = cfg.characters.resolve(o.character, f"Dash Food / {label}")
            oid = f"{cid}__{slugify(o.outfit)}"
            outfits.setdefault(oid, {
                "id": oid,
                "characterId": cid,
                "name": o.outfit,
                "nameIt": translations.outfit_name_it(o.outfit, cid, cfg.character_genders),
                "isDefault": False,
                "imageUrl": image_of_outfit.get(oid),
            })
            rules.add((oid, gid))
        for title in group.courses:
            if cfg.courses.is_ignored(title):
                continue
            dash_pairs.add((gid, cfg.courses.resolve(title, f"Dash Food / {label} / Locations")))

    # Presenza di un gruppo di cibo su un corso:
    #   ON_COURSE = stand sul percorso secondo "List of Yoshi's locations" (sezione Course locations)
    #   NEARBY    = solo nella colonna Locations di Dash Food, che include le strade vicine
    on_course: set[tuple[str, str]] = set()
    for stand in raw.course_stands:
        cid = cfg.courses.resolve(stand.course, "List of Yoshi's locations")
        for label in stand.foods:
            if not label:
                warnings.append(f"Yoshi's su {stand.course}: stand senza cibo indicato sul wiki")
                continue
            for gid in cfg.yoshi_label_groups(label, f"Yoshi's / {stand.course}"):
                on_course.add((gid, cid))
    group_courses = [
        {"foodGroupId": g, "courseId": c,
         "presence": "ON_COURSE" if (g, c) in on_course else "NEARBY",
         "listedInDashFood": (g, c) in dash_pairs}
        for g, c in sorted(on_course | dash_pairs)
    ]

    events = []
    for order, cup in enumerate(raw.cups):
        stops = [cfg.courses.resolve(t, cup.name) for t in cup.courses]
        events.append({"id": "cup_" + slugify(cup.name.removesuffix(" Cup")), "type": "CUP",
                       "name": cup.name, "order": order, "stops": stops, "imageUrl": image_of_event.get(cup.name)})
    for i, rally in enumerate(raw.rallies):
        stops = [cfg.courses.resolve(t, rally.name) for t in rally.courses]
        events.append({"id": "rally_" + slugify(rally.name.removesuffix(" Rally")), "type": "RALLY",
                       "name": rally.name, "order": len(raw.cups) + i, "stops": stops,
                       "imageUrl": image_of_event.get(rally.name)})

    rally_urls = [_page_url(cfg, t) for t in pages["rallies"]]

    event_it = _resolve_it_events(it_names, events, course_by_it)
    for e in events:
        e["nameIt"] = event_it.get(e["id"])

    if not it_names.is_empty():
        missing_it = sorted(
            [c["id"] for c in characters if not c["nameIt"]]
            + [r["id"] for r in regions if not r["nameIt"]]
            + [e["id"] for e in events if not e["nameIt"]]
        )
        if missing_it:
            raise ParseError(f"mariowiki.it: nome italiano non trovato per {missing_it}")

    if not images.is_empty():
        # Estrazione con immagini: ognuna deve risolversi in un elemento noto e ogni elemento deve
        # averne una. Un'immagine in più o in meno vuol dire che la pagina è cambiata: meglio
        # fermarsi che mostrare un'immagine sbagliata.
        unknown = sorted(set(image_of_outfit) - set(outfits))
        if unknown:
            raise ParseError(f"Mario Kart World: immagini di outfit sconosciuti: {unknown}")
        missing = sorted(
            [o["id"] for o in outfits.values() if not o["imageUrl"]]
            + [c["id"] for c in characters if not c["imageUrl"]]
            + [e["id"] for e in events if not e["imageUrl"]]
        )
        if not starters:
            missing.append("starter (galleria Default drivers vuota)")
        if missing:
            raise ParseError(f"Mario Kart World: immagine non trovata per {missing}")

    p_switches = []
    for i, m in enumerate(raw.missions, start=1):
        rid = cfg.regions.resolve(m.region, "List of Mario Kart World missions")
        if cfg.courses.knows(m.location):
            cid, aid = cfg.courses.resolve(m.location), None
        else:
            cid, aid = None, cfg.areas.resolve(m.location, f"missione {m.name!r}")
        p_switches.append({"id": f"pswitch_{i:03d}", "index": i, "regionId": rid,
                           "courseId": cid, "areaId": aid, "name": m.name})

    med = cfg.medallions
    medallions = [
        {"id": f"medallion_{rid}_{n:02d}", "regionId": rid, "index": n}
        for rid, count in med["counts"].items()
        for n in range(1, int(count) + 1)
    ]

    seed = {
        "characters.json": {"source": dash_url, "items": characters},
        "outfits.json": {"source": dash_url, "items": sorted(outfits.values(), key=lambda o: o["id"])},
        "food_groups.json": {"source": dash_url, "items": food_groups},
        "outfit_food_rules.json": {
            "source": dash_url,
            "items": [{"outfitId": o, "foodGroupId": g} for o, g in sorted(rules)],
        },
        "food_group_courses.json": {
            "source": [dash_url, _page_url(cfg, pages["yoshis"])],
            "items": group_courses,
        },
        "courses.json": {"source": navbox_url, "items": courses},
        "regions.json": {"source": _page_url(cfg, pages["missions"]), "items": regions},
        "areas.json": {"source": _page_url(cfg, pages["missions"]), "items": areas},
        "peach_medallions.json": {"source": med["source"]["url"], "manual": True, "items": medallions},
        "events.json": {"source": [navbox_url, *rally_urls], "items": events},
        "meta.json": {
            "seedVersion": seed_version,
            "gameVersion": cfg.expected.get("game_version"),
            "extractedOn": date.today().isoformat(),
            "origin": raw.origin,
            "note": raw.note,
            "warnings": warnings,
            "license": LICENSE,
            "sources": [
                {"title": s.title, "url": s.url or _page_url(cfg, s.title), "revid": s.revid} for s in raw.sources
            ],
        },
    }
    if p_switches:
        seed["p_switches.json"] = {"source": _page_url(cfg, pages["missions"]), "items": p_switches}
    return seed

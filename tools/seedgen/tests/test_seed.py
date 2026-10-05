"""Test sul seed: il riferimento trascritto passa la validazione, seed/ è coerente con esso,
e la validazione intercetta davvero gli errori."""

import copy

import pytest

from conftest import GOLDEN, SEED_DIR
from seedgen.build import build
from seedgen.cli import main
from seedgen.errors import EXIT_CHANGED, EXIT_OK, ParseError, ValidationError
from seedgen.output import COMPARED_FILES, diff, read_seed, write_seed
from seedgen.raw import RawData
from seedgen.validate import validate


@pytest.fixture(scope="module")
def golden_seed(cfg):
    return build(RawData.from_yaml(GOLDEN), cfg)


def test_golden_is_valid(golden_seed, cfg):
    validate(golden_seed, cfg)


def _without_name_it(seed: dict) -> dict:
    """I nomi in altre lingue (seedgen/i18n.py, seedgen/it_wiki.py, cibi manuali), gli URL delle
    immagini e lo stato iniziale di sblocco (seedgen/images.py) non sono nella trascrizione golden,
    come i pulsanti P: azzerarli prima del confronto, non escludere l'intero file (il resto va
    comunque verificato)."""
    seed = copy.deepcopy(seed)
    for filename in ("characters.json", "outfits.json", "courses.json", "events.json", "regions.json",
                     "food_groups.json"):
        for item in seed.get(filename, {}).get("items", []):
            item.pop("nameIt", None)
            item.pop("imageUrl", None)
            item.pop("starter", None)
            item.pop("unlockCriteria", None)
            item.pop("unlockCriteriaIt", None)
    seed.pop("mirror_mode.json", None)  # anche questo solo dalla pagina live (seedgen/images.py)
    # Stand e varianti dei cibi non sono nella trascrizione (fase C1): si verificano sulle fixture reali.
    for filename in ("yoshi_stands.json", "food_variants.json", "food_group_courses.json"):
        seed.pop(filename, None)
    # Mappa e collezionabili solo da mkworld-checklist (seedgen/checklist.py), mai nella trascrizione.
    for filename in ("peach_medallions.json", "question_panels.json", "map.json"):
        seed.pop(filename, None)
    return seed


def test_versioned_seed_matches_golden(golden_seed):
    # I pulsanti P non sono nella trascrizione golden (arrivano solo con l'estrazione via API,
    # vedi SPEC §5.3 "Stato"): si escludono dal confronto, si verificano solo con validate().
    versioned = {k: v for k, v in _without_name_it(read_seed(SEED_DIR)).items() if k != "p_switches.json"}
    golden = {k: v for k, v in _without_name_it(golden_seed).items() if k != "p_switches.json"}
    assert diff(versioned, golden) == []


def test_versioned_seed_is_valid(cfg):
    validate(read_seed(SEED_DIR), cfg)


def test_known_outfits(golden_seed):
    ids = {o["id"] for o in golden_seed["outfits.json"]["items"]}
    # campioni verificabili a occhio sulla pagina Dash Food
    assert {"mario__touring", "peach__yukata", "waluigi__mariachi", "toad__engineer", "donkey_kong__all_terrain"} <= ids
    rules = {(r["outfitId"], r["foodGroupId"]) for r in golden_seed["outfit_food_rules.json"]["items"]}
    assert {("mario__touring", "hamburger"), ("mario__touring", "barbecue"), ("mario__touring", "moo_moo_milk")} <= rules


def _broken(seed, mutate):
    s = copy.deepcopy(seed)
    mutate(s)
    return s


def test_validation_catches_missing_outfit(golden_seed, cfg):
    s = _broken(golden_seed, lambda s: s["outfits.json"]["items"].pop())
    with pytest.raises(ValidationError, match="outfit"):
        validate(s, cfg)


def test_validation_catches_broken_equivalence(golden_seed, cfg):
    def mutate(s):
        for r in s["outfit_food_rules.json"]["items"]:
            if r["outfitId"] == "baby_daisy__touring" and r["foodGroupId"] == "hamburger":
                r["outfitId"] = "baby_daisy__pro_racer"
    with pytest.raises(ValidationError, match="equivalenza"):
        validate(_broken(golden_seed, mutate), cfg)


def test_stands_and_variants_in_versioned_seed(cfg):
    seed = read_seed(SEED_DIR)
    stands = seed["yoshi_stands.json"]["items"]
    assert len(stands) == cfg.expected["yoshi_course_stands"] + cfg.expected["yoshi_route_stands"]
    assert len(seed["food_variants.json"]["items"]) == cfg.expected["food_variants"]
    assert "food_group_courses.json" not in seed     # sostituito dagli stand (fase C1)


def _with_stands(golden_seed, stands):
    s = copy.deepcopy(golden_seed)
    s["yoshi_stands.json"] = {"source": "x", "items": stands}
    return s


def _stand(**kw):
    base = {"id": "stand_x", "courseId": None, "regionId": None, "establishment": None, "establishmentIt": None,
            "location": "Somewhere", "locationIt": "Da qualche parte", "foods": [{"foodGroupId": "sushi", "food": None}]}
    return {**base, **kw}


def test_validation_catches_route_stand_with_region(golden_seed, cfg):
    s = _with_stands(golden_seed, [_stand(regionId="mesa")])
    with pytest.raises(ValidationError, match="strada con bioma"):
        validate(s, cfg)


def test_validation_catches_untranslated_location(golden_seed, cfg):
    s = _with_stands(golden_seed, [_stand(locationIt=None)])
    with pytest.raises(ValidationError, match="senza traduzione"):
        validate(s, cfg)


def test_validation_catches_stand_count(golden_seed, cfg):
    s = _with_stands(golden_seed, [_stand()])
    with pytest.raises(ValidationError, match="stand Yoshi's sulle strade: 1"):
        validate(s, cfg)


def test_versioned_map_files(cfg):
    seed = read_seed(SEED_DIR)
    assert len(seed["peach_medallions.json"]["items"]) == cfg.expected["peach_medallions"]
    assert len(seed["question_panels.json"]["items"]) == cfg.expected["question_panels"]
    assert all("x" in p and "y" in p for p in seed["p_switches.json"]["items"])


def test_partial_map_files_are_caught(cfg):
    seed = read_seed(SEED_DIR)
    del seed["question_panels.json"]
    with pytest.raises(ValidationError, match="presenti solo in parte"):
        validate(seed, cfg)


def test_point_out_of_bounds_is_caught(cfg):
    seed = read_seed(SEED_DIR)
    seed["peach_medallions.json"]["items"][0]["x"] = 120
    with pytest.raises(ValidationError, match="fuori dai limiti"):
        validate(seed, cfg)


def test_p_switch_region_mismatch_is_caught(golden_seed, cfg):
    def mutate(s):
        s["p_switches.json"] = {"source": "x", "items": [
            {"id": f"pswitch_{i:03d}", "index": i, "regionId": "snow", "courseId": "mario_bros_circuit",
             "areaId": None, "name": "x"} for i in range(1, 395)]}
    with pytest.raises(ValidationError, match="elencato in snow"):
        validate(_broken(golden_seed, mutate), cfg)


def test_validation_catches_bad_reference(golden_seed, cfg):
    s = _broken(golden_seed, lambda s: s["events.json"]["items"][0]["stops"].__setitem__(0, "coconut_mall"))
    with pytest.raises(ValidationError, match="coconut_mall"):
        validate(s, cfg)


def test_check_exit_codes(golden_seed, tmp_path):
    base, same, changed = tmp_path / "seed", tmp_path / "same", tmp_path / "changed"
    for d in (base, same):
        write_seed(golden_seed, d)
    write_seed(_broken(golden_seed, lambda s: s["events.json"]["items"][0]["stops"].reverse()), changed)
    assert main(["check", "--seed", str(base), "--candidate", str(same)]) == EXIT_OK
    assert main(["check", "--seed", str(base), "--candidate", str(changed)]) == EXIT_CHANGED


def test_accept_bumps_version(golden_seed, tmp_path):
    base, cand = tmp_path / "seed", tmp_path / "cand"
    write_seed(golden_seed, base)
    new = copy.deepcopy(golden_seed)
    new["events.json"]["items"][0]["name"] = "Mushroom Cup (renamed)"
    write_seed(new, cand)
    assert main(["accept", "--seed", str(base), "--candidate", str(cand)]) == EXIT_OK
    after = read_seed(base)
    assert after["meta.json"]["seedVersion"] == golden_seed["meta.json"]["seedVersion"] + 1
    assert all(after.get(f) == new.get(f) for f in COMPARED_FILES)


# --- Stand nel build (seedgen/build.py, _stands) ---------------------------------------------------

def _golden_with_stands(stands):
    from seedgen.raw import RawStand
    raw = RawData.from_yaml(GOLDEN)
    raw.stands = [RawStand(**s) for s in stands]
    return raw


def _places(cfg, locations):
    import dataclasses
    return dataclasses.replace(cfg, stand_places_it={
        "establishments": [{"en": "Snack bar", "it": "Snack bar"}],
        "locations": [{"en": en, "it": it} for en, it in locations.items()],
    })


def test_build_stands_ids_regions_and_translations(cfg):
    raw = _golden_with_stands([
        {"course": "Great ? Block Ruins", "foods": ["Sushi"], "establishment": "Snack bar", "location": "Top"},
        {"course": "Great ? Block Ruins", "foods": ["Kebabs"], "establishment": "Snack bar", "location": None},
        {"course": None, "foods": ["Meat"], "location": "Bridge"},
    ])
    stands = build(raw, _places(cfg, {"Top": "In cima", "Bridge": "Ponte"}))["yoshi_stands.json"]["items"]
    assert [s["id"] for s in stands] == ["stand_great_q_block_ruins_01", "stand_great_q_block_ruins_02", "stand_route_01"]
    assert [s["regionId"] for s in stands] == ["savanna_jungle", "savanna_jungle", None]
    assert [s["locationIt"] for s in stands] == ["In cima", None, "Ponte"]
    assert stands[2]["foods"] == [{"foodGroupId": "wild_bone", "food": None}]


def test_build_stops_on_missing_or_stale_translation(cfg):
    raw = _golden_with_stands([{"course": None, "foods": ["Meat"], "location": "Bridge"}])
    with pytest.raises(ParseError, match="senza traduzione"):
        build(raw, _places(cfg, {}))
    with pytest.raises(ParseError, match="non corrispondono più"):
        build(raw, _places(cfg, {"Bridge": "Ponte", "Old place": "Vecchio"}))


def test_build_cross_checks_course_stands_with_dash_food(cfg):
    # Mario Circuit non è nella colonna Locations del sushi su Dash Food.
    raw = _golden_with_stands([{"course": "Mario Circuit", "foods": ["Sushi"], "establishment": "Snack bar",
                                "location": "Top"}])
    with pytest.raises(ParseError, match="Dash Food non li elenca"):
        build(raw, _places(cfg, {"Top": "In cima"}))

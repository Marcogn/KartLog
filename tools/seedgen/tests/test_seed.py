"""Test sul seed: il riferimento trascritto passa la validazione, seed/ è coerente con esso,
e la validazione intercetta davvero gli errori."""

import copy

import pytest

from conftest import GOLDEN, SEED_DIR
from seedgen.build import build
from seedgen.cli import main
from seedgen.errors import EXIT_CHANGED, EXIT_OK, ValidationError
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


def test_food_presence(golden_seed):
    gc = {(i["foodGroupId"], i["courseId"]): i["presence"] for i in golden_seed["food_group_courses.json"]["items"]}
    assert gc[("sushi", "great_q_block_ruins")] == "ON_COURSE"
    assert gc[("sushi", "peach_beach")] == "NEARBY"
    assert gc[("spicy_curry", "shy_guy_bazaar")] == "ON_COURSE"
    assert gc[("spicy_curry", "mario_bros_circuit")] == "NEARBY"   # stand alla stazione a nord, non sul tracciato
    assert ("sushi", "mario_circuit") not in gc


def test_validation_catches_on_course_not_in_dash_food(golden_seed, cfg):
    def mutate(s):
        s["food_group_courses.json"]["items"].append(
            {"foodGroupId": "sushi", "courseId": "mario_circuit", "presence": "ON_COURSE", "listedInDashFood": False})
    with pytest.raises(ValidationError, match="sushi su mario_circuit"):
        validate(_broken(golden_seed, mutate), cfg)


def test_medallions_sum_to_200(golden_seed):
    items = golden_seed["peach_medallions.json"]["items"]
    assert len(items) == 200 and len({i["regionId"] for i in items}) == 10


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

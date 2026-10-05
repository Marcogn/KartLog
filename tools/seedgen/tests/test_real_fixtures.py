"""Controllo incrociato del parser sulle pagine REALI.

Si attiva solo dopo `python -m seedgen fetch-fixtures` (salva in tests/fixtures/real/).
Confronta quello che il parser estrae con la trascrizione manuale in tests/golden/.

- Se differiscono solo per aggiornamenti del wiki successivi alla trascrizione, va rivisto il diff
  e poi accettato (seedgen accept); il file golden resta come storico.
- Se differiscono perché il parser legge male la pagina, va corretto il parser.

I nomi in altre lingue (seedgen/i18n.py) non c'entrano qui: le loro pagine sorgente sono intere
biografie/articoli (fino a 2-3 MB l'una) e non hanno senso come fixture committate solo per una
piccola tabella — vedi tests/test_i18n_live.py, sempre dal vivo.
"""

import pytest

from conftest import GOLDEN, REAL
from seedgen import wiki
from seedgen.build import build
from seedgen.output import diff
from seedgen.parse import parse_all
from seedgen.raw import RawData
from seedgen.validate import validate

pytestmark = pytest.mark.skipif(
    not REAL.is_dir() or not any(REAL.glob("*.json")),
    reason="Nessuna fixture reale: eseguire `python -m seedgen fetch-fixtures` (richiede rete).",
)


@pytest.fixture(scope="module")
def real_seed(cfg):
    pages = wiki.load_fixtures(cfg, REAL)
    return build(parse_all(pages, cfg), cfg)


def test_real_pages_parse_and_validate(real_seed, cfg):
    validate(real_seed, cfg)


def test_real_pages_match_golden_transcription(real_seed, cfg):
    golden = build(RawData.from_yaml(GOLDEN), cfg)
    # I pulsanti P non sono nella trascrizione: si verificano solo con validate() (394, regioni coerenti).
    # Nemmeno stand e varianti dei cibi (fase C1): si verificano nei test qui sotto.
    comparable = {k: v for k, v in real_seed.items()
                  if k not in ("p_switches.json", "yoshi_stands.json", "food_variants.json")}
    changes = diff(golden, comparable)
    assert changes == [], "Parser e trascrizione divergono:\n" + "\n".join(changes)


def _stands(real_seed):
    return real_seed["yoshi_stands.json"]["items"]


def test_real_stands(real_seed, cfg):
    stands = _stands(real_seed)
    assert sum(1 for s in stands if s["courseId"]) == cfg.expected["yoshi_course_stands"]
    assert sum(1 for s in stands if not s["courseId"]) == cfg.expected["yoshi_route_stands"]
    # Le strade non hanno bioma (la pagina non lo dice), i percorsi quello del corso.
    assert all(s["regionId"] is None for s in stands if not s["courseId"])
    assert {s["regionId"] for s in stands if s["courseId"] == "crown_city"} == {"southern_sea"}
    # Crown City: dieci stand, i primi due senza luogo indicato (mai inventato).
    crown = [s for s in stands if s["courseId"] == "crown_city"]
    assert len(crown) == 9
    assert crown[0]["location"] is None and crown[0]["locationIt"] is None
    assert crown[3]["location"] == "In an alley next to Bank Coin Coffer"


def test_real_stand_foods(real_seed):
    by_id = {s["id"]: s for s in _stands(real_seed)}
    foods = lambda sid: [(f["foodGroupId"], f["food"]) for f in by_id[sid]["foods"]]
    # Il gruppo sushi: ogni stand ha il suo cibo preciso.
    assert foods("stand_cheep_cheep_falls_01") == [("sushi", "Takoyaki")]
    assert foods("stand_cheep_cheep_falls_04") == [("sushi", "Cheep Cheep taiyaki")]
    assert foods("stand_great_q_block_ruins_01") == [("sushi", "Sushi")]
    # Le taglie no: "Burgers" vale per tutto il gruppo.
    assert foods("stand_mario_bros_circuit_01") == [("hamburger", None)]
    # Uno stand di snack dà tutti e tre i gruppi.
    assert [g for g, _ in foods("stand_crown_city_02")] == ["snacks_1", "snacks_2", "snacks_3"]
    # Carne con l'osso e popcorn: solo sulle strade.
    for group in ("wild_bone", "popcorn"):
        assert {s["courseId"] for s in _stands(real_seed) if any(f["foodGroupId"] == group for f in s["foods"])} == {None}


def test_real_variants(real_seed, cfg):
    variants = real_seed["food_variants.json"]["items"]
    assert len(variants) == cfg.expected["food_variants"]
    sushi = [(v["name"], v["boost"]) for v in variants if v["foodGroupId"] == "sushi"]
    assert sushi == [("Takoyaki", ["MEDIUM"]), ("Candy apple", ["MEDIUM"]), ("Cheep Cheep taiyaki", ["MEDIUM"]),
                     ("Sushi", ["SMALL"]), ("Sushi", ["MEDIUM"]), ("Sushi", ["LARGE"]),
                     ("Sushi", ["SMALL", "MEDIUM", "LARGE"])]
    assert all("/thumb/" not in v["imageUrl"] for v in variants)

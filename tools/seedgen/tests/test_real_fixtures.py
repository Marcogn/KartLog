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
    comparable = {k: v for k, v in real_seed.items() if k != "p_switches.json"}
    changes = diff(golden, comparable)
    assert changes == [], "Parser e trascrizione divergono:\n" + "\n".join(changes)

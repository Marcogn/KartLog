"""Controllo dei nomi in altre lingue (seedgen/i18n.py) sulle pagine REALI, sempre dal vivo.

A differenza di test_real_fixtures.py, qui non ci sono fixture committate: le pagine di
personaggi/corsi sono intere biografie (fino a 2-3 MB l'una), non ha senso salvarle solo per una
piccola tabella. Si salta il modulo se il wiki non è raggiungibile (stesso spirito dello skip di
test_real_fixtures.py quando mancano le fixture).
"""

import pytest
import requests

from seedgen.i18n import OUTFIT_SECTIONS
from seedgen.parse import parse_translations
from seedgen.wiki import fetch_i18n

try:
    _REACHABLE = requests.head("https://www.mariowiki.com", timeout=5).status_code < 500
except requests.RequestException:
    _REACHABLE = False

pytestmark = pytest.mark.skipif(not _REACHABLE, reason="mariowiki.com non raggiungibile (rete assente/bloccata).")


@pytest.fixture(scope="module")
def translations(cfg):
    return parse_translations(fetch_i18n(cfg), cfg)


def test_known_character_names_it(translations):
    """Campione verificato a mano (25/09/2026): se questi valori cambiano, rivedere seedgen/i18n.py."""
    assert translations.character_it["mario"] == "Mario"
    assert translations.character_it["toad"] == "Toad"           # non la variante storica "Ughetto"
    assert translations.character_it["king_boo"] == "Re Boo"
    assert translations.character_it["rosalina"] == "Rosalinda"
    assert translations.character_it["peach"] == "Peach"         # pagina raggiunta via redirect


def test_known_course_names_it(translations):
    assert translations.course_it["mario_circuit"] == "Circuito di Mario"
    assert translations.course_it["rainbow_road"] == "Pista Arcobaleno"
    assert translations.course_it["moo_moo_meadows"] == "Prateria Verde"
    assert translations.course_it["wario_stadium"] == "Stadio di Wario"  # pagina "(Mario Kart 64)"


def test_known_outfit_names_it(translations, cfg):
    assert translations.outfit_name_it("Explorer", "toad", cfg.character_genders) == "Esploratore"
    assert translations.outfit_name_it("Explorer", "toadette", cfg.character_genders) == "Esploratrice"
    assert translations.outfit_name_it("Aero", "mario", cfg.character_genders) == "Motociclista"


def test_every_known_outfit_name_gets_a_translation(translations):
    all_names = {name for names in OUTFIT_SECTIONS.values() for name in names}
    missing = [name for name in all_names if translations.outfit_it.get(name) is None]
    assert missing == [], f"outfit senza traduzione italiana: {missing}"

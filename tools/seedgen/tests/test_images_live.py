"""Controllo di seedgen/images.py sulla pagina REALE "Mario Kart World", sempre dal vivo.

Stesso schema di test_i18n_live.py: niente fixture committata (la pagina supera 1 MB), modulo saltato
se il wiki non è raggiungibile.
"""

import pytest
import requests

from seedgen.images import extract_images
from seedgen.wiki import fetch_images_page

try:
    _REACHABLE = requests.head("https://www.mariowiki.com", timeout=5).status_code < 500
except requests.RequestException:
    _REACHABLE = False

pytestmark = pytest.mark.skipif(not _REACHABLE, reason="mariowiki.com non raggiungibile (rete assente/bloccata).")


@pytest.fixture(scope="module")
def images(cfg):
    return extract_images(fetch_images_page(cfg).html)


def test_every_character_has_an_image(images, cfg):
    known = {cfg.characters.resolve(t) for t in images.characters if cfg.characters.knows(t)}
    assert known == {e.id for e in cfg.characters.entities}


def test_outfit_and_event_counts(images, cfg):
    assert len(images.outfits) == cfg.expected["alternative_outfits"]
    assert len(images.events) == cfg.expected["cups"] + cfg.expected["rallies"]


def test_known_samples(images):
    """Campione verificato a mano (26/09/2026) sulla pagina."""
    assert images.characters["Mario"].endswith("/MarioMKworld.png")
    assert images.outfits[("Mario", "Touring")].endswith("/Mariotourmkworld.png")
    assert images.events["Mushroom Cup"].endswith("/Mario_Kart_World_Mushroom_Cup_Icon.png")

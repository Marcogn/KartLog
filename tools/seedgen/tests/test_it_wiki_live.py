"""Controllo di seedgen/it_wiki.py sulle pagine REALI di mariowiki.it, sempre dal vivo.

Stesso schema di test_i18n_live.py: niente fixture committate, modulo saltato senza rete.
"""

import pytest
import requests

from seedgen.build import _resolve_it_biomes, _resolve_it_drivers
from seedgen.it_wiki import norm
from seedgen.wiki import fetch_it_names

try:
    _REACHABLE = requests.head("https://www.mariowiki.it", timeout=5).status_code < 500
except requests.RequestException:
    _REACHABLE = False

pytestmark = pytest.mark.skipif(not _REACHABLE, reason="mariowiki.it non raggiungibile (rete assente/bloccata).")


@pytest.fixture(scope="module")
def it_names(cfg):
    return fetch_it_names(cfg)[0]


def test_every_driver_resolves(it_names, cfg):
    resolved = _resolve_it_drivers(it_names, cfg)
    assert set(resolved) == {e.id for e in cfg.all_drivers()}


def test_known_samples(it_names, cfg):
    """Campione verificato a mano (26/09/2026) sulla pagina "Mario Kart World" di mariowiki.it."""
    resolved = _resolve_it_drivers(it_names, cfg)
    assert resolved["cow"] == "Mucca"
    assert resolved["spike"] == "Spunzo"          # via itWikiPage, nessun langlink
    assert resolved["birdo"] == "Strutzi"         # coincide con mariowiki.com
    names = [e.name for e in it_names.events]
    assert names[0] == "Trofeo Fungo" and "Rally Trivella" in names


def test_counts(it_names, cfg):
    assert len(it_names.events) == cfg.expected["cups"] + cfg.expected["rallies"]
    assert len(it_names.biomes) == cfg.expected["regions"]
    # Solo per nome: i corsi italiani veri arrivano da seedgen/i18n.py, qui basta Circuito Mario Bros.
    result = _resolve_it_biomes(it_names, cfg, {norm("Circuito Mario Bros."): "mario_bros_circuit"})
    assert result["mesa"] == "Bioma della Mesa"

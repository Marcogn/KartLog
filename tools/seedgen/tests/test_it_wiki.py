"""Test di seedgen/it_wiki.py (mariowiki.it) su HTML sintetico, e della risoluzione in build.py.

Il confronto con le pagine reali sta in test_it_wiki_live.py (richiede la rete).
"""

import pytest

from conftest import GOLDEN
from seedgen.build import _resolve_it_biomes, _resolve_it_drivers, _resolve_it_events, build
from seedgen.errors import ParseError
from seedgen.i18n import Translations
from seedgen.it_wiki import ItBiome, ItEvent, ItNames, extract_biomes, extract_drivers, extract_events, norm
from seedgen.raw import RawData


def _h(level: int, anchor: str) -> str:
    return f'<h{level}><span class="mw-headline" id="{anchor}">{anchor}</span></h{level}>'


def _driver_cell(title: str, text: str) -> str:
    return (f'<td><span><a href="/File:x" class="mw-file-description"><img src="/x.png"/></a></span>'
            f'<br /><a href="/{title}" title="{title}">{text}</a></td>')


GAME = (
    _h(2, "Personaggi") + _h(3, "Di_base")
    + "<table><tr>" + _driver_cell("Mucca", "Mucca") + _driver_cell("Tipo Timido (personaggio)", "Tipo Timido")
    + "</tr></table>" + _h(3, "Sbloccabili")
    + "<table><tr>" + _driver_cell("Delfì", "Delfì") + "</tr></table>"
    + _h(4, "Criteri_di_sblocco")
    + '<table><tr><td><a href="/Delf" title="Delfì">Delfì</a></td>'
    + '<td><a href="/Trofeo_Fungo" title="Trofeo Fungo">Trofeo Fungo</a></td></tr></table>'
    + _h(2, "Percorsi") + _h(3, "Gran_Premio_2")
    + "<table><tr><th>Trofeo</th><th>Percorso 1</th></tr>"
    + '<tr><th>Trofeo Fungo</th><td><a title="Circuito Mario Bros.">Circuito Mario Bros.</a></td>'
    + '<td><a title="Deserto Picchiasol (Mario Kart DS)">Deserto Picchiasol</a><small>(Mario Kart DS)</small></td></tr>'
    + "</table>" + _h(3, "Modalità_sopravvivenza")
    + "<table><tr><th>Rally</th><th>Punto di partenza</th></tr>"
    + '<tr><td>Rally Trivella<sup>1</sup></td><td><a title="Trofea">Trofea</a></td></tr></table>'
)

MISSIONS = (
    _h(2, "Bioma della Mesa") + "<p>Questo bioma contiene Circuito Mario Bros., Monte Rotaia e Stadio di Wario.</p>"
    + _h(2, "Bioma della savana") + "<p>Questi bioma contiene Rovine del blocco\xa0?.</p>"
    + _h(2, "Voci correlate") + "<ul><li>x</li></ul>"
)


def test_drivers_only_from_gallery_tables():
    # La tabella "Criteri di sblocco" (senza icone) non aggiunge Trofeo Fungo come pilota.
    assert extract_drivers(GAME) == {"Mucca": "Mucca", "Tipo Timido (personaggio)": "Tipo Timido", "Delfì": "Delfì"}


def test_events_strip_notes_and_disambiguators():
    assert extract_events(GAME) == [
        ItEvent("Trofeo Fungo", ["Circuito Mario Bros.", "Deserto Picchiasol"]),
        ItEvent("Rally Trivella", ["Trofea"]),
    ]


def test_biomes_from_intro_including_typo():
    assert extract_biomes(MISSIONS) == [
        ItBiome("Bioma della Mesa", ["Circuito Mario Bros.", "Monte Rotaia", "Stadio di Wario"]),
        ItBiome("Bioma della savana", ["Rovine del blocco ?"]),  # spazio non separabile normalizzato
    ]
    assert norm("Rovine del blocco\xa0?") == norm("Rovine del blocco ?")
    assert norm("Circuito Mario Bros.") == norm("Circuito Mario Bros")


# --- risoluzione in build.py ------------------------------------------------------------------------

def test_driver_via_langlink_or_it_wiki_page(cfg):
    it = ItNames(drivers={"Mucca": "Mucca", "Spunzo (personaggio)": "Spunzo"}, langlinks={"Mucca": "Cow"})
    assert _resolve_it_drivers(it, cfg) == {"cow": "Mucca", "spike": "Spunzo"}


def test_driver_without_langlink_fails(cfg):
    with pytest.raises(ParseError, match="itWikiPage"):
        _resolve_it_drivers(ItNames(drivers={"Sconosciuto": "Sconosciuto"}), cfg)


def test_event_recognised_by_its_stops():
    events = [{"id": "cup_a", "stops": ["c1", "c2"]}, {"id": "cup_b", "stops": ["c2", "c1"]}]
    course_by_it = {norm("Uno"): "c1", norm("Due"): "c2"}
    it = ItNames(events=[ItEvent("Trofeo B", ["Due", "Uno"])])
    assert _resolve_it_events(it, events, course_by_it) == {"cup_b": "Trofeo B"}
    with pytest.raises(ParseError, match="non riconosciute"):
        _resolve_it_events(ItNames(events=[ItEvent("X", ["Tre"])]), events, course_by_it)


def test_biomes_by_courses_then_by_elimination(cfg):
    course_ids = {e.id for e in cfg.courses.entities}
    one_course = {}  # regione -> un suo corso qualsiasi
    for course, region in cfg.region_of.items():
        if course in course_ids:
            one_course.setdefault(region, course)
    course_by_it = {norm(f"it {course}"): course for course in one_course.values()}
    biomes = [
        ItBiome(f"Bioma {rid}", [f"it {course}", "Luogo che non è un corso"])
        for rid, course in one_course.items() if rid != "savanna_jungle"
    ]
    biomes.append(ItBiome("Bioma della savana", ["Niente di noto"]))
    result = _resolve_it_biomes(ItNames(biomes=biomes), cfg, course_by_it)
    assert result["mesa"] == "Bioma mesa"
    assert result["savanna_jungle"] == "Bioma della savana"  # l'unico rimasto, per esclusione
    assert len(result) == 10


def test_biome_with_mixed_regions_fails(cfg):
    course_by_it = {norm("A"): "mario_bros_circuit", norm("B"): "rainbow_road", norm("C"): "desert_hills"}
    with pytest.raises(ParseError, match="regioni diverse"):
        _resolve_it_biomes(ItNames(biomes=[ItBiome("X", ["A", "C"])]), cfg, course_by_it)


def test_food_names_are_the_manual_translation(cfg):
    seed = build(RawData.from_yaml(GOLDEN), cfg)
    groups = {g["id"]: g["nameIt"] for g in seed["food_groups.json"]["items"]}
    assert groups["donut"] == "Ciambella"
    assert groups["sushi"] == "Takoyaki / Mela caramellata / Taiyaki Pesce Smack / Sushi"


def test_build_requires_every_italian_name_once_it_wiki_is_read(cfg):
    it = ItNames(drivers={"Mucca": "Mucca"}, langlinks={"Mucca": "Cow"})
    with pytest.raises(ParseError, match="nome italiano non trovato"):
        build(RawData.from_yaml(GOLDEN), cfg, translations=Translations(), it_names=it)

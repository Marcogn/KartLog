"""Test del parser su fixture SINTETICHE che riproducono la struttura attesa delle pagine.

Non sostituiscono test_real_fixtures.py: confermano che la logica (rowspan, colspan, celle condivise,
link con disambiguatore, note a piè di pagina) funziona, non che la pagina reale sia fatta così.
"""

import pytest
from bs4 import BeautifulSoup

from conftest import synthetic_page
from seedgen.errors import ParseError, UnknownNameError
from seedgen.html_table import cell_lines, expand
import dataclasses

from seedgen.parse import navbox_rally_titles, parse_cups, parse_dash_food, parse_missions, parse_rally, parse_yoshis


def test_expand_rowspan_colspan():
    html = """<table><tr><th>A</th><th colspan=2>B</th></tr>
    <tr><td rowspan=2>x</td><td>y</td><td rowspan=2>z</td></tr>
    <tr><td>w</td></tr></table>"""
    grid = expand(BeautifulSoup(html, "html.parser").table)
    assert grid.header_labels() == ["A", "B", "B"]
    r1, r2 = grid.body()
    assert r1[0] is r2[0] and r1[2] is r2[2]
    assert [c.get_text() for c in r2] == ["x", "w", "z"]


def test_cell_lines_keeps_inline_links_and_drops_footnotes():
    cell = BeautifulSoup('<td><a title="Mario">Mario</a> (Touring)<br>Toad (X)<sup>[1]</sup></td>', "html.parser").td
    assert cell_lines(cell) == ["Mario (Touring)", "Toad (X)"]


def test_dash_food_groups():
    groups = parse_dash_food(synthetic_page("dash_food.html", "Dash Food"))
    by_name = {g.names[0]: g for g in groups}
    assert list(by_name) == ["Hamburger", "Takoyaki", "Snacks 1", "Snacks 2", "Lunchbox"]

    burger = by_name["Hamburger"]
    assert len(burger.outfits) == 6
    assert (burger.outfits[0].character, burger.outfits[0].outfit) == ("Mario", "Touring")
    assert burger.outfits[-1].outfit == "Touring"  # la nota [1] non sporca il nome
    assert burger.courses == ["Crown City", "Mario Circuit (Mario Kart World)"]

    # takoyaki e sushi condividono la cella Outfits: un solo gruppo con due nomi
    assert by_name["Takoyaki"].names == ["Takoyaki", "Sushi"]

    # gli snack hanno outfit separati ma la stessa cella Locations
    assert by_name["Snacks 1"].courses == by_name["Snacks 2"].courses == ["Choco Mountain"]
    assert by_name["Snacks 1"].outfits[0].outfit == "Mechanic"

    lunch = by_name["Lunchbox"]
    assert lunch.reverts_to_default and lunch.outfits == []


def test_dash_food_rejects_unexpected_outfit_line():
    html = (synthetic_page("dash_food.html", "Dash Food").html).replace("Luigi (Happi)", "Luigi Happi")
    from seedgen.wiki import WikiPage
    with pytest.raises(ParseError, match="Luigi Happi"):
        parse_dash_food(WikiPage("Dash Food", 0, html))


def test_navbox_cups_ignore_snes_links(cfg):
    page = synthetic_page("navbox.html", "Template:Mario Kart World")
    sub = dataclasses.replace(cfg, sources={**cfg.sources, "cups": ["Mushroom Cup", "Special Cup"]})
    cups = parse_cups(page, sub)
    assert cups[1].courses == ["Acorn Heights", "Mario Circuit (Mario Kart World)", "Peach Stadium",
                               "Rainbow Road (Mario Kart World)"]
    assert navbox_rally_titles(page) == ["Golden Rally", "Boomerang Rally (Mario Kart World)"]


def test_navbox_missing_cup_fails(cfg):
    with pytest.raises(ParseError, match="Flower Cup"):
        parse_cups(synthetic_page("navbox.html", "Template:Mario Kart World"), cfg)


def test_rally_horizontal_skips_non_course_links(cfg):
    event = parse_rally(synthetic_page("rally.html", "Golden Rally"), cfg)
    assert event.courses == ["Desert Hills", "Mario Bros. Circuit", "Choco Mountain", "Moo Moo Meadows",
                             "Mario Circuit (Mario Kart World)", "Acorn Heights"]


def test_rally_vertical(cfg):
    event = parse_rally(synthetic_page("rally_vertical.html", "Drill Rally"), cfg)
    assert event.courses == ["Wario Shipyard", "DK Pass", "Bowser's Castle (Mario Kart World)"]


def test_unknown_names_fail(cfg):
    with pytest.raises(UnknownNameError):
        cfg.characters.resolve("Luigi's Mansion")
    with pytest.raises(UnknownNameError):
        cfg.courses.resolve("Coconut Mall")


def test_yoshis_only_course_section_and_rowspans():
    stands = {s.course: s.foods for s in parse_yoshis(synthetic_page("yoshis.html", "List of Yoshi's locations"))}
    assert list(stands) == ["Crown City", "Wario Stadium"]          # la sezione Route locations è ignorata
    assert stands["Crown City"] == ["Pizza", "Chips", "soft drinks", "chocolate bars", "Kebabs", ""]
    assert stands["Wario Stadium"] == ["Curry"]


def test_yoshi_labels_map_to_groups(cfg):
    assert cfg.yoshi_label_groups("soft drinks") == ["snacks_1", "snacks_2", "snacks_3"]
    assert cfg.yoshi_label_groups("Candy apples") == ["sushi"]
    with pytest.raises(UnknownNameError):
        cfg.yoshi_label_groups("Ramen")


def test_missions(cfg):
    ms = parse_missions(synthetic_page("missions.html", "List of Mario Kart World missions"), cfg)
    assert [(m.region, m.location) for m in ms] == [
        ("Mesa biome", "Mario Bros. Circuit"),
        ("Mesa biome", "Wario Stadium (Mario Kart 64)"),
        ("Desert biome", "Chain Chomp Desert"),
    ]
    assert ms[0].name == "Ride the rail! Ride the line!"

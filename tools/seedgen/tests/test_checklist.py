"""seedgen/checklist.py con dati sintetici: nessuna rete."""

import pytest

from seedgen.checklist import MapPoint, match_missions, mission_key, parse, webp_size
from seedgen.errors import ParseError

TEXTS = {"STICKERS": {
    "SHARED": {"PEACH_COIN_TOOLTIP": "Collect a Peach Medallion!"},
    "7_INSTRUCTIONS": "  On top of the tower.  ",
    "9_MISSION_NAME": "Grab the coins!",
}}


def _item(index, kind, x=10.0, y=20.0, **model):
    return {"index": index, "instructions": f"{index}_INSTRUCTIONS",
            "collectibleModel": {"collectibleType": kind, "xPercentage": x, "yPercentage": y, **model}}


def test_webp_size_vp8x():
    header = b"RIFF" + b"\0" * 4 + b"WEBP" + b"VP8X" + b"\0" * 8 + (2579).to_bytes(3, "little") + (2321).to_bytes(3, "little")
    assert webp_size(header) == (2580, 2322)


def test_webp_size_rejects_other_formats():
    with pytest.raises(ParseError):
        webp_size(b"\x89PNG" + b"\0" * 40)


def test_parse_splits_by_type_and_skips_album_only_items():
    items = [
        _item(9, "p-switch", missionName="9_MISSION_NAME", youtubeId="abc"),
        _item(7, "peach-coin"),
        _item(8, "questionmark-panel", x=0, y=100),
        {"index": 0, "instructions": "SHARED.PEACH_COIN_TOOLTIP"},  # voce dell'album, niente mappa
    ]
    c = parse(items, TEXTS, commit="c0ffee", repo_url="https://example/r", map_url="https://m", map_size=(10, 20))
    assert c.medallions == [MapPoint(7, 10.0, 20.0, "On top of the tower.", None)]
    assert [p.source_index for p in c.panels] == [8]
    assert c.p_switches[0].mission_name == "Grab the coins!" and c.p_switches[0].youtube_id == "abc"
    assert c.source_url == "https://example/r/tree/c0ffee"


def test_parse_rejects_position_outside_map():
    with pytest.raises(ParseError, match="posizione"):
        parse([_item(1, "peach-coin", x=101)], TEXTS, commit="", repo_url="", map_url="", map_size=(1, 1))


def test_parse_rejects_unknown_type():
    with pytest.raises(ParseError, match="sconosciuto"):
        parse([_item(1, "star-coin")], TEXTS, commit="", repo_url="", map_url="", map_size=(1, 1))


def _p(name):
    return MapPoint(1, 0, 0, None, None, mission_name=name)


def test_mission_key_ignores_case_and_punctuation():
    assert mission_key("SEARCH FIELDS. ABDUCT BLUE COINS.") == mission_key("Search fields, abduct blue coins!")


def test_match_missions_uses_renames():
    points = match_missions([_p("Ride to the THEATRE!")], ["Ride to the theater!"],
                            {"Ride to the theatre!": "Ride to the theater!"})
    assert list(points) == ["Ride to the theater!"]


def test_match_missions_stops_on_unknown_name():
    with pytest.raises(ParseError, match="non abbinate"):
        match_missions([_p("Something else!")], ["Ride to the theater!"], {})


def test_match_missions_stops_on_duplicates():
    with pytest.raises(ParseError, match="due Pulsanti P"):
        match_missions([_p("Go!"), _p("GO!")], ["Go!"], {})

"""Test di seedgen/images.py: parser su HTML sintetico e controlli di build.

La pagina reale ("Mario Kart World", oltre 1 MB) non è una fixture committata: il confronto con la
pagina vera sta in test_images_live.py, che richiede la rete.
"""

import copy
from dataclasses import replace

import pytest

from conftest import GOLDEN
from seedgen.build import build
from seedgen.errors import ParseError, ValidationError
from seedgen.images import Images, extract_images, original_url
from seedgen.raw import RawData
from seedgen.validate import validate

CDN = "https://mario.wiki.gallery/images"


def _img(path: str, alt: str = "x") -> str:
    name = path.rsplit("/", 1)[1]
    return f'<img alt="{alt}" src="{CDN}/thumb/{path}/100px-{name}" width="100" height="129" />'


def _gallery(*items: tuple[str, str]) -> str:
    boxes = "".join(
        f'<li class="gallerybox"><div><div class="thumb"><a href="/File:x" class="image">{_img(path)}</a></div>'
        f'<div class="gallerytext">{caption}</div></div></li>'
        for path, caption in items
    )
    return f'<ul class="gallery mw-gallery-traditional">{boxes}</ul>'


def _heading(level: int, anchor: str) -> str:
    return f'<h{level}><span class="mw-headline" id="{anchor}">{anchor}</span></h{level}>'


HTML = (
    _heading(2, "Characters")
    + _heading(3, "Default_drivers")
    + _gallery(("5/51/MarioMKworld.png", '<a href="/Mario" title="Mario">Mario</a>'),
               ("a/a1/PeachMKworld.png", '<a href="/Princess_Peach" title="Princess Peach">Peach</a>'),
               ("0/0g/Goomba.png", '<b><a href="/Goomba" title="Goomba">Goomba</a></b>'))
    + _heading(3, "Unlockable_drivers")
    + _gallery(("c/cc/DaisyMKworld.png", '<a href="/Daisy" title="Daisy">Daisy</a>'))
    + _heading(3, "Character_outfits")
    + "<p>testo</p><dl><dt>Mario</dt></dl>"
    + _gallery(("6/67/Mariotourmkworld.png", "Touring"), ("f/fd/Pro.png", "Pro Racer"))
    + "<dl><dt>Peach</dt></dl>"
    + _gallery(("1/11/PeachTouring.png", "Touring"))
    + _heading(3, "Unlock_criteria")
    + _gallery(("9/99/NonUnOutfit.png", "Sbloccato"))
    + '<table><tr><th>Character</th><th>Criteria</th></tr>'
    + '<tr><td><a href="/Daisy" title="Daisy">Daisy</a></td><td>Clear the <a href="/F">Flower Cup</a></td></tr>'
    + '<tr><td><a href="/Spike" title="Spike">Spike</a></td><td rowspan="2">Be summoned by '
    + '<a href="/K">Kamek</a> (note)<sup>1</sup></td></tr>'
    + '<tr><td><a href="/Swoop" title="Swoop">Swoop</a></td></tr></table>'
    + _heading(2, "Courses")
    + "<p>Mirror Mode is unlocked by:</p><ul><li>Playing seven cups (with a note)</li>"
    + "<li>Activating 10 <a href='/P'>? Panels</a>, then more.</li></ul>"
    + '<table><tr><th>Cup</th></tr><tr><th class="subheader"><a href="/File:M" class="image">'
    + _img("7/78/Mario_Kart_World_Mushroom_Cup_Icon.png")
    + '</a><br /><a href="/Mushroom_Cup" title="Mushroom Cup">Mushroom Cup</a></th></tr>'
    + '<tr><th class="subheader"><a href="/File:B" class="image">'
    + _img("a/ab/Mario_Kart_World_Boomerang_Rally_Icon.png")
    + '</a><br /><a href="/Boomerang_Rally_(Mario_Kart_World)" title="Boomerang Rally (Mario Kart World)">'
    + "Boomerang Rally</a></th></tr></table>"
)


def test_original_url_strips_thumbnail():
    images = extract_images(HTML)
    assert images.characters["Mario"] == f"{CDN}/5/51/MarioMKworld.png"


def test_drivers_from_both_sections_keyed_by_page_title():
    images = extract_images(HTML)
    assert set(images.characters) == {"Mario", "Princess Peach", "Goomba", "Daisy"}
    # Solo la galleria "Default drivers" è disponibile dall'inizio.
    assert images.starters == {"Mario", "Princess Peach", "Goomba"}


def test_outfits_grouped_by_character_and_section_bounded():
    images = extract_images(HTML)
    assert images.outfits == {
        ("Mario", "Touring"): f"{CDN}/6/67/Mariotourmkworld.png",
        ("Mario", "Pro Racer"): f"{CDN}/f/fd/Pro.png",
        ("Peach", "Touring"): f"{CDN}/1/11/PeachTouring.png",
    }  # la galleria sotto "Unlock criteria" non è letta


def test_event_icons_keyed_by_link_text():
    images = extract_images(HTML)
    assert images.events == {
        "Mushroom Cup": f"{CDN}/7/78/Mario_Kart_World_Mushroom_Cup_Icon.png",
        "Boomerang Rally": f"{CDN}/a/ab/Mario_Kart_World_Boomerang_Rally_Icon.png",
    }


def test_unlock_criteria_follow_rowspan_without_notes():
    assert extract_images(HTML).unlock_criteria == {
        "Daisy": "Clear the Flower Cup", "Spike": "Be summoned by Kamek", "Swoop": "Be summoned by Kamek",
    }


def test_mirror_steps_without_parenthetical_notes():
    # "? Panels" è un nome: lo spazio prima del "?" resta, quello prima della virgola no.
    assert extract_images(HTML).mirror_steps == ["Playing seven cups", "Activating 10 ? Panels, then more."]


def test_missing_section_fails():
    with pytest.raises(ParseError, match="Character_outfits"):
        extract_images(HTML.replace('id="Character_outfits"', 'id="Outfits"'))


def test_unexpected_url_fails():
    from bs4 import BeautifulSoup

    img = BeautifulSoup('<img src="data:image/png;base64,xx" />', "html.parser").img
    with pytest.raises(ParseError):
        original_url(img)


# --- build -----------------------------------------------------------------------------------------

def _complete_images(cfg, raw: RawData) -> Images:
    """Un'immagine finta ma ben formata per ogni elemento del golden: serve solo a esercitare build()."""
    characters = {e.name: f"{CDN}/c/{e.id}.png" for e in cfg.all_drivers()}
    outfits = {}
    for group in raw.food_groups:
        for o in group.outfits:
            outfits[(o.character, o.outfit)] = f"{CDN}/o/{o.character}_{o.outfit}.png"
    events = {e.name: f"{CDN}/e/{e.name}.png" for e in [*raw.cups, *raw.rallies]}
    starters = frozenset(e.name for e in cfg.all_drivers()[:cfg.expected["starter_drivers"]])
    criteria = {e.name: f"Unlock {e.name}" for e in cfg.all_drivers()[cfg.expected["starter_drivers"]:]}
    return Images(characters=characters, outfits=outfits, events=events, starters=starters,
                  unlock_criteria=criteria, mirror_steps=[m["en"] for m in cfg.mirror_mode_it])


@pytest.fixture(scope="module")
def golden_raw():
    return RawData.from_yaml(GOLDEN)


def test_build_without_images_leaves_urls_null(golden_raw, cfg):
    seed = build(golden_raw, cfg)
    assert all(c["imageUrl"] is None for c in seed["characters.json"]["items"])


def test_build_with_images(golden_raw, cfg):
    seed = build(golden_raw, cfg, images=_complete_images(cfg, golden_raw))
    validate(seed, cfg)
    outfits = {o["id"]: o for o in seed["outfits.json"]["items"]}
    characters = {c["id"]: c for c in seed["characters.json"]["items"]}
    # L'outfit di default usa l'immagine del personaggio.
    assert outfits["mario__default"]["imageUrl"] == characters["mario"]["imageUrl"]
    assert outfits["mario__touring"]["imageUrl"] == f"{CDN}/o/Mario_Touring.png"


def test_build_fails_on_missing_image(golden_raw, cfg):
    images = _complete_images(cfg, golden_raw)
    events = dict(images.events)
    events.pop("Turnip Rally")
    with pytest.raises(ParseError, match="rally_turnip"):
        build(golden_raw, cfg, images=replace(images, events=events))


def test_build_fails_on_image_of_unknown_outfit(golden_raw, cfg):
    images = _complete_images(cfg, golden_raw)
    outfits = {**images.outfits, ("Mario", "Astronaut"): f"{CDN}/o/x.png"}
    with pytest.raises(ParseError, match="mario__astronaut"):
        build(golden_raw, cfg, images=replace(images, outfits=outfits))


def test_validation_rejects_foreign_image_host(golden_raw, cfg):
    seed = copy.deepcopy(build(golden_raw, cfg, images=_complete_images(cfg, golden_raw)))
    seed["events.json"]["items"][0]["imageUrl"] = "https://example.com/x.png"
    with pytest.raises(ValidationError, match="imageUrl"):
        validate(seed, cfg)


def test_build_puts_criteria_only_on_unlockable_drivers_and_mirror_translation(golden_raw, cfg):
    seed = build(golden_raw, cfg, images=_complete_images(cfg, golden_raw))
    characters = seed["characters.json"]["items"]
    assert all((c["unlockCriteria"] is None) == c["starter"] for c in characters)
    steps = seed["mirror_mode.json"]["items"]
    assert [s["text"] for s in steps] == [m["en"] for m in cfg.mirror_mode_it]
    assert all(s["textIt"] for s in steps)


def test_build_fails_when_a_starter_has_a_criterion(golden_raw, cfg):
    images = _complete_images(cfg, golden_raw)
    criteria = {**images.unlock_criteria, "Mario": "?"}
    with pytest.raises(ParseError, match="mario"):
        build(golden_raw, cfg, images=replace(images, unlock_criteria=criteria))


def test_build_fails_when_mirror_text_changes_on_the_wiki(golden_raw, cfg):
    images = _complete_images(cfg, golden_raw)
    steps = [*images.mirror_steps[:-1], "Something new"]
    with pytest.raises(ParseError, match="mirror_mode_it.yaml"):
        build(golden_raw, cfg, images=replace(images, mirror_steps=steps))

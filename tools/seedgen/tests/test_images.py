"""Test di seedgen/images.py: parser su HTML sintetico e controlli di build.

La pagina reale ("Mario Kart World", oltre 1 MB) non è una fixture committata: il confronto con la
pagina vera sta in test_images_live.py, che richiede la rete.
"""

import copy

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
    + _heading(2, "Courses")
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
    characters = {e.name: f"{CDN}/c/{e.id}.png" for e in cfg.characters.entities}
    outfits = {}
    for group in raw.food_groups:
        for o in group.outfits:
            outfits[(o.character, o.outfit)] = f"{CDN}/o/{o.character}_{o.outfit}.png"
    events = {e.name: f"{CDN}/e/{e.name}.png" for e in [*raw.cups, *raw.rallies]}
    return Images(characters=characters, outfits=outfits, events=events)


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
        build(golden_raw, cfg, images=Images(images.characters, images.outfits, events))


def test_build_fails_on_image_of_unknown_outfit(golden_raw, cfg):
    images = _complete_images(cfg, golden_raw)
    outfits = {**images.outfits, ("Mario", "Astronaut"): f"{CDN}/o/x.png"}
    with pytest.raises(ParseError, match="mario__astronaut"):
        build(golden_raw, cfg, images=Images(images.characters, outfits, images.events))


def test_validation_rejects_foreign_image_host(golden_raw, cfg):
    seed = copy.deepcopy(build(golden_raw, cfg, images=_complete_images(cfg, golden_raw)))
    seed["events.json"]["items"][0]["imageUrl"] = "https://example.com/x.png"
    with pytest.raises(ValidationError, match="imageUrl"):
        validate(seed, cfg)

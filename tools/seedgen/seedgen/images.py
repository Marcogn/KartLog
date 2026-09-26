"""URL delle immagini di personaggi, outfit ed eventi, dalla pagina "Mario Kart World" del wiki.

seedgen NON scarica le immagini: ne registra solo l'URL (file originale su mario.wiki.gallery, il CDN
di Super Mario Wiki), che l'app scarica a runtime. Le immagini non sono coperte dalla licenza CC BY-SA
del testo del wiki (sono screenshot/artwork del gioco) e per questo non entrano nel repository né
nell'APK: vedi CLAUDE.md, "Decisioni prese".

Struttura della pagina (verificata a mano il 26/09/2026, revid 5498278):
- sezioni "Default drivers" e "Unlockable drivers": una galleria, un'immagine per pilota (compresi i
  piloti senza outfit, es. Goomba, che KartLog non traccia e vengono ignorati);
- sezione "Character outfits": per ogni personaggio un `<dl><dt>Nome</dt></dl>` seguito da una
  galleria con un'immagine per outfit alternativo (didascalia = nome dell'outfit). L'outfit di
  default non è in questa galleria: la sua immagine è quella del personaggio;
- tabelle dei Gran Premi e dei Knockout Tour: prima cella di ogni riga (`th.subheader`) con l'icona
  della cup/del rally e il link col suo nome.

Come per i nomi in altre lingue (seedgen/i18n.py) la pagina si scarica sempre dal vivo, mai da
fixture: è di oltre 1 MB e se ne usa una piccola parte.
"""

from __future__ import annotations

from dataclasses import dataclass, field

from bs4 import BeautifulSoup, Tag

from .errors import ParseError

DRIVER_SECTIONS = ("Default_drivers", "Unlockable_drivers")
OUTFIT_SECTION = "Character_outfits"


@dataclass(frozen=True)
class Images:
    """Nomi come li scrive il wiki (non ancora slug): la risoluzione avviene in build.py."""

    characters: dict[str, str] = field(default_factory=dict)             # titolo pagina pilota -> URL
    outfits: dict[tuple[str, str], str] = field(default_factory=dict)   # (personaggio, outfit) -> URL
    events: dict[str, str] = field(default_factory=dict)                 # "Mushroom Cup" -> URL
    # Titoli dei piloti nella galleria "Default drivers": disponibili dall'inizio. Gli altri
    # (galleria "Unlockable drivers") si sbloccano giocando.
    starters: frozenset[str] = frozenset()

    def is_empty(self) -> bool:
        return not (self.characters or self.outfits or self.events)


def original_url(img: Tag) -> str:
    """URL del file originale a partire dal thumbnail mostrato nella pagina.

    `.../images/thumb/5/51/MarioMKworld.png/100px-MarioMKworld.png` -> `.../images/5/51/MarioMKworld.png`.
    """
    src = img.get("src") or ""
    if src.startswith("//"):
        src = "https:" + src
    if "/images/thumb/" in src:
        src = src.replace("/images/thumb/", "/images/", 1).rsplit("/", 1)[0]
    if not src.startswith("https://"):
        raise ParseError(f"URL immagine inatteso: {img.get('src')!r}")
    return src


def _section_nodes(soup: BeautifulSoup, anchor: str) -> list[Tag]:
    """Elementi fratelli tra l'intestazione con id `anchor` e la successiva di livello pari o superiore."""
    headline = soup.find(class_="mw-headline", id=anchor)
    if headline is None:
        raise ParseError(f"Mario Kart World: sezione {anchor!r} non trovata")
    heading = headline.find_parent(["h2", "h3", "h4"])
    level = int(heading.name[1])
    nodes = []
    for sibling in heading.find_next_siblings():
        if sibling.name in {f"h{n}" for n in range(1, level + 1)}:
            break
        nodes.append(sibling)
    return nodes


def _gallery_items(gallery: Tag) -> list[tuple[Tag, Tag]]:
    """(img, cella didascalia) per ogni elemento di una galleria MediaWiki."""
    items = []
    for box in gallery.find_all("li", class_="gallerybox"):
        img = box.find("img")
        text = box.find(class_="gallerytext")
        if img is not None and text is not None:
            items.append((img, text))
    return items


def _parse_drivers(soup: BeautifulSoup) -> tuple[dict[str, str], frozenset[str]]:
    characters: dict[str, str] = {}
    starters: set[str] = set()
    for anchor in DRIVER_SECTIONS:
        for node in _section_nodes(soup, anchor):
            galleries = [node] if "gallery" in (node.get("class") or []) else node.find_all("ul", class_="gallery")
            for gallery in galleries:
                for img, text in _gallery_items(gallery):
                    link = text.find("a")
                    if link is not None:
                        title = link.get("title") or link.get_text(strip=True)
                        characters.setdefault(title, original_url(img))
                        if anchor == "Default_drivers":
                            starters.add(title)
    return characters, frozenset(starters)


def _parse_outfits(soup: BeautifulSoup) -> dict[tuple[str, str], str]:
    outfits: dict[tuple[str, str], str] = {}
    character: str | None = None
    for node in _section_nodes(soup, OUTFIT_SECTION):
        if node.name == "dl" and node.find("dt") is not None:
            character = node.find("dt").get_text(strip=True)
        elif node.name == "ul" and "gallery" in (node.get("class") or []):
            if character is None:
                raise ParseError("Mario Kart World: galleria di outfit senza personaggio")
            for img, text in _gallery_items(node):
                outfits[(character, text.get_text(strip=True))] = original_url(img)
    return outfits


def _parse_events(soup: BeautifulSoup) -> dict[str, str]:
    events: dict[str, str] = {}
    for th in soup.find_all("th", class_="subheader"):
        img = th.find("img")
        links = [a for a in th.find_all("a") if "image" not in (a.get("class") or [])]
        if img is None or not links:
            continue
        name = links[-1].get_text(strip=True)
        if name.endswith(" Cup") or name.endswith(" Rally"):
            events.setdefault(name, original_url(img))
    return events


def extract_images(html: str) -> Images:
    soup = BeautifulSoup(html, "html.parser")
    characters, starters = _parse_drivers(soup)
    return Images(characters=characters, outfits=_parse_outfits(soup), events=_parse_events(soup), starters=starters)

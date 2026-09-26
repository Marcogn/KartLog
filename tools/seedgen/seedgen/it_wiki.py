"""Nomi italiani da Super Mario Wiki italiana (mariowiki.it, CC BY-SA 4.0 come mariowiki.com).

Seconda fonte, usata SOLO per ciò che mariowiki.com non ha (decisione dell'autore del 26/09/2026):
- i piloti senza outfit (i 24 con outfit hanno già il nome da seedgen/i18n.py, e coincidono);
- i nomi dei 10 biomi;
- i nomi dei Gran Premi (Trofei) e dei Knockout Tour (rally).

Nessun nome è tradotto qui: si leggono dalle pagine, e l'abbinamento con gli elementi del seed è
sempre verificabile, mai per posizione:
- piloti: collegamento interlingua (langlink) della pagina italiana verso quella inglese, oppure
  `itWikiPage` in aliases.yaml quando il langlink manca;
- Trofei e rally: sequenza delle tappe, confrontata con i nomi italiani dei corsi (seedgen/i18n.py);
- biomi: corsi citati nell'intro della sezione ("Questo bioma contiene ...").

Le missioni dei pulsanti P NON vengono da qui: la lista italiana è "in costruzione" (26/09/2026: una
trentina di traduzioni su 394) e non ha una chiave in comune con quella inglese (ordine diverso,
luogo e tempo spesso assenti): solo 6 righe si abbinerebbero con certezza.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

from bs4 import BeautifulSoup, Tag

from .errors import ParseError

DRIVER_SECTIONS = ("Di_base", "Sbloccabili")
CUP_SECTION = "Gran_Premio_2"          # il primo "Gran_Premio" è la modalità di gioco, non i percorsi
RALLY_SECTION = "Modalità_sopravvivenza"
# "Questi bioma" compare così su alcune sezioni della pagina (refuso del wiki).
BIOME_INTRO = re.compile(r"Quest[oi] bioma contiene (.+?)\.?\s*$")


def norm(text: str) -> str:
    """Chiave di confronto: spazi (anche non separabili) compressi, maiuscole e punto finale ignorati
    ("Circuito Mario Bros." a fine frase perde il punto nell'intro dei biomi)."""
    return " ".join(text.replace("\xa0", " ").split()).casefold().rstrip(".")


def _text(tag: Tag) -> str:
    """Testo senza le note a piè di pagina (<sup>, es. "Rally Trivella" + nota "1")."""
    for sup in tag.find_all("sup"):
        sup.decompose()
    return " ".join(tag.get_text(" ", strip=True).split())


@dataclass(frozen=True)
class ItEvent:
    name: str             # "Trofeo Fungo", "Rally Turbo"
    stops: list[str]      # nomi italiani dei corsi, in ordine


@dataclass(frozen=True)
class ItBiome:
    name: str             # "Bioma della Mesa"
    courses: list[str]    # nomi italiani dei corsi citati nella sezione


@dataclass(frozen=True)
class ItNames:
    """Dati grezzi di mariowiki.it, nomi come li scrive il wiki: la risoluzione avviene in build.py."""

    drivers: dict[str, str] = field(default_factory=dict)   # titolo pagina italiana -> nome mostrato
    langlinks: dict[str, str] = field(default_factory=dict)  # titolo pagina italiana -> titolo inglese
    events: list[ItEvent] = field(default_factory=list)
    biomes: list[ItBiome] = field(default_factory=list)

    def is_empty(self) -> bool:
        return not (self.drivers or self.events or self.biomes)


def _heading(soup: BeautifulSoup, anchor: str) -> Tag:
    headline = soup.find(class_="mw-headline", id=anchor)
    if headline is None:
        raise ParseError(f"mariowiki.it: sezione {anchor!r} non trovata")
    return headline.find_parent(["h2", "h3", "h4"])


def _section_nodes(soup: BeautifulSoup, anchor: str) -> list[Tag]:
    heading = _heading(soup, anchor)
    level = int(heading.name[1])
    stop = {f"h{n}" for n in range(1, level + 1)}
    nodes = []
    for sibling in heading.find_next_siblings():
        if sibling.name in stop:
            break
        nodes.append(sibling)
    return nodes


def _cell_link_text(cell: Tag) -> str:
    """Testo del primo link della cella (esclusi i link-immagine): il nome, senza note tra parentesi."""
    for a in cell.find_all("a"):
        if a.find("img") is None and "mw-file-description" not in (a.get("class") or []):
            return " ".join(a.get_text(" ", strip=True).split())
    return " ".join(cell.get_text(" ", strip=True).split())


def extract_drivers(html: str) -> dict[str, str]:
    """Titolo della pagina italiana -> nome mostrato, per le sezioni "Di base" e "Sbloccabili"."""
    soup = BeautifulSoup(html, "html.parser")
    drivers: dict[str, str] = {}
    for anchor in DRIVER_SECTIONS:
        for node in _section_nodes(soup, anchor):
            if node.name != "table":
                continue
            # Solo la tabella-galleria (celle con l'icona del pilota): nella stessa sezione c'è
            # anche "Criteri di sblocco", con link a Trofei e oggetti.
            for cell in node.find_all("td"):
                if cell.find("img") is None:
                    continue
                links = [a for a in cell.find_all("a") if a.find("img") is None and a.get("title")]
                if links:
                    drivers[links[-1]["title"]] = links[-1].get_text(" ", strip=True)
    return drivers


def _event_table(soup: BeautifulSoup, anchor: str) -> Tag:
    tables = [n for n in _section_nodes(soup, anchor) if n.name == "table"]
    if len(tables) != 1:
        raise ParseError(f"mariowiki.it: attesa 1 tabella nella sezione {anchor!r}, trovate {len(tables)}")
    return tables[0]


def extract_events(html: str) -> list[ItEvent]:
    """Trofei (nome in `th`) e rally (nome nella prima `td`), con le tappe in ordine."""
    soup = BeautifulSoup(html, "html.parser")
    events: list[ItEvent] = []
    for anchor in (CUP_SECTION, RALLY_SECTION):
        for tr in _event_table(soup, anchor).find_all("tr"):
            cells = tr.find_all(["th", "td"], recursive=False)
            if len(cells) < 2 or not tr.find("td"):
                continue  # riga di intestazione
            name = _text(cells[0])
            events.append(ItEvent(name=name, stops=[_cell_link_text(c) for c in cells[1:]]))
    return events


def extract_biomes(html: str) -> list[ItBiome]:
    """Un bioma per sezione h2 della lista missioni, con i corsi citati nell'intro ("Questo bioma contiene
    ..."). La colonna Luogo delle missioni non si usa: a volte indica la destinazione in un altro bioma
    (es. una missione del deserto verso lo Stadio di Wario, che è nella Mesa)."""
    soup = BeautifulSoup(html, "html.parser")
    biomes: list[ItBiome] = []
    for headline in soup.select("h2 > .mw-headline"):
        heading = headline.find_parent("h2")
        name = " ".join(headline.get_text(" ", strip=True).split())
        courses: list[str] = []
        for sibling in heading.find_next_siblings():
            if sibling.name == "h2":
                break
            if sibling.name == "p":
                m = BIOME_INTRO.search(" ".join(sibling.get_text(" ", strip=True).split()))
                if m:
                    courses += [c.strip() for c in re.split(r",\s*|\s+e\s+", m.group(1)) if c.strip()]
        if courses:
            biomes.append(ItBiome(name=name, courses=courses))
    return biomes

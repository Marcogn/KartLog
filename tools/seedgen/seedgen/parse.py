"""Parser delle tre pagine sorgente (HTML restituito dalla MediaWiki API).

Ogni parser cerca la tabella giusta per *intestazioni*, non per posizione, e fallisce con un messaggio
chiaro se la struttura non è quella attesa. Mai indovinare.
"""

from __future__ import annotations

import re

from bs4 import BeautifulSoup, Tag

from .config import Config
from .errors import ParseError
from .html_table import Grid, cell_lines, cell_text, expand, find_column, link_titles
from .i18n import Translations, extract_outfit_translations, extract_single_italian_name
from .images import original_url
from .raw import RawData, RawEvent, RawFoodGroup, RawFoodVariant, RawMission, RawOutfit, RawSource, RawStand
from .wiki import WikiPage

OUTFIT_LINE = re.compile(r"^(?P<character>.+?)\s*\((?P<outfit>[^()]+)\)$")


def _soup(page: WikiPage) -> BeautifulSoup:
    return BeautifulSoup(page.html, "html.parser")


def _tables_with_headers(soup: BeautifulSoup, *required: str) -> list[Grid]:
    found = []
    for table in soup.find_all("table"):
        grid = expand(table)
        labels = grid.header_labels()
        if all(find_column(labels, h) >= 0 for h in required):
            found.append(grid)
    return found


# ---------------------------------------------------------------------------
# Dash Food
# ---------------------------------------------------------------------------

def _food_name(cell: Tag | None) -> str:
    """Nome inglese del cibo: il primo <b> della cella, altrimenti il testo prima della parentesi."""
    if cell is None:
        return ""
    bold = cell.find("b")
    text = cell_text(bold) if bold else cell_text(cell).split("(")[0]
    return text.strip()


def _food_variant(name_cell: Tag | None, boost_cell: Tag | None, image_cell: Tag | None, title: str) -> RawFoodVariant:
    """Una riga della tabella = un cibo (variante del gruppo), con la sua immagine: solo l'URL del file
    originale, mai il file (SPEC §5.2). Un'immagine mancante ferma l'estrazione."""
    name = _food_name(name_cell)
    img = image_cell.find("img") if image_cell is not None else None
    if not name or img is None:
        raise ParseError(f"{title}: riga di List of food senza nome o senza immagine ({name!r})")
    return RawFoodVariant(name=name, boost=cell_text(boost_cell), image_url=original_url(img))


def parse_dash_food(page: WikiPage) -> list[RawFoodGroup]:
    grids = _tables_with_headers(_soup(page), "Name", "Outfits", "Locations")
    if len(grids) != 1:
        raise ParseError(
            f"{page.title}: attesa 1 tabella con colonne Name/Outfits/Locations, trovate {len(grids)}"
        )
    grid = grids[0]
    labels = grid.header_labels()
    name_col = find_column(labels, "Name")
    # La colonna Name ha colspan=2: la prima è il gruppo ("Snacks 1"), la seconda il cibo della riga
    # ("Canned juice"); per quasi tutti i cibi è la stessa cella.
    variant_col = max(i for i, lab in enumerate(labels) if lab.casefold() == "name")
    image_col = find_column(labels, "Image")
    boost_col = find_column(labels, "Boost level")
    outfit_col = find_column(labels, "Outfits")
    loc_col = find_column(labels, "Locations")
    if image_col < 0 or boost_col < 0:
        raise ParseError(f"{page.title}: colonne Image e Boost level attese nella tabella List of food")

    # Un gruppo = una cella Outfits distinta (le righe dei tier piccolo/medio/grande la condividono
    # via rowspan; takoyaki/candy apple/taiyaki/sushi condividono la stessa cella).
    groups: dict[int, dict] = {}
    for row in grid.body():
        outfit_cell = row[outfit_col]
        if outfit_cell is None:
            continue
        g = groups.setdefault(id(outfit_cell), {"cell": outfit_cell, "names": [], "courses": [], "variants": []})
        name = _food_name(row[name_col])
        if name and name not in g["names"]:
            g["names"].append(name)
        g["variants"].append(_food_variant(row[variant_col], row[boost_col], row[image_col], page.title))
        for title in link_titles(row[loc_col]):
            if title not in g["courses"]:
                g["courses"].append(title)

    result: list[RawFoodGroup] = []
    for g in groups.values():
        lines = cell_lines(g["cell"])
        label = " / ".join(g["names"]) or "?"
        if not lines:
            raise ParseError(f"{page.title}: cella Outfits vuota per il gruppo {label!r}")
        if " ".join(lines).upper().startswith("N/A"):
            result.append(RawFoodGroup(names=g["names"], outfits=[], courses=g["courses"], reverts_to_default=True,
                                       variants=g["variants"]))
            continue
        outfits = []
        for line in lines:
            m = OUTFIT_LINE.match(line)
            if not m:
                raise ParseError(f"{page.title}: riga outfit non riconosciuta nel gruppo {label!r}: {line!r}")
            outfits.append(RawOutfit(m["character"].strip(), m["outfit"].strip()))
        result.append(RawFoodGroup(names=g["names"], outfits=outfits, courses=g["courses"], variants=g["variants"]))
    if not result:
        raise ParseError(f"{page.title}: nessun gruppo di cibo trovato")
    return result


# ---------------------------------------------------------------------------
# Navbox: cup e lista rally
# ---------------------------------------------------------------------------

def _navbox_rows(page: WikiPage) -> list[tuple[str, Tag]]:
    """Coppie (etichetta riga, ultima cella con link) per ogni riga della navbox."""
    rows = []
    for tr in _soup(page).find_all("tr"):
        cells = tr.find_all(["th", "td"], recursive=False)
        if len(cells) < 2:
            continue
        label = cell_text(cells[0])
        content = next((c for c in reversed(cells[1:]) if c.find("a")), None)
        if label and content is not None:
            rows.append((label, content))
    return rows


def parse_cups(page: WikiPage, cfg: Config) -> list[RawEvent]:
    rows = _navbox_rows(page)
    cups = []
    for cup_name in cfg.sources["cups"]:
        matches = [content for label, content in rows if label.casefold() == cup_name.casefold()]
        if len(matches) != 1:
            raise ParseError(f"{page.title}: attesa 1 riga {cup_name!r} nella navbox, trovate {len(matches)}")
        titles = [t for t in link_titles(matches[0]) if not cfg.courses.is_ignored(t)]
        cups.append(RawEvent(name=cup_name, courses=titles))
    return cups


def navbox_rally_titles(page: WikiPage) -> list[str]:
    rows = [content for label, content in _navbox_rows(page) if "knockout tour" in label.casefold()]
    if len(rows) != 1:
        raise ParseError(f"{page.title}: attesa 1 riga 'Knockout Tour rallies' nella navbox, trovate {len(rows)}")
    return link_titles(rows[0])


# ---------------------------------------------------------------------------
# Pagine dei rally
# ---------------------------------------------------------------------------

def _is_stop_label(label: str) -> bool:
    low = label.casefold()
    return low == "starting point" or low == "final course" or re.fullmatch(r"checkpoint \d+", low) is not None


def _course_in_cell(cell: Tag | None, cfg: Config, context: str) -> str:
    """Il primo link della cella che è un corso noto. Gli altri link (es. il gioco d'origine
    'Mario Kart DS' accanto a Desert Hills) sono ignorati. Nessun corso -> errore."""
    titles = link_titles(cell)
    for t in titles:
        if cfg.courses.knows(t):
            return t
    raise ParseError(f"{context}: nessun corso riconosciuto nella cella (link: {titles})")


def parse_rally(page: WikiPage, cfg: Config) -> RawEvent:
    soup = _soup(page)
    # Orientamento A: una riga di intestazione "Starting point | Checkpoint 1 | ... | Final course".
    for grid in _tables_with_headers(soup, "Starting point", "Final course"):
        labels = grid.header_labels()
        cols = [i for i, lab in enumerate(labels) if _is_stop_label(lab)]
        body = [r for r in grid.body() if any(r[i] is not None for i in cols)]
        if body:
            row = body[0]
            courses = [_course_in_cell(row[i], cfg, f"{page.title} / {labels[i]}") for i in cols]
            return RawEvent(name=page.title, courses=courses)
    # Orientamento B: una riga per tappa, etichetta nella prima colonna.
    for table in soup.find_all("table"):
        grid = expand(table)
        stops = [(cell_text(r[0]), r[-1]) for r in grid.rows if r and r[0] is not None and _is_stop_label(cell_text(r[0]))]
        labels = [s[0].casefold() for s in stops]
        if "starting point" in labels and "final course" in labels:
            courses = [_course_in_cell(cell, cfg, f"{page.title} / {lab}") for lab, cell in stops]
            return RawEvent(name=page.title, courses=courses)
    raise ParseError(f"{page.title}: tabella delle tappe (Starting point ... Final course) non trovata")


# ---------------------------------------------------------------------------
# Pagine con sezioni: intestazioni + tabelle in ordine di documento
# ---------------------------------------------------------------------------

def _heading_text(tag: Tag) -> str:
    text = cell_text(tag)
    return re.sub(r"\s*\[\s*edit\s*\]\s*$", "", text, flags=re.I).strip()


def _sections(page: WikiPage):
    """Genera (h2, h3, tabella) per ogni tabella di primo livello, con le intestazioni che la precedono."""
    h2 = h3 = ""
    for el in _soup(page).find_all(["h2", "h3", "table"]):
        if el.name == "h2":
            h2, h3 = _heading_text(el), ""
        elif el.name == "h3":
            h3 = _heading_text(el)
        elif el.find_parent("table") is None:
            yield h2, h3, el


def _yoshi_labels(cell: Tag | None) -> list[str]:
    """Etichette di cibo di una cella: separate da <br> ("Snacks<br>Soft drinks…") o da virgole."""
    return [p.strip() for line in cell_lines(cell) for p in line.split(",") if p.strip()]


def _optional_text(cell: Tag | None) -> str | None:
    """Testo della cella senza note a piè di pagina; None se vuota (un luogo assente non si inventa)."""
    text = " ".join(cell_lines(cell))
    return text or None


def parse_yoshis(page: WikiPage) -> list[RawStand]:
    """Tutti gli stand, uno per riga (ogni riga ha la sua foto): sezione 'Course locations' (un h3 per
    corso, colonne Establishment/Dash Food/Location) e 'Route locations' (una tabella, colonne Dash
    Food/Location). Le celle con rowspan valgono per tutte le righe che coprono."""
    stands: list[RawStand] = []
    seen_sections: set[str] = set()
    for h2, h3, table in _sections(page):
        section = h2.casefold()
        if section not in ("course locations", "route locations"):
            continue
        grid = expand(table)
        labels = grid.header_labels()
        food_col, loc_col = find_column(labels, "Dash Food"), find_column(labels, "Location")
        if food_col < 0 and section == "route locations":
            continue  # la navbox in fondo alla pagina
        if food_col < 0 or loc_col < 0:
            raise ParseError(f"{page.title} / {h3 or h2}: tabella senza colonne 'Dash Food' e 'Location'")
        on_course = section == "course locations"
        if on_course and not h3:
            raise ParseError(f"{page.title}: tabella in 'Course locations' senza intestazione di corso")
        est_col = find_column(labels, "Establishment")
        if on_course and est_col < 0:
            raise ParseError(f"{page.title} / {h3}: tabella senza colonna 'Establishment'")
        seen_sections.add(section)
        for row in grid.body():
            stands.append(RawStand(
                course=h3 if on_course else None,
                foods=_yoshi_labels(row[food_col]),
                establishment=_optional_text(row[est_col]) if est_col >= 0 else None,
                location=_optional_text(row[loc_col]),
            ))
    missing = {"course locations", "route locations"} - seen_sections
    if missing:
        raise ParseError(f"{page.title}: sezioni non trovate o vuote: {sorted(missing)}")
    return stands


def _location_name(cell: Tag | None, cfg: Config) -> str:
    for t in link_titles(cell):
        if cfg.courses.knows(t) or cfg.areas.knows(t):
            return t
    return cell_text(cell)


def parse_missions(page: WikiPage, cfg: Config) -> list[RawMission]:
    missions: list[RawMission] = []
    for h2, _h3, table in _sections(page):
        grid = expand(table)
        labels = grid.header_labels()
        name_col, loc_col = find_column(labels, "In-game text"), find_column(labels, "Location")
        if name_col < 0 or loc_col < 0:
            continue
        seen_rows = set()
        for row in grid.body():
            key = id(row[name_col])
            if row[name_col] is None or key in seen_rows:
                continue
            seen_rows.add(key)
            missions.append(RawMission(region=h2, name=cell_text(row[name_col]), location=_location_name(row[loc_col], cfg)))
    if not missions:
        raise ParseError(f"{page.title}: nessuna tabella con colonne 'In-game text' e 'Location'")
    return missions


# ---------------------------------------------------------------------------

def parse_all(pages: dict[str, WikiPage], cfg: Config) -> RawData:
    src = cfg.sources["pages"]
    navbox = pages[src["navbox"]]

    listed = set(src["rallies"])
    on_wiki = set(navbox_rally_titles(navbox))
    new = sorted(on_wiki - listed)
    if new:
        raise ParseError(
            f"La navbox elenca rally non presenti in sources.yaml: {new}. "
            "Aggiungerli a sources.yaml (e aggiornare expected_counts.yaml) dopo averli verificati."
        )

    rallies = []
    for title in src["rallies"]:
        event = parse_rally(pages[title], cfg)
        event.name = title.split(" (")[0]          # "Boomerang Rally (Mario Kart World)" -> "Boomerang Rally"
        rallies.append(event)

    # Solo le pagine dei dati di gioco "core", non le ~55 pagine per personaggio/corso/outfit usate
    # solo per il nome italiano (seedgen/i18n.py): quelle non vanno nell'attribuzione principale,
    # affollerebbero la schermata Info per un dato supplementare. `pages` qui dentro può contenere
    # anche quelle (viene dal fetch completo di wiki.all_titles), per questo si elenca esplicitamente
    # solo ciò che parse_all usa davvero.
    core_titles = [src["dash_food"], src["yoshis"], src["missions"], src["navbox"], *src["rallies"]]
    return RawData(
        food_groups=parse_dash_food(pages[src["dash_food"]]),
        cups=parse_cups(navbox, cfg),
        rallies=rallies,
        stands=parse_yoshis(pages[src["yoshis"]]),
        missions=parse_missions(pages[src["missions"]], cfg),
        sources=[RawSource(pages[t].title, pages[t].revid) for t in core_titles],
        origin="api",
    )


def parse_translations(pages: dict[str, WikiPage], cfg: Config) -> Translations:
    """Nomi ufficiali in italiano (SPEC, "nomi ufficiali"): vedi seedgen/i18n.py per le regole."""
    src = cfg.sources["pages"]
    character_it = {}
    for e in cfg.characters.entities:
        page = pages.get(e.i18n_page())
        if page is not None:
            name = extract_single_italian_name(page.html)
            if name:
                character_it[e.id] = name
    course_it = {}
    for e in cfg.courses.entities:
        page = pages.get(e.i18n_page())
        if page is not None:
            name = extract_single_italian_name(page.html)
            if name:
                course_it[e.id] = name
    outfit_it = {}
    outfit_page = pages.get(src["outfit_names_it"])
    if outfit_page is not None:
        outfit_it = extract_outfit_translations(outfit_page.html)
    return Translations(character_it=character_it, course_it=course_it, outfit_it=outfit_it)

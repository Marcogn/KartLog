"""Nomi ufficiali in altre lingue, estratti dalle tabelle "Names in other languages" del wiki.

Mai una traduzione automatica: solo ciò che compare, citato, nella tabella `id="foreign-names"` di
una pagina mariowiki (licenza CC BY-SA 4.0, stessa fonte del resto di seedgen). Se una lingua non ha
una riga per un personaggio/corso/outfit, il campo resta `None` e l'app mostra il nome inglese.

Personaggi e corsi: una pagina dedicata per elemento (titolo in `aliases.yaml`, campo `wikiPage` se
diverso dal nome), con al più una riga "Italian" (o poche, se il nome è cambiato nel tempo: si tiene
l'ultima, la più recente, come da convenzione dichiarata sulle pagine stesse: "Subsequent names are
listed in chronological order... from oldest to newest").

Outfit: un'unica pagina ("List of Mario Kart World outfit names in other languages") con una sezione
per ogni NOME di outfit (a volte una sezione copre più nomi inglesi insieme, es. "Biker / Biker Jr.").
Alcune lingue (l'italiano compreso, per alcuni outfit) distinguono maschile/femminile: la riga
"Italian" può quindi comparire una o due volte per sezione, con la colonna Note(s) che dice quale.
`OUTFIT_SECTIONS` registra quali nomi copre ciascuna sezione della pagina: è un fatto strutturale
della pagina wiki (verificato a mano leggendola), non un dato di gioco inventato.
"""

from __future__ import annotations

from dataclasses import dataclass, field

from bs4 import BeautifulSoup, Tag

from .html_table import cell_text, expand, find_column

# Sezione (id dell'ancora "mw-headline") -> nomi di outfit inglesi che copre. Verificato a mano sulla
# pagina "List of Mario Kart World outfit names in other languages" (25/09/2026).
OUTFIT_SECTIONS: dict[str, list[str]] = {
    "Aero": ["Aero"],
    "All-Terrain": ["All-Terrain"],
    "Aristocrat": ["Aristocrat"],
    "Aurora": ["Aurora"],
    "Aviator": ["Aviator"],
    "Biker_.2F_Biker_Jr.": ["Biker", "Biker Jr."],
    "Burger_Bud": ["Burger Bud"],
    "Conductor": ["Conductor"],
    "Cowboy": ["Cowboy"],
    "Dune_Rider_.2F_Oasis": ["Dune Rider", "Oasis"],
    "Engineer": ["Engineer"],
    "Explorer": ["Explorer"],
    "Farmer": ["Farmer"],
    "Fisherman": ["Fisherman"],
    "Food_Slinger": ["Food Slinger"],
    "Gondolier": ["Gondolier"],
    "Happi_.2F_Matsuri_.2F_Yukata": ["Happi", "Matsuri", "Yukata"],
    "Mariachi": ["Mariachi"],
    "Mechanic": ["Mechanic"],
    "Pirate": ["Pirate"],
    "Pit_Crew": ["Pit Crew"],
    "Pro_Racer": ["Pro Racer"],
    "Road_Ruffian": ["Road Ruffian"],
    "Runner": ["Runner"],
    "Sailor": ["Sailor"],
    "Sightseeing": ["Sightseeing"],
    "Slope_Styler": ["Slope Styler"],
    "Soft_Server": ["Soft Server"],
    "Supercharged": ["Supercharged"],
    "Swimwear": ["Swimwear"],
    "Touring": ["Touring"],
    "Vacation": ["Vacation"],
    "Wampire": ["Wampire"],
    "Wicked_Wasp": ["Wicked Wasp"],
    "Work_Crew": ["Work Crew"],
}


def _foreign_name_cell_text(cell: Tag | None) -> str:
    if cell is None:
        return ""
    span = cell.find("span", lang="it")
    text = span.get_text(strip=True) if span is not None else cell_text(cell)
    text = text.strip()
    return "" if text == "-" else text


def _italian_rows(table: Tag) -> list[tuple[Tag | None, Tag | None]]:
    """Righe (cella Name, cella Note(s)) per la lingua "Italian" in una tabella foreign-names."""
    grid = expand(table)
    labels = grid.header_labels()
    lang_col = find_column(labels, "Language")
    name_col = find_column(labels, "Name")
    note_col = find_column(labels, "Note(s)")
    if lang_col == -1 or name_col == -1:
        return []
    rows = []
    for row in grid.body():
        if cell_text(row[lang_col]) == "Italian":
            note_cell = row[note_col] if note_col != -1 else None
            rows.append((row[name_col], note_cell))
    return rows


def extract_single_italian_name(html: str) -> str | None:
    """Nome italiano attuale da una pagina con una sola tabella foreign-names (personaggio o corso).

    Per convenzione della pagina stessa ("The contemporaneous name for each language is listed
    first. Subsequent names are listed in chronological order... from oldest to newest, and have
    the media with which they are associated in the Note(s) column."): la riga SENZA nota è quella
    attuale, le successive con nota sono varianti storiche (es. Toad in italiano ha anche
    "Suddito dei Funghi" nota "Super Mario Bros. 2" e "Ughetto" nota "DIC cartoons" — non quelle).
    """
    soup = BeautifulSoup(html, "html.parser")
    table = soup.find("table", id="foreign-names")
    if table is None:
        return None
    entries = [
        (_foreign_name_cell_text(name_cell), cell_text(note_cell))
        for name_cell, note_cell in _italian_rows(table)
    ]
    entries = [(name, note) for name, note in entries if name]
    if not entries:
        return None
    current = next((name for name, note in entries if not note), None)
    return current if current is not None else entries[0][0]


@dataclass(frozen=True)
class OutfitTranslation:
    default: str | None = None
    masculine: str | None = None
    feminine: str | None = None

    def for_gender(self, gender: str | None) -> str | None:
        if gender == "F" and self.feminine:
            return self.feminine
        if gender == "M" and self.masculine:
            return self.masculine
        return self.default or self.masculine or self.feminine


_EMPTY_TRANSLATION = OutfitTranslation()


def _build_translation(entries: list[tuple[str, str]]) -> OutfitTranslation:
    if not entries:
        return _EMPTY_TRANSLATION
    if len(entries) == 1:
        return OutfitTranslation(default=entries[0][0])
    masculine = next((t for t, note in entries if note.casefold() == "masculine"), None)
    feminine = next((t for t, note in entries if note.casefold() == "feminine"), None)
    if masculine or feminine:
        return OutfitTranslation(masculine=masculine, feminine=feminine)
    # Più righe italiane senza una nota maschile/femminile riconosciuta: caso non previsto, non si
    # indovina quale usare piuttosto di un'altra.
    return _EMPTY_TRANSLATION


@dataclass(frozen=True)
class Translations:
    character_it: dict[str, str] = field(default_factory=dict)
    course_it: dict[str, str] = field(default_factory=dict)
    outfit_it: dict[str, OutfitTranslation] = field(default_factory=dict)

    def outfit_name_it(self, outfit_name: str, character_id: str, genders: dict[str, str]) -> str | None:
        translation = self.outfit_it.get(outfit_name)
        if translation is None:
            return None
        return translation.for_gender(genders.get(character_id))


def extract_outfit_translations(html: str) -> dict[str, OutfitTranslation]:
    """Nome per nome di outfit (chiave = nome inglese, es. "Explorer"), da tutte le sezioni note."""
    soup = BeautifulSoup(html, "html.parser")
    result: dict[str, OutfitTranslation] = {}
    for anchor, outfit_names in OUTFIT_SECTIONS.items():
        headline = soup.find(class_="mw-headline", id=anchor)
        if headline is None:
            continue
        heading = headline.find_parent("h2")
        if heading is None:
            continue
        table = None
        for sibling in heading.find_next_siblings():
            if sibling.name == "h2":
                break
            if sibling.name == "table" and sibling.get("id") == "foreign-names":
                table = sibling
                break
        if table is None:
            continue
        entries = [
            (_foreign_name_cell_text(cell), cell_text(note))
            for cell, note in _italian_rows(table)
        ]
        entries = [(name, note) for name, note in entries if name]
        translation = _build_translation(entries)
        for outfit_name in outfit_names:
            result[outfit_name] = translation
    return result

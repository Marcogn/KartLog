"""Espansione di una tabella HTML con rowspan/colspan in una griglia rettangolare.

Ogni posizione della griglia contiene la cella *sorgente* (lo stesso oggetto Tag per tutte le posizioni
coperte da un rowspan/colspan). Così righe diverse che condividono la stessa cella Outfits si riconoscono
come appartenenti allo stesso gruppo di cibo.
"""

from __future__ import annotations

from dataclasses import dataclass

from bs4 import BeautifulSoup, Tag


@dataclass
class Grid:
    rows: list[list[Tag | None]]
    header_rows: int

    def header_labels(self) -> list[str]:
        """Etichetta di ogni colonna, presa dall'ultima riga di intestazione."""
        if not self.header_rows:
            return []
        last = self.rows[self.header_rows - 1]
        return [cell_text(c) if c is not None else "" for c in last]

    def body(self) -> list[list[Tag | None]]:
        return self.rows[self.header_rows:]


def _span(cell: Tag, attr: str) -> int:
    try:
        return max(1, int(str(cell.get(attr, "1")).strip() or "1"))
    except ValueError:
        return 1


def expand(table: Tag) -> Grid:
    """Costruisce la griglia. Le righe di intestazione sono quelle composte solo da <th>."""
    trs = [tr for tr in table.find_all("tr") if tr.find_parent("table") is table]
    grid: list[list[Tag | None]] = []
    pending: dict[tuple[int, int], Tag] = {}  # (riga, colonna) -> cella che la occupa per rowspan
    header_rows = 0
    counting_header = True

    for r, tr in enumerate(trs):
        cells = [c for c in tr.find_all(["td", "th"], recursive=False)]
        if counting_header:
            if cells and all(c.name == "th" for c in cells):
                header_rows += 1
            else:
                counting_header = False
        row: list[Tag | None] = []
        col = 0
        for cell in cells:
            while (r, col) in pending:
                row.append(pending.pop((r, col)))
                col += 1
            rs, cs = _span(cell, "rowspan"), _span(cell, "colspan")
            for dc in range(cs):
                row.append(cell)
                for dr in range(1, rs):
                    pending[(r + dr, col + dc)] = cell
            col += cs
        # Celle a destra ancora occupate da rowspan di righe precedenti.
        trailing = sorted(c for (pr, c) in pending if pr == r)
        for c in trailing:
            while col < c:
                row.append(None)
                col += 1
            row.append(pending.pop((r, c)))
            col += 1
        grid.append(row)

    width = max((len(r) for r in grid), default=0)
    for row in grid:
        row.extend([None] * (width - len(row)))
    return Grid(rows=grid, header_rows=header_rows)


def cell_text(cell: Tag | None) -> str:
    if cell is None:
        return ""
    return " ".join(cell.get_text(" ", strip=True).split())


def cell_lines(cell: Tag | None) -> list[str]:
    """Testo della cella diviso SOLO su <br> e su elementi di blocco (p, div, li).

    I link inline restano sulla stessa riga: "<a>Mario</a> (Touring)" -> "Mario (Touring)".
    Le note a piè di pagina (<sup>) vengono rimosse.
    """
    if cell is None:
        return []
    clone = BeautifulSoup(str(cell), "html.parser")
    for sup in clone.find_all("sup"):
        sup.decompose()
    for br in clone.find_all("br"):
        br.replace_with("\n")
    for block in clone.find_all(["p", "div", "li"]):
        block.insert_before("\n")
        block.insert_after("\n")
    raw = clone.get_text("")
    return [" ".join(line.split()) for line in raw.split("\n") if line.strip()]


def link_titles(cell: Tag | None) -> list[str]:
    """Titoli dei link interni (<a title=...>) nell'ordine in cui compaiono, senza duplicati."""
    if cell is None:
        return []
    seen: list[str] = []
    for a in cell.find_all("a"):
        title = a.get("title")
        if title and title not in seen:
            seen.append(title)
    return seen


def find_column(labels: list[str], wanted: str) -> int:
    """Indice della prima colonna la cui etichetta è `wanted` (case-insensitive). -1 se assente."""
    for i, label in enumerate(labels):
        if label.casefold() == wanted.casefold():
            return i
    return -1

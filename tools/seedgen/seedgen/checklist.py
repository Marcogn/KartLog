"""Mappa dei collezionabili da mkworld-checklist (mktools.io), scelta dell'autore del 28/09/2026.

Il progetto https://github.com/BamisWasTaken/mkworld-checklist ha, per ogni Pulsante P, Moneta Peach e
pannello "?", la posizione in percentuale sulla sua immagine della mappa, un testo con le istruzioni
(solo inglese) e spesso un video YouTube. Si legge a un commit FISSATO in sources.yaml, mai dal ramo
principale: un aggiornamento del loro repository cambia i dati solo quando si cambia il commit qui.

- Monete Peach e pannelli "?" arrivano solo da qui (le monete prima erano conteggi per bioma da
  Nintendo Life, superati: le monete di questa fonte non hanno un bioma).
- I Pulsanti P restano quelli di mariowiki (nome, bioma, luogo): da qui prendono solo posizione,
  istruzioni e video, abbinati per nome della missione. Le differenze di grafia sono in
  manual/checklist_mission_names.yaml; un nome che non si abbina ferma l'estrazione.
- La mappa è solo un URL (come le immagini del wiki): la scarica l'app, mai nel repository.
"""

from __future__ import annotations

import re
import struct
import time
from dataclasses import dataclass, field

import requests

from .errors import NetworkError, ParseError

MEDALLION = "peach-coin"
PANEL = "questionmark-panel"
P_SWITCH = "p-switch"


@dataclass(frozen=True)
class MapPoint:
    source_index: int        # "index" della loro checklist: stabile tra le versioni, fa da ID
    x: float                 # percentuale della larghezza della mappa (0-100)
    y: float                 # percentuale dell'altezza
    hint: str | None         # istruzioni in inglese
    youtube_id: str | None
    mission_name: str | None = None  # solo Pulsanti P


@dataclass
class Checklist:
    commit: str
    repo_url: str
    map_url: str
    map_width: int
    map_height: int
    medallions: list[MapPoint] = field(default_factory=list)
    panels: list[MapPoint] = field(default_factory=list)
    p_switches: list[MapPoint] = field(default_factory=list)

    @property
    def source_url(self) -> str:
        return f"{self.repo_url}/tree/{self.commit}"


def webp_size(data: bytes) -> tuple[int, int]:
    """Larghezza e altezza di un WebP dall'intestazione (VP8, VP8L o VP8X), senza decodificarlo."""
    if len(data) < 30 or data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise ParseError("mappa di mkworld-checklist: non è un file WebP")
    chunk = data[12:16]
    if chunk == b"VP8X":
        w = int.from_bytes(data[24:27], "little") + 1
        h = int.from_bytes(data[27:30], "little") + 1
    elif chunk == b"VP8L":
        bits = struct.unpack("<I", data[21:25])[0]
        w = (bits & 0x3FFF) + 1
        h = ((bits >> 14) & 0x3FFF) + 1
    elif chunk == b"VP8 ":
        w, h = struct.unpack("<HH", data[26:30])
        w, h = w & 0x3FFF, h & 0x3FFF
    else:
        raise ParseError(f"mappa di mkworld-checklist: formato WebP sconosciuto {chunk!r}")
    return w, h


def mission_key(name: str) -> str:
    """Chiave di confronto dei nomi delle missioni: solo lettere e cifre, minuscole."""
    return re.sub(r"[^0-9a-z]", "", name.casefold())


def _text(texts: dict, key: str | None) -> str | None:
    """Testo di en.json per una chiave come "1056_INSTRUCTIONS" (sotto STICKERS) o "SHARED.X"."""
    if not key:
        return None
    node = texts.get("STICKERS", {})
    for part in key.split("."):
        node = node.get(part) if isinstance(node, dict) else None
    return node.strip() if isinstance(node, str) and node.strip() else None


def parse(items: list[dict], texts: dict, *, commit: str, repo_url: str, map_url: str,
          map_size: tuple[int, int]) -> Checklist:
    result = Checklist(commit=commit, repo_url=repo_url, map_url=map_url,
                       map_width=map_size[0], map_height=map_size[1])
    by_type = {MEDALLION: result.medallions, PANEL: result.panels, P_SWITCH: result.p_switches}
    for item in items:
        model = item.get("collectibleModel")
        if not model:
            continue  # voci dell'album senza posizione sulla mappa (coppe, traguardi...)
        kind = model.get("collectibleType")
        if kind not in by_type:
            raise ParseError(f"mkworld-checklist: tipo di collezionabile sconosciuto {kind!r}")
        x, y = model.get("xPercentage"), model.get("yPercentage")
        if not isinstance(x, (int, float)) or not isinstance(y, (int, float)) or not (0 <= x <= 100 and 0 <= y <= 100):
            raise ParseError(f"mkworld-checklist: posizione non valida per l'elemento {item.get('index')}")
        mission = None
        if kind == P_SWITCH:
            mission = _text(texts, model.get("missionName"))
            if not mission:
                raise ParseError(f"mkworld-checklist: Pulsante P {item['index']} senza nome della missione")
        by_type[kind].append(MapPoint(
            source_index=int(item["index"]),
            x=round(float(x), 4),
            y=round(float(y), 4),
            hint=_text(texts, item.get("instructions")),
            youtube_id=model.get("youtubeId") or None,
            mission_name=mission,
        ))
    for points in by_type.values():
        points.sort(key=lambda p: p.source_index)
    return result


def match_missions(points: list[MapPoint], names: list[str], renames: dict[str, str]) -> dict[str, MapPoint]:
    """Nome della missione di mariowiki -> punto sulla mappa. Deve essere una corrispondenza 1:1."""
    ours = {mission_key(n): n for n in names}
    renamed = {mission_key(k): mission_key(v) for k, v in renames.items()}
    result: dict[str, MapPoint] = {}
    unmatched = []
    for p in points:
        key = mission_key(p.mission_name or "")
        key = renamed.get(key, key)
        name = ours.get(key)
        if name is None:
            unmatched.append(p.mission_name)
        elif name in result:
            raise ParseError(f"mkworld-checklist: due Pulsanti P per la missione {name!r}")
        else:
            result[name] = p
    if unmatched:
        raise ParseError(
            "mkworld-checklist: missioni non abbinate a quelle di mariowiki, aggiungerle a "
            f"manual/checklist_mission_names.yaml dopo averle verificate: {unmatched}"
        )
    return result


def _get(session: requests.Session, url: str, timeout: int, retries: int) -> requests.Response:
    delay, last = 2.0, ""
    for _ in range(retries):
        try:
            resp = session.get(url, timeout=timeout)
        except requests.RequestException as exc:
            last = f"{type(exc).__name__}: {exc}"
        else:
            if resp.status_code in (200, 206):  # 206: richiesta con Range (intestazione della mappa)
                return resp
            if resp.status_code != 429 and resp.status_code < 500:
                raise NetworkError(f"HTTP {resp.status_code} per {url}")
            last = f"HTTP {resp.status_code}"
        time.sleep(delay)
        delay *= 2
    raise NetworkError(f"download fallito per {url}: {last}")


def fetch(cfg) -> Checklist:
    src = cfg.sources["mkworld_checklist"]
    http = cfg.sources.get("http", {})
    timeout, retries = http.get("timeout_seconds", 30), http.get("max_retries", 4)
    base = f"{src['raw_prefix']}{src['commit']}/"
    session = requests.Session()
    session.headers["User-Agent"] = cfg.sources["user_agent"]
    items = _get(session, base + src["data"], timeout, retries).json()
    texts = _get(session, base + src["texts"], timeout, retries).json()
    map_url = base + src["map"]
    # Solo l'intestazione, per le proporzioni: l'immagine la scarica l'app.
    session.headers["Range"] = "bytes=0-63"
    size = webp_size(_get(session, map_url, timeout, retries).content[:64])
    return parse(items, texts, commit=src["commit"], repo_url=src["repo_url"], map_url=map_url, map_size=size)

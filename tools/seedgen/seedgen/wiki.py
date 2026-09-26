"""Client minimale per la MediaWiki API di Super Mario Wiki.

Si usa `action=parse&prop=text|revid`, cioè l'HTML renderizzato *restituito dall'API* (non lo scraping
delle pagine del sito). Motivo: la tabella Dash Food usa rowspan/colspan e link a immagini
([[File:...|link=Corso]]) che nel wikitext sono scomodi da interpretare, mentre nell'HTML diventano
celle normali e tag <a title="Corso">. Il `revid` rende ogni estrazione tracciabile.

Regole di cortesia verso il wiki: richieste sequenziali, User-Agent con contatto, maxlag, retry con
backoff, nessun download di immagini (solo i loro URL, vedi seedgen/images.py).
"""

from __future__ import annotations

import json
import time
from dataclasses import dataclass
from pathlib import Path

import requests

from .errors import NetworkError, ParseError, SeedgenError


@dataclass(frozen=True)
class WikiPage:
    title: str
    revid: int
    html: str

    def to_json(self) -> dict:
        return {"title": self.title, "revid": self.revid, "html": self.html}

    @classmethod
    def from_json(cls, data: dict) -> "WikiPage":
        return cls(title=data["title"], revid=int(data["revid"]), html=data["html"])


class WikiClient:
    def __init__(self, sources: dict):
        self.api_url = sources["api_url"]
        http = sources.get("http", {})
        self.timeout = http.get("timeout_seconds", 30)
        self.total_timeout = http.get("total_timeout_seconds", 300)
        self.max_retries = http.get("max_retries", 4)
        self.maxlag = http.get("maxlag", 5)
        user_agent = sources["user_agent"]
        if "<REPO_URL>" in user_agent:
            raise SeedgenError(
                "sources.yaml: sostituire <REPO_URL> in user_agent con l'URL del repository "
                "o un contatto prima di interrogare il wiki."
            )
        self.session = requests.Session()
        self.session.headers["User-Agent"] = user_agent
        self._started = time.monotonic()

    def fetch(self, title: str) -> WikiPage:
        data = self._get(title, {
            "action": "parse",
            "page": title,
            "prop": "text|revid",
            "disablelimitreport": "1",
            "disableeditsection": "1",
            "disabletoc": "1",
            "redirects": "1",
        })
        parsed = data["parse"]
        return WikiPage(title=parsed["title"], revid=int(parsed["revid"]), html=parsed["text"])

    def langlinks(self, titles: list[str], lang: str = "en") -> dict[str, str]:
        """Titolo -> titolo della pagina collegata nella lingua `lang` (solo quelli che ne hanno una)."""
        result: dict[str, str] = {}
        for start in range(0, len(titles), 50):  # limite della MediaWiki API per `titles`
            batch = titles[start:start + 50]
            data = self._get(batch[0], {
                "action": "query",
                "prop": "langlinks",
                "lllang": lang,
                "lllimit": "max",
                "titles": "|".join(batch),
            })
            query = data.get("query", {})
            # Titoli normalizzati dall'API (es. spazi/maiuscole): si riportano a quelli richiesti.
            back = {n["to"]: n["from"] for n in query.get("normalized", [])}
            for page in query.get("pages", []):  # formatversion=2: lista, non dizionario
                links = page.get("langlinks") or []
                if links:
                    result[back.get(page["title"], page["title"])] = links[0].get("*") or links[0].get("title")
        return result

    def _get(self, label: str, params: dict) -> dict:
        params = {**params, "format": "json", "formatversion": "2", "maxlag": str(self.maxlag)}
        delay = 2.0
        last_error = ""
        for attempt in range(1, self.max_retries + 1):
            if time.monotonic() - self._started > self.total_timeout:
                raise NetworkError(f"Timeout totale superato durante il download di {label!r}")
            try:
                resp = self.session.get(self.api_url, params=params, timeout=self.timeout)
            except requests.RequestException as exc:
                last_error = f"{type(exc).__name__}: {exc}"
            else:
                if resp.status_code >= 500 or resp.status_code == 429:
                    last_error = f"HTTP {resp.status_code}"
                    delay = float(resp.headers.get("Retry-After", delay))
                elif resp.status_code != 200:
                    raise NetworkError(f"HTTP {resp.status_code} per {label!r}")
                else:
                    data = resp.json()
                    error = data.get("error")
                    if error and error.get("code") == "maxlag":
                        last_error = "maxlag"
                        delay = float(resp.headers.get("Retry-After", delay))
                    elif error:
                        # es. missingtitle: il titolo in sources.yaml non esiste più.
                        raise ParseError(f"Errore API per {label!r}: {error.get('code')}: {error.get('info')}")
                    else:
                        return data
            if attempt < self.max_retries:
                time.sleep(delay)
                delay = min(delay * 2, 60)
        raise NetworkError(f"Impossibile scaricare {label!r} dopo {self.max_retries} tentativi ({last_error})")


def all_titles(cfg) -> list[str]:
    pages = cfg.sources["pages"]
    return [pages["dash_food"], pages["yoshis"], pages["missions"], pages["navbox"], *pages["rallies"]]


def i18n_titles(cfg) -> list[str]:
    """Pagine per i nomi in altre lingue (seedgen/i18n.py): una per personaggio/corso, più quella
    degli outfit. Separate da `all_titles()` perché sono pagine di biografia INTERE (fino a 2-3 MB
    l'una) — va bene scaricarle a ogni `generate` reale, ma non hanno senso come fixture committate
    (`tests/fixtures/real/`) solo per estrarre una piccola tabella: vedi `fetch_i18n()`, sempre in
    rete, mai da fixture."""
    pages = cfg.sources["pages"]
    titles = [
        pages["outfit_names_it"],
        *(e.i18n_page() for e in cfg.characters.entities),
        *(e.i18n_page() for e in cfg.courses.entities),
    ]
    return list(dict.fromkeys(titles))  # deduplica preservando l'ordine


def fixture_name(title: str) -> str:
    return title.replace(" ", "_").replace("/", "_").replace(":", "_").replace("?", "Q") + ".json"


def save_fixtures(pages: list[WikiPage], directory: Path) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    for page in pages:
        path = directory / fixture_name(page.title)
        path.write_text(json.dumps(page.to_json(), ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


def load_fixtures(cfg, directory: Path) -> dict[str, WikiPage]:
    pages: dict[str, WikiPage] = {}
    for title in all_titles(cfg):
        path = directory / fixture_name(title)
        if not path.is_file():
            raise SeedgenError(f"Fixture mancante: {path}. Eseguire prima `python -m seedgen fetch-fixtures`.")
        pages[title] = WikiPage.from_json(json.loads(path.read_text(encoding="utf-8")))
    return pages


def fetch_all(cfg) -> dict[str, WikiPage]:
    client = WikiClient(cfg.sources)
    return {title: client.fetch(title) for title in all_titles(cfg)}


def fetch_i18n(cfg) -> dict[str, WikiPage]:
    """Sempre dal wiki, mai da fixture (vedi `i18n_titles`)."""
    client = WikiClient(cfg.sources)
    return {title: client.fetch(title) for title in i18n_titles(cfg)}


def fetch_images_page(cfg) -> WikiPage:
    """Sempre dal wiki, mai da fixture (vedi seedgen/images.py)."""
    return WikiClient(cfg.sources).fetch(cfg.sources["pages"]["images"])


def it_client(cfg) -> WikiClient:
    """Client per mariowiki.it: stesso User-Agent e stesse regole di cortesia, altra API."""
    return WikiClient({**cfg.sources, "api_url": cfg.sources["it_wiki"]["api_url"]})


def fetch_it_names(cfg):
    """Sempre dal vivo, mai da fixture (seedgen/it_wiki.py). Restituisce (ItNames, pagine lette)."""
    from .it_wiki import ItNames, extract_biomes, extract_drivers, extract_events

    client = it_client(cfg)
    pages = cfg.sources["it_wiki"]["pages"]
    game = client.fetch(pages["game"])
    missions = client.fetch(pages["missions"])
    drivers = extract_drivers(game.html)
    names = ItNames(
        drivers=drivers,
        langlinks=client.langlinks(sorted(drivers)),
        events=extract_events(game.html),
        biomes=extract_biomes(missions.html),
    )
    return names, [game, missions]

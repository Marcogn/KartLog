import sys
from pathlib import Path

import pytest

TOOL_DIR = Path(__file__).resolve().parent.parent
REPO_ROOT = TOOL_DIR.parent.parent
sys.path.insert(0, str(TOOL_DIR))

from seedgen.config import Config  # noqa: E402
from seedgen.wiki import WikiPage  # noqa: E402

SYNTHETIC = TOOL_DIR / "tests" / "fixtures" / "synthetic"
REAL = TOOL_DIR / "tests" / "fixtures" / "real"
GOLDEN = TOOL_DIR / "tests" / "golden" / "raw_manual_2026-09-25.yaml"
SEED_DIR = REPO_ROOT / "seed"


@pytest.fixture(scope="session")
def cfg() -> Config:
    return Config.load(TOOL_DIR)


def synthetic_page(filename: str, title: str) -> WikiPage:
    return WikiPage(title=title, revid=0, html=(SYNTHETIC / filename).read_text(encoding="utf-8"))

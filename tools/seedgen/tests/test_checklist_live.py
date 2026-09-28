"""seedgen/checklist.py sul repository REALE, al commit fissato in sources.yaml. Saltato senza rete."""

import pytest
import requests

from seedgen.checklist import fetch, match_missions
from seedgen.output import read_seed

from conftest import SEED_DIR

try:
    _REACHABLE = requests.head("https://raw.githubusercontent.com", timeout=5).status_code < 500
except requests.RequestException:
    _REACHABLE = False

pytestmark = pytest.mark.skipif(not _REACHABLE, reason="raw.githubusercontent.com non raggiungibile.")


@pytest.fixture(scope="module")
def checklist(cfg):
    return fetch(cfg)


def test_counts_match_expected(checklist, cfg):
    assert len(checklist.medallions) == cfg.expected["peach_medallions"]
    assert len(checklist.panels) == cfg.expected["question_panels"]
    assert len(checklist.p_switches) == cfg.expected["p_switches"]
    assert checklist.map_width > 0 and checklist.map_height > 0


def test_every_mission_matches_mariowiki(checklist, cfg):
    names = [p["name"] for p in read_seed(SEED_DIR)["p_switches.json"]["items"]]
    assert len(match_missions(checklist.p_switches, names, cfg.checklist_mission_names)) == len(names)

"""Test del parser di seedgen/i18n.py su fixture SINTETICHE che riproducono la struttura reale.

Non sostituiscono tests/test_i18n_live.py: confermano che la logica (rowspan del gruppo "Italian",
note "Masculine"/"Feminine", nomi storici con nota vs nome attuale senza nota, sezioni che coprono
più nomi di outfit) funziona, non che la pagina reale sia fatta esattamente così — quel confronto
richiede la rete (le pagine vere sono intere biografie, troppo grandi per una fixture committata).
"""

from seedgen.i18n import OUTFIT_SECTIONS, extract_outfit_translations, extract_single_italian_name

FOREIGN_NAMES_HEAD = """
<table id="foreign-names" class="wikitable">
<tbody><tr><th>Language</th><th>Name</th><th>Meaning</th><th>Note(s)</th><th>Ref.</th></tr>
"""


def _table(*rows: str) -> str:
    return FOREIGN_NAMES_HEAD + "".join(rows) + "</tbody></table>"


def test_single_italian_row_with_no_history():
    # Caso Mario: una sola riga "Italian", nessuna nota.
    html = _table(
        '<tr><td><span class="nowrap">Japanese</span></td><td>x</td><td>x</td>'
        '<td><span class="no-foreign-note"></span></td><td></td></tr>',
        '<tr><td><span class="nowrap">Italian</span></td>'
        '<td dir="ltr"><span class="nowrap"><span lang="it">Mario</span></span></td>'
        '<td><span class="no-foreign-meaning">-</span></td>'
        '<td><span class="no-foreign-note"></span></td><td></td></tr>',
    )
    assert extract_single_italian_name(html) == "Mario"


def test_historical_names_have_a_note_the_current_one_does_not():
    # Caso Toad: 3 righe nel rowspan del gruppo "Italian", solo la prima (senza nota) è quella
    # attuale — le altre due sono varianti storiche con la nota che dice quale media le usava.
    html = _table(
        '<tr><td rowspan="3"><span class="nowrap">Italian</span></td>'
        '<td dir="ltr"><span class="nowrap"><span lang="it">Toad</span></span></td>'
        '<td rowspan="3"><span class="no-foreign-meaning">-</span></td>'
        '<td><span class="no-foreign-note"></span></td><td></td></tr>',
        '<tr><td dir="ltr"><span class="nowrap"><span lang="it">Suddito dei Funghi</span></span></td>'
        '<td><span class="foreign-note"><small>Super Mario Bros. 2</small></span></td><td></td></tr>',
        '<tr><td dir="ltr"><span class="nowrap"><span lang="it">Ughetto</span></span></td>'
        '<td><span class="foreign-note"><small>DIC cartoons</small></span></td><td></td></tr>',
    )
    assert extract_single_italian_name(html) == "Toad"


def test_missing_table_or_missing_language_returns_none():
    assert extract_single_italian_name("<p>no table here</p>") is None
    html = _table(
        '<tr><td><span class="nowrap">Japanese</span></td><td>x</td><td>x</td>'
        '<td><span class="no-foreign-note"></span></td><td></td></tr>',
    )
    assert extract_single_italian_name(html) is None


def _outfit_page(section_id: str, *italian_rows: str) -> str:
    return (
        f'<h2><span class="mw-headline" id="{section_id}">{section_id}</span></h2>\n'
        + _table(*italian_rows)
        + f'\n<h2><span class="mw-headline" id="Next">Next</span></h2>\n' + _table()
    )


def test_outfit_single_gender_neutral_value():
    html = _outfit_page(
        "Aero",
        '<tr><td><span class="nowrap">Italian</span></td>'
        '<td dir="ltr"><span class="nowrap"><span lang="it">Motociclista</span></span></td>'
        '<td><span class="foreign-meaning">Biker</span></td><td></td><td></td></tr>',
    )
    translations = extract_outfit_translations(html)
    assert translations["Aero"].for_gender("M") == "Motociclista"
    assert translations["Aero"].for_gender("F") == "Motociclista"
    assert translations["Aero"].for_gender(None) == "Motociclista"


def test_outfit_masculine_feminine_split():
    html = _outfit_page(
        "Explorer",
        '<tr><td rowspan="2"><span class="nowrap">Italian</span></td>'
        '<td dir="ltr"><span class="nowrap"><span lang="it">Esploratore</span></span></td>'
        '<td rowspan="2"><span class="foreign-meaning">Explorer</span></td>'
        '<td><span class="foreign-note"><small>Masculine</small></span></td><td></td></tr>',
        '<tr><td dir="ltr"><span class="nowrap"><span lang="it">Esploratrice</span></span></td>'
        '<td><span class="foreign-note"><small>Feminine</small></span></td><td></td></tr>',
    )
    translations = extract_outfit_translations(html)
    assert translations["Explorer"].for_gender("M") == "Esploratore"
    assert translations["Explorer"].for_gender("F") == "Esploratrice"
    # Nessun genere noto: si preferisce comunque non restituire None se una variante esiste.
    assert translations["Explorer"].for_gender(None) in {"Esploratore", "Esploratrice"}


def test_outfit_section_with_no_italian_row_is_absent():
    html = _outfit_page(
        "Aero",
        '<tr><td><span class="nowrap">Korean</span></td><td>x</td><td>x</td><td></td><td></td></tr>',
    )
    translations = extract_outfit_translations(html)
    assert translations.get("Aero") is None or translations["Aero"].for_gender(None) is None


def test_outfit_sections_registry_matches_seed_outfit_names():
    """OUTFIT_SECTIONS deve coprire ogni nome atteso, senza duplicati tra sezioni diverse."""
    all_names = [name for names in OUTFIT_SECTIONS.values() for name in names]
    assert len(all_names) == len(set(all_names)), "un nome di outfit compare in più di una sezione"
    assert "Biker" in OUTFIT_SECTIONS["Biker_.2F_Biker_Jr."]
    assert "Biker Jr." in OUTFIT_SECTIONS["Biker_.2F_Biker_Jr."]

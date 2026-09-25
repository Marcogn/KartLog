"""seedgen: estrae i dati di gioco di KartLog da Super Mario Wiki e genera i JSON in seed/.

Pipeline:  fetch (wiki.py) -> parse (parse.py) -> RawData (raw.py)
           -> build (build.py, normalizzazione con aliases.yaml)
           -> validate (validate.py, conteggi da expected_counts.yaml)
           -> output (output.py, JSON deterministici + diff)
"""

__version__ = "1.0.0"

"""Errori ed exit code. Il task Gradle `generateSeed` si basa su questi codici."""

EXIT_OK = 0             # dati validi e identici a seed/ (oppure scritti con successo)
EXIT_USAGE = 1          # argomenti errati, file di configurazione mancanti
EXIT_INVALID = 2        # parsing o validazione falliti: non usare questi dati
EXIT_CHANGED = 3        # dati validi ma diversi da seed/: serve revisione umana
EXIT_NETWORK = 4        # wiki irraggiungibile o timeout


class SeedgenError(Exception):
    exit_code = EXIT_USAGE


class ParseError(SeedgenError):
    """La pagina non ha la struttura attesa (tabella mancante, colonna rinominata, ...)."""

    exit_code = EXIT_INVALID


class UnknownNameError(ParseError):
    """Un personaggio o un corso letto dal wiki non è in aliases.yaml."""


class ValidationError(SeedgenError):
    exit_code = EXIT_INVALID

    def __init__(self, problems: list[str]):
        self.problems = problems
        super().__init__("Validazione fallita:\n  - " + "\n  - ".join(problems))


class NetworkError(SeedgenError):
    exit_code = EXIT_NETWORK

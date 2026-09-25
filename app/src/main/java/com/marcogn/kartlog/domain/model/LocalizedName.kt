package com.marcogn.kartlog.domain.model

import java.util.Locale

/**
 * Nome ufficiale di un elemento di gioco (personaggio, outfit, corso) nella lingua dell'app, se
 * esiste; altrimenti quello inglese di [name] (mai una traduzione automatica). [nameIt] arriva da
 * `seedgen/i18n.py`: `null` quando nessuna fonte ufficiale in italiano è nota per quell'elemento.
 */
fun localizedName(name: String, nameIt: String?, locale: Locale = Locale.getDefault()): String =
    if (locale.language == "it" && !nameIt.isNullOrBlank()) nameIt else name

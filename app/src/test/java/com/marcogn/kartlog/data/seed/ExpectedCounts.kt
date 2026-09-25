package com.marcogn.kartlog.data.seed

import java.io.File

/**
 * Legge solo le chiavi intere di `tools/seedgen/expected_counts.yaml` (SPEC §5.6): stessa fonte
 * di verità dello script Python, senza aggiungere una dipendenza YAML lato Android per un file
 * così semplice (`chiave: valore`, un valore per riga).
 */
object ExpectedCounts {
    private val INT_LINE = Regex("""^(\w+):\s*(\d+)\s*(?:#.*)?$""")

    private val values: Map<String, Int> by lazy {
        val file = File("../tools/seedgen/expected_counts.yaml")
        check(file.isFile) { "expected_counts.yaml non trovato in ${file.absolutePath}" }
        file.readLines()
            .mapNotNull { INT_LINE.find(it.trim()) }
            .associate { it.groupValues[1] to it.groupValues[2].toInt() }
    }

    operator fun get(key: String): Int = checkNotNull(values[key]) { "chiave assente in expected_counts.yaml: $key" }
}

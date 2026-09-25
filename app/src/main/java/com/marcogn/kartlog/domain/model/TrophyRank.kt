package com.marcogn.kartlog.domain.model

/**
 * Miglior risultato di un Gran Premio o Knockout Tour, in ordine crescente (SPEC §2.6). Le stelle
 * esistono solo con l'oro: "argento e 3 stelle" non è un risultato possibile, quindi stelle e
 * trofeo sono **una** scala, non due campi indipendenti. Fonte: Game Rant, "Mario Kart World: How
 * to Get a Three-Star Gold Trophy in Grand Prix" (bronzo, argento, oro, oro 1-2-3 stelle); per i
 * Knockout Tour TheGamer (1° oro, 2° argento, 3° bronzo, dal 4° nessun trofeo).
 *
 * Dal 4° posto in giù non c'è trofeo: non è un livello, equivale a "nessun risultato registrato".
 */
enum class TrophyRank(val level: Int) {
    BRONZE(1),
    SILVER(2),
    GOLD(3),
    GOLD_1_STAR(4),
    GOLD_2_STARS(5),
    GOLD_3_STARS(6),
    ;

    companion object {
        const val MAX_LEVEL = 6

        /**
         * Conversione dal vecchio modello (stelle 0–3 e posizione indipendenti, schema Room v2 e
         * backup v1): 1° -> oro con le sue stelle, 2° -> argento, 3° -> bronzo, altrimenti (4° o
         * oltre, eliminato in un KO) nessun trofeo. Le stelle registrate con un piazzamento diverso
         * dal 1° non esistono nel gioco e si ignorano. Stessa regola di [com.marcogn.kartlog.data.local.MIGRATION_2_3].
         */
        fun fromLegacy(placement: Int?, stars: Int): TrophyRank? = when (placement) {
            1 -> entries[GOLD.ordinal + stars.coerceIn(0, 3)]
            2 -> SILVER
            3 -> BRONZE
            else -> null
        }
    }
}

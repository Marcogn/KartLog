package com.marcogn.kartlog.ui.skin

import com.marcogn.kartlog.data.local.dao.CharacterProgress

/** Cosa succede toccando la card di un pilota. */
enum class CardTap { NONE, UNLOCK_POPUP, OUTFITS }

/**
 * Aspetto e tap della card di un pilota, in un posto solo (e testabile) invece che sparsi nella UI.
 * Regola dell'autore (27/09/2026): grigio = ti manca, colorato = ce l'hai.
 *
 * | pilota                         | bloccato                   | sbloccato                           |
 * |--------------------------------|----------------------------|-------------------------------------|
 * | con outfit                     | grigio, lucchetto, popup   | colorato, pagina degli outfit       |
 * | da sbloccare senza outfit      | grigio, lucchetto, popup   | colorato, popup (per ribloccarlo)   |
 * | di base senza outfit           | —                          | colorato, nessuna azione            |
 */
data class CardBehavior(val dimmed: Boolean, val showsLock: Boolean, val tap: CardTap)

fun cardBehavior(character: CharacterProgress): CardBehavior {
    val locked = !character.unlocked
    val tap = when {
        character.isUnlockable && (locked || !character.hasOutfits) -> CardTap.UNLOCK_POPUP
        character.hasOutfits -> CardTap.OUTFITS
        else -> CardTap.NONE
    }
    return CardBehavior(dimmed = locked, showsLock = locked, tap = tap)
}

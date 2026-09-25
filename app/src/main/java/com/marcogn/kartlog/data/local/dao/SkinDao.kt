package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Query di sola lettura per la schermata Skin (SPEC §2.3): incrociano dati seed e stato utente. */
@Dao
interface SkinDao {

    /**
     * Un personaggio per riga, con quanti dei suoi outfit sono posseduti. Solo i personaggi con
     * almeno un outfit alternativo (SPEC §2.3) — con i dati attuali sono tutti e 24.
     */
    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.nameIt AS nameIt, c.rosterOrder AS rosterOrder,
               COUNT(o.id) AS totalOutfits,
               SUM(CASE WHEN oo.outfitId IS NOT NULL THEN 1 ELSE 0 END) AS ownedOutfits
        FROM characters c
        JOIN outfits o ON o.characterId = c.id
        LEFT JOIN owned_outfits oo ON oo.outfitId = o.id
        GROUP BY c.id
        HAVING SUM(CASE WHEN o.isDefault = 0 THEN 1 ELSE 0 END) > 0
        ORDER BY c.rosterOrder ASC
        """
    )
    fun charactersWithProgress(): Flow<List<CharacterProgress>>

    @Query("SELECT * FROM characters WHERE id = :characterId")
    fun character(characterId: String): Flow<com.marcogn.kartlog.data.local.entity.CharacterEntity?>

    /**
     * Gli outfit di un personaggio con lo stato di possesso e i nomi dei gruppi di cibo che li
     * sbloccano, uniti con " · " (SPEC §2.3). `foodGroups` è null se non ci sono regole note
     * (mostrato in UI come "cibo sconosciuto").
     */
    @Query(
        """
        SELECT o.id AS outfitId, o.name AS outfitName, o.nameIt AS outfitNameIt, o.isDefault AS isDefault,
               (oo.outfitId IS NOT NULL) AS owned,
               (
                   SELECT GROUP_CONCAT(fg.name, ' · ')
                   FROM outfit_food_rules ofr
                   JOIN food_groups fg ON fg.id = ofr.foodGroupId
                   WHERE ofr.outfitId = o.id
               ) AS foodGroups
        FROM outfits o
        LEFT JOIN owned_outfits oo ON oo.outfitId = o.id
        WHERE o.characterId = :characterId
        ORDER BY o.isDefault DESC, o.name ASC
        """
    )
    fun outfitsForCharacter(characterId: String): Flow<List<OutfitProgress>>
}

data class CharacterProgress(
    val id: String,
    val name: String,
    val nameIt: String?,
    val rosterOrder: Int,
    val totalOutfits: Int,
    val ownedOutfits: Int,
) {
    val isComplete: Boolean get() = ownedOutfits >= totalOutfits
}

data class OutfitProgress(
    val outfitId: String,
    val outfitName: String?,
    val outfitNameIt: String?,
    val isDefault: Boolean,
    val owned: Boolean,
    val foodGroups: String?,
)

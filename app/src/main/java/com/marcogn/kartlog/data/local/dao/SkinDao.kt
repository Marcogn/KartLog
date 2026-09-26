package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.marcogn.kartlog.domain.model.localizedName
import kotlinx.coroutines.flow.Flow

/** Query di sola lettura per la schermata Personaggi (SPEC §2.3): incrociano dati seed e stato utente. */
@Dao
interface SkinDao {

    /**
     * Un pilota per riga, con quanti dei suoi outfit sono posseduti (0/0 per i piloti senza
     * outfit) e se è sbloccato: la scelta dell'utente se c'è, altrimenti `starter` dal seed.
     */
    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.nameIt AS nameIt, c.rosterOrder AS rosterOrder, c.imageUrl AS imageUrl,
               COALESCE(cu.unlocked, c.starter) AS unlocked,
               COUNT(o.id) AS totalOutfits,
               SUM(CASE WHEN oo.outfitId IS NOT NULL THEN 1 ELSE 0 END) AS ownedOutfits
        FROM characters c
        LEFT JOIN outfits o ON o.characterId = c.id
        LEFT JOIN owned_outfits oo ON oo.outfitId = o.id
        LEFT JOIN character_unlocks cu ON cu.characterId = c.id
        GROUP BY c.id
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
               o.imageUrl AS imageUrl,
               (oo.outfitId IS NOT NULL) AS owned,
               (
                   SELECT GROUP_CONCAT(fg.name, ' · ')
                   FROM outfit_food_rules ofr
                   JOIN food_groups fg ON fg.id = ofr.foodGroupId
                   WHERE ofr.outfitId = o.id
               ) AS foodGroups,
               (
                   SELECT GROUP_CONCAT(COALESCE(fg.nameIt, fg.name), ' · ')
                   FROM outfit_food_rules ofr
                   JOIN food_groups fg ON fg.id = ofr.foodGroupId
                   WHERE ofr.outfitId = o.id
               ) AS foodGroupsIt
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
    val imageUrl: String?,
    val unlocked: Boolean,
    val totalOutfits: Int,
    val ownedOutfits: Int,
) {
    /** Piloti senza outfit alternativi (Goomba, Mucca…): nessuna schermata di dettaglio. */
    val hasOutfits: Boolean get() = totalOutfits > 0

    /** Con outfit: tutti posseduti. Senza: basta averlo sbloccato, non c'è altro da raccogliere. */
    val isComplete: Boolean get() = if (hasOutfits) ownedOutfits >= totalOutfits else unlocked
}

data class OutfitProgress(
    val outfitId: String,
    val outfitName: String?,
    val outfitNameIt: String?,
    val isDefault: Boolean,
    val imageUrl: String?,
    val owned: Boolean,
    val foodGroups: String?,
    /** Stessa lista con i nomi italiani (traduzione NON ufficiale dei cibi, vedi il seed). */
    val foodGroupsIt: String?,
) {
    val localizedFoodGroups: String? get() = foodGroups?.let { localizedName(it, foodGroupsIt) }
}

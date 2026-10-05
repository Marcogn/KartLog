package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.marcogn.kartlog.data.local.entity.FoodVariantEntity
import kotlinx.coroutines.flow.Flow

/**
 * Dati della sezione Cibi (fase C2, preparati in C1): i gruppi di cibo con le loro varianti, gli
 * stand Yoshi's dove si trovano (percorsi e strade) e gli outfit che sbloccano. Solo `Flow` grezzi:
 * la schermata li combina nel suo ViewModel. I testi `*It` di cibi, luoghi e locali sono traduzioni
 * NON ufficiali (tools/seedgen/manual/).
 */
@Dao
interface FoodDao {

    /**
     * Un gruppo per tessera della griglia, nell'ordine del seed: immagine della prima variante e
     * quanti outfit dà (`totalOutfits`) e quanti di questi non hai (`missingOutfits`).
     */
    @Query(
        """
        SELECT g.id AS id, g.name AS name, g.nameIt AS nameIt, g.revertsToDefault AS revertsToDefault,
               (SELECT v.imageUrl FROM food_variants v WHERE v.foodGroupId = g.id ORDER BY v.`order` LIMIT 1) AS imageUrl,
               (SELECT COUNT(*) FROM outfit_food_rules r WHERE r.foodGroupId = g.id) AS totalOutfits,
               (SELECT COUNT(*) FROM outfit_food_rules r
                    LEFT JOIN owned_outfits oo ON oo.outfitId = r.outfitId
                    WHERE r.foodGroupId = g.id AND oo.outfitId IS NULL) AS missingOutfits
        FROM food_groups g
        ORDER BY g.rowid
        """
    )
    fun foodGroups(): Flow<List<FoodGroupSummaryRow>>

    /** Le varianti di un gruppo nell'ordine della tabella di Dash Food (piccola, media, grande…). */
    @Query("SELECT * FROM food_variants WHERE foodGroupId = :foodGroupId ORDER BY `order`")
    fun variants(foodGroupId: String): Flow<List<FoodVariantEntity>>

    /**
     * Gli stand che hanno il gruppo, nell'ordine della pagina del wiki (prima i percorsi, poi le
     * strade). `courseId` null = strada; `regionId` null = bioma non indicato dalla fonte (strade).
     * `food` non null solo dove la fonte nomina il cibo preciso (gruppo sushi).
     */
    @Query(
        """
        SELECT s.id AS standId, s.courseId AS courseId, c.name AS courseName, c.nameIt AS courseNameIt,
               s.regionId AS regionId, r.name AS regionName, r.nameIt AS regionNameIt, r.`order` AS regionOrder,
               s.establishment AS establishment, s.establishmentIt AS establishmentIt,
               s.location AS location, s.locationIt AS locationIt, f.food AS food
        FROM yoshi_stand_foods f
        JOIN yoshi_stands s ON s.id = f.standId
        LEFT JOIN courses c ON c.id = s.courseId
        LEFT JOIN regions r ON r.id = s.regionId
        WHERE f.foodGroupId = :foodGroupId
        ORDER BY s.`order`
        """
    )
    fun stands(foodGroupId: String): Flow<List<FoodStandRow>>

    /** Gli outfit che il gruppo sblocca, per personaggio (ordine di roster), con lo stato posseduto. */
    @Query(
        """
        SELECT o.id AS outfitId, o.name AS name, o.nameIt AS nameIt, o.imageUrl AS imageUrl,
               ch.id AS characterId, ch.name AS characterName, ch.nameIt AS characterNameIt,
               ch.imageUrl AS characterImageUrl, ch.rosterOrder AS rosterOrder,
               (oo.outfitId IS NOT NULL) AS owned
        FROM outfit_food_rules r
        JOIN outfits o ON o.id = r.outfitId
        JOIN characters ch ON ch.id = o.characterId
        LEFT JOIN owned_outfits oo ON oo.outfitId = o.id
        WHERE r.foodGroupId = :foodGroupId
        ORDER BY ch.rosterOrder, o.id
        """
    )
    fun outfits(foodGroupId: String): Flow<List<FoodOutfitRow>>
}

data class FoodGroupSummaryRow(
    val id: String,
    val name: String,
    val nameIt: String?,
    val revertsToDefault: Boolean,
    val imageUrl: String?,
    val totalOutfits: Int,
    val missingOutfits: Int,
)

data class FoodStandRow(
    val standId: String,
    val courseId: String?,
    val courseName: String?,
    val courseNameIt: String?,
    val regionId: String?,
    val regionName: String?,
    val regionNameIt: String?,
    val regionOrder: Int?,
    val establishment: String?,
    val establishmentIt: String?,
    val location: String?,
    val locationIt: String?,
    val food: String?,
)

data class FoodOutfitRow(
    val outfitId: String,
    val name: String?,
    val nameIt: String?,
    val imageUrl: String?,
    val characterId: String,
    val characterName: String,
    val characterNameIt: String?,
    val characterImageUrl: String?,
    val rosterOrder: Int,
    val owned: Boolean,
)

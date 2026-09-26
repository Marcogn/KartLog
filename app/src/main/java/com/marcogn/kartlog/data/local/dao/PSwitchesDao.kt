package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.marcogn.kartlog.domain.model.localizedName
import kotlinx.coroutines.flow.Flow

/** Query di sola lettura per la schermata Pulsanti P (SPEC §2.4). */
@Dao
interface PSwitchesDao {

    // locationSortKey serve solo all'ORDER BY, non alla riga risultante (locationName è calcolato
    // in Kotlin, con localizedName): Room la scarterebbe comunque, l'annotazione toglie solo l'avviso.
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT p.id AS pSwitchId, p."index" AS "index", p.name AS name,
               (cp.pSwitchId IS NOT NULL) AS completed,
               r.id AS regionId, r.name AS regionName, r.nameIt AS regionNameIt, r."order" AS regionOrder,
               c.name AS courseName, c.nameIt AS courseNameIt, a.name AS areaName,
               COALESCE(c.name, a.name) AS locationSortKey
        FROM p_switches p
        JOIN regions r ON r.id = p.regionId
        LEFT JOIN courses c ON c.id = p.courseId
        LEFT JOIN areas a ON a.id = p.areaId
        LEFT JOIN completed_p_switches cp ON cp.pSwitchId = p.id
        ORDER BY r."order" ASC, locationSortKey ASC, p."index" ASC
        """
    )
    fun allPSwitches(): Flow<List<PSwitchWithLocation>>
}

data class PSwitchWithLocation(
    val pSwitchId: String,
    val index: Int,
    val name: String,
    val completed: Boolean,
    val regionId: String,
    val regionName: String,
    val regionNameIt: String?,
    val regionOrder: Int,
    val courseName: String?,
    val courseNameIt: String?,
    val areaName: String?,
) {
    /** Nome del corso in italiano se noto (seedgen/i18n.py), altrimenti quello del luogo (inglese). */
    val locationName: String
        get() = courseName?.let { localizedName(it, courseNameIt) } ?: areaName.orEmpty()

    /** Nome del bioma in italiano se noto (mariowiki.it, seedgen/it_wiki.py). */
    val localizedRegionName: String
        get() = localizedName(regionName, regionNameIt)
}

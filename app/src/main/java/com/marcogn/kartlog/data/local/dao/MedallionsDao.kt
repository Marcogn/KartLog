package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Query di sola lettura per la schermata Monete Peach (SPEC §2.4). */
@Dao
interface MedallionsDao {

    @Query(
        """
        SELECT m.id AS medallionId, m."index" AS "index", (cm.medallionId IS NOT NULL) AS collected,
               r.id AS regionId, r.name AS regionName, r."order" AS regionOrder
        FROM peach_medallions m
        JOIN regions r ON r.id = m.regionId
        LEFT JOIN collected_medallions cm ON cm.medallionId = m.id
        ORDER BY r."order" ASC, m."index" ASC
        """
    )
    fun allMedallions(): Flow<List<MedallionWithRegion>>

    /**
     * Un contatore per bioma (SPEC §2.4): i medaglioni non hanno dettagli propri (fonte manuale con i
     * soli conteggi per regione), quindi le righe di `peach_medallions` sono "posti" anonimi.
     */
    @Query(
        """
        SELECT r.id AS regionId, r.name AS regionName, r.nameIt AS regionNameIt, r."order" AS regionOrder,
               COUNT(m.id) AS total, COUNT(cm.medallionId) AS collected
        FROM regions r
        JOIN peach_medallions m ON m.regionId = r.id
        LEFT JOIN collected_medallions cm ON cm.medallionId = m.id
        GROUP BY r.id
        ORDER BY r."order" ASC
        """
    )
    fun regionCounters(): Flow<List<RegionMedallionCount>>
}

data class RegionMedallionCount(
    val regionId: String,
    val regionName: String,
    val regionNameIt: String?,
    val regionOrder: Int,
    val total: Int,
    val collected: Int,
)

data class MedallionWithRegion(
    val medallionId: String,
    val index: Int,
    val collected: Boolean,
    val regionId: String,
    val regionName: String,
    val regionOrder: Int,
)

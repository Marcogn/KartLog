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
}

data class MedallionWithRegion(
    val medallionId: String,
    val index: Int,
    val collected: Boolean,
    val regionId: String,
    val regionName: String,
    val regionOrder: Int,
)

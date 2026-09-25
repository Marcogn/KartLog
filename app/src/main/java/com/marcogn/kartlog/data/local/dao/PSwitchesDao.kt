package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Query di sola lettura per la schermata Pulsanti P (SPEC §2.4). */
@Dao
interface PSwitchesDao {

    @Query(
        """
        SELECT p.id AS pSwitchId, p."index" AS "index", p.name AS name,
               (cp.pSwitchId IS NOT NULL) AS completed,
               r.id AS regionId, r.name AS regionName, r."order" AS regionOrder,
               COALESCE(c.name, a.name) AS locationName
        FROM p_switches p
        JOIN regions r ON r.id = p.regionId
        LEFT JOIN courses c ON c.id = p.courseId
        LEFT JOIN areas a ON a.id = p.areaId
        LEFT JOIN completed_p_switches cp ON cp.pSwitchId = p.id
        ORDER BY r."order" ASC, locationName ASC, p."index" ASC
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
    val regionOrder: Int,
    val locationName: String,
)

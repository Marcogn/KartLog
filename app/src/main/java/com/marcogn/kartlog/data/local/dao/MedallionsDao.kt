package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Query di sola lettura per la schermata Monete Peach (SPEC §2.4). */
@Dao
interface MedallionsDao {

    /** Le 200 monete di mkworld-checklist, ognuna con le sue istruzioni e il suo stato. */
    @Query(
        """
        SELECT m.id AS medallionId, m."index" AS "index", m.hint AS hint, m.youtubeId AS youtubeId,
               (cm.medallionId IS NOT NULL) AS collected
        FROM peach_medallions m
        LEFT JOIN collected_medallions cm ON cm.medallionId = m.id
        ORDER BY m."index" ASC
        """
    )
    fun allMedallions(): Flow<List<MedallionRow>>
}

data class MedallionRow(
    val medallionId: String,
    val index: Int,
    val hint: String?,
    val youtubeId: String?,
    val collected: Boolean,
)

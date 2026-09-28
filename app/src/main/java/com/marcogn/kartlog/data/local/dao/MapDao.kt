package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.marcogn.kartlog.domain.model.localizedName
import kotlinx.coroutines.flow.Flow

/**
 * Tutti i punti della mappa (Monete Peach, Pulsanti P, pannelli "?") con il loro stato, in un solo
 * `Flow` come le altre schermate: una query con UNION invece di tre separate.
 */
@Dao
interface MapDao {

    @Query(
        """
        SELECT 'MEDALLION' AS type, m.id AS id, m."index" AS "index", m.x AS x, m.y AS y, m.hint AS hint,
               m.youtubeId AS youtubeId, NULL AS name, NULL AS courseName, NULL AS courseNameIt, NULL AS areaName,
               (cm.medallionId IS NOT NULL) AS done
        FROM peach_medallions m
        LEFT JOIN collected_medallions cm ON cm.medallionId = m.id
        UNION ALL
        SELECT 'P_SWITCH', p.id, p."index", p.x, p.y, p.hint, p.youtubeId, p.name, c.name, c.nameIt, a.name,
               (cp.pSwitchId IS NOT NULL)
        FROM p_switches p
        LEFT JOIN courses c ON c.id = p.courseId
        LEFT JOIN areas a ON a.id = p.areaId
        LEFT JOIN completed_p_switches cp ON cp.pSwitchId = p.id
        WHERE p.x IS NOT NULL AND p.y IS NOT NULL
        UNION ALL
        SELECT 'QUESTION_PANEL', q.id, q."index", q.x, q.y, q.hint, q.youtubeId, NULL, NULL, NULL, NULL,
               (aq.panelId IS NOT NULL)
        FROM question_panels q
        LEFT JOIN activated_question_panels aq ON aq.panelId = q.id
        """
    )
    fun allPoints(): Flow<List<MapPointRow>>
}

data class MapPointRow(
    /** Nome di [com.marcogn.kartlog.domain.model.MapPointType]. */
    val type: String,
    val id: String,
    val index: Int,
    val x: Double,
    val y: Double,
    val hint: String?,
    val youtubeId: String?,
    /** Solo Pulsanti P: nome della missione (inglese) e luogo. */
    val name: String?,
    val courseName: String?,
    val courseNameIt: String?,
    val areaName: String?,
    val done: Boolean,
) {
    val locationName: String?
        get() = courseName?.let { localizedName(it, courseNameIt) } ?: areaName
}

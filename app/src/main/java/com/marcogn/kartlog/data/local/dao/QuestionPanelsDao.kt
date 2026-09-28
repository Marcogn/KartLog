package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Query di sola lettura per la schermata Pannelli "?", stesso schema di [MedallionsDao]. */
@Dao
interface QuestionPanelsDao {

    /** I 150 pannelli di mkworld-checklist, ognuno con le sue istruzioni e il suo stato. */
    @Query(
        """
        SELECT q.id AS panelId, q."index" AS "index", q.hint AS hint, q.youtubeId AS youtubeId,
               (aq.panelId IS NOT NULL) AS activated
        FROM question_panels q
        LEFT JOIN activated_question_panels aq ON aq.panelId = q.id
        ORDER BY q."index" ASC
        """
    )
    fun allPanels(): Flow<List<QuestionPanelRow>>
}

data class QuestionPanelRow(
    val panelId: String,
    val index: Int,
    val hint: String?,
    val youtubeId: String?,
    val activated: Boolean,
)

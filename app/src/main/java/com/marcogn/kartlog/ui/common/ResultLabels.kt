package com.marcogn.kartlog.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.TrophyRank

// Etichette condivise da Risultati e Consigliami (SPEC §2.5, §2.6).

@Composable
fun ccLabel(cc: Cc): String = when (cc) {
    Cc.CC_50 -> stringResource(R.string.consigliami_cc_50)
    Cc.CC_100 -> stringResource(R.string.consigliami_cc_100)
    Cc.CC_150 -> stringResource(R.string.consigliami_cc_150)
    Cc.MIRROR -> stringResource(R.string.consigliami_cc_mirror)
}

@Composable
fun rankLabel(rank: TrophyRank): String = stringResource(
    when (rank) {
        TrophyRank.BRONZE -> R.string.rank_bronze
        TrophyRank.SILVER -> R.string.rank_silver
        TrophyRank.GOLD -> R.string.rank_gold
        TrophyRank.GOLD_1_STAR -> R.string.rank_gold_1_star
        TrophyRank.GOLD_2_STARS -> R.string.rank_gold_2_stars
        TrophyRank.GOLD_3_STARS -> R.string.rank_gold_3_stars
    }
)

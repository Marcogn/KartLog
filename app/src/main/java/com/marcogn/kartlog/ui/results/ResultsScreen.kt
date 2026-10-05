package com.marcogn.kartlog.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.TrophyRank
import com.marcogn.kartlog.ui.common.EventIcon
import com.marcogn.kartlog.ui.common.KartChoiceButton
import com.marcogn.kartlog.ui.common.KartColors
import com.marcogn.kartlog.ui.common.KartCounterPill
import com.marcogn.kartlog.ui.common.KartInfoButton
import com.marcogn.kartlog.ui.common.KartPanel
import com.marcogn.kartlog.ui.common.KartPopup
import com.marcogn.kartlog.ui.common.KartPopupText
import com.marcogn.kartlog.ui.common.KartTabs
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.ccLabel
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.common.rankLabel
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

@Composable
fun ResultsScreen(
    onMenuClick: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ResultsContent(
        state = state,
        onMenuClick = onMenuClick,
        onCcChanged = viewModel::onCcChanged,
        onRankChanged = viewModel::onRankChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultsContent(
    state: ResultsUiState,
    onMenuClick: () -> Unit,
    onCcChanged: (Cc) -> Unit,
    onRankChanged: (String, TrophyRank?) -> Unit,
) {
    var editingEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var showMirrorInfo by rememberSaveable { mutableStateOf(false) }

    if (showMirrorInfo) {
        KartPopup(title = stringResource(R.string.results_mirror_info_title), onDismiss = { showMirrorInfo = false }) {
            state.mirrorSteps.forEachIndexed { i, step ->
                KartPopupText("${i + 1}. $step", Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
        }
    }

    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme(), faded = true),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(stringResource(R.string.results_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        KartTabs(
            options = Cc.entries,
            selected = state.cc,
            label = { ccLabel(it) },
            onSelected = onCcChanged,
            tabTrailing = { cc ->
                if (cc == Cc.MIRROR && state.mirrorSteps.isNotEmpty()) {
                    KartInfoButton(stringResource(R.string.results_mirror_info_cd), onClick = { showMirrorInfo = true })
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                resultsSection(R.string.results_section_cup, state.cups, KartTiles.Orange, onClick = { editingEventId = it })
                resultsSection(R.string.results_section_rally, state.rallies, KartTiles.Green, onClick = { editingEventId = it })
            }
        }
    }

    val editing = editingEventId?.let { id -> (state.cups + state.rallies).firstOrNull { it.eventId == id } }
    if (editing != null) {
        RankPickerPopup(
            title = "${editing.eventName} · ${ccLabel(state.cc)}",
            selected = editing.rank,
            onSelected = { rank ->
                onRankChanged(editing.eventId, rank)
                editingEventId = null
            },
            onDismiss = { editingEventId = null },
        )
    }
}

private fun LazyListScope.resultsSection(
    titleRes: Int,
    rows: List<ResultRow>,
    colors: KartColors,
    onClick: (String) -> Unit,
) {
    item(key = "header_$titleRes") {
        OutlinedTitle(
            stringResource(titleRes),
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 10.dp),
            fontSize = 24.sp,
            textAlign = TextAlign.Start,
        )
    }
    items(rows, key = { it.eventId }) { row ->
        // Grigio = nessun trofeo, giallo = registrato (stessa regola del resto dell'app).
        KartPanel(colors = colors, modifier = Modifier.fillMaxWidth(), onClick = { onClick(row.eventId) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EventIcon(name = row.eventName, imageUrl = row.imageUrl, outlined = true)
                OutlinedTitle(row.eventName, Modifier.weight(1f), fontSize = 19.sp, textAlign = TextAlign.Start, maxLines = 2)
                KartCounterPill(
                    text = row.rank?.let { rankLabel(it) } ?: stringResource(R.string.results_no_trophy),
                    colors = if (row.rank == null) KartTiles.Gray else KartTiles.Yellow,
                )
            }
        }
    }
}

/** Un solo valore per (evento, cilindrata): scegliere un trofeo sostituisce quello registrato (SPEC §2.6). */
@Composable
private fun RankPickerPopup(
    title: String,
    selected: TrophyRank?,
    onSelected: (TrophyRank?) -> Unit,
    onDismiss: () -> Unit,
) {
    KartPopup(title = title, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KartChoiceButton(
                text = stringResource(R.string.results_no_trophy),
                selected = selected == null,
                onClick = { onSelected(null) },
                modifier = Modifier.fillMaxWidth(),
            )
            TrophyRank.entries.forEach { rank ->
                KartChoiceButton(
                    text = rankLabel(rank),
                    selected = selected == rank,
                    onClick = { onSelected(rank) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

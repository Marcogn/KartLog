package com.marcogn.kartlog.ui.results

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.TrophyRank
import com.marcogn.kartlog.ui.common.ccLabel
import com.marcogn.kartlog.ui.common.rankLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onMenuClick: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var editingEventId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.results_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.results_cc_label), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Cc.entries.forEach { cc ->
                            FilterChip(
                                selected = state.cc == cc,
                                onClick = { viewModel.onCcChanged(cc) },
                                label = { Text(ccLabel(cc)) },
                            )
                        }
                    }
                }
            }
            resultsSection(R.string.results_section_cup, state.cups, onClick = { editingEventId = it })
            resultsSection(R.string.results_section_rally, state.rallies, onClick = { editingEventId = it })
        }
    }

    val editing = editingEventId?.let { id -> (state.cups + state.rallies).firstOrNull { it.eventId == id } }
    if (editing != null) {
        RankPickerDialog(
            title = "${editing.eventName} · ${ccLabel(state.cc)}",
            selected = editing.rank,
            onSelected = { rank ->
                viewModel.onRankChanged(editing.eventId, rank)
                editingEventId = null
            },
            onDismiss = { editingEventId = null },
        )
    }
}

private fun LazyListScope.resultsSection(
    titleRes: Int,
    rows: List<ResultRow>,
    onClick: (String) -> Unit,
) {
    item(key = "header_$titleRes") {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        HorizontalDivider()
    }
    items(rows, key = { it.eventId }) { row ->
        ListItem(
            headlineContent = { Text(row.eventName) },
            trailingContent = {
                Text(
                    text = row.rank?.let { rankLabel(it) } ?: stringResource(R.string.results_no_trophy),
                    color = if (row.rank == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
            },
            modifier = Modifier.clickable { onClick(row.eventId) },
        )
    }
}

/** Un solo valore per (evento, cilindrata): scegliere un trofeo sostituisce quello registrato (SPEC §2.6). */
@Composable
private fun RankPickerDialog(
    title: String,
    selected: TrophyRank?,
    onSelected: (TrophyRank?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                RankOption(stringResource(R.string.results_dialog_clear), selected == null) { onSelected(null) }
                TrophyRank.entries.forEach { rank ->
                    RankOption(rankLabel(rank), selected == rank) { onSelected(rank) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.results_dialog_cancel)) } },
    )
}

@Composable
private fun RankOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

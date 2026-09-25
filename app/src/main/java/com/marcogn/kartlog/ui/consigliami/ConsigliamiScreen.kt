package com.marcogn.kartlog.ui.consigliami

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.entity.RaceResultEntity
import com.marcogn.kartlog.domain.consigliami.CharacterGain
import com.marcogn.kartlog.domain.consigliami.EventScore
import com.marcogn.kartlog.domain.consigliami.RecommendationGroup
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.Presence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsigliamiScreen(
    onMenuClick: () -> Unit,
    onEventClick: (eventId: String, includeNearby: Boolean) -> Unit,
    viewModel: ConsigliamiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.consigliami_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Text(
                    text = stringResource(R.string.consigliami_banner),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.eventFilter == ConsigliamiEventFilter.CUP,
                    onClick = { viewModel.onEventFilterChanged(ConsigliamiEventFilter.CUP) },
                    label = { Text(stringResource(R.string.consigliami_filter_cup)) },
                )
                FilterChip(
                    selected = state.eventFilter == ConsigliamiEventFilter.RALLY,
                    onClick = { viewModel.onEventFilterChanged(ConsigliamiEventFilter.RALLY) },
                    label = { Text(stringResource(R.string.consigliami_filter_rally)) },
                )
                FilterChip(
                    selected = state.eventFilter == ConsigliamiEventFilter.BOTH,
                    onClick = { viewModel.onEventFilterChanged(ConsigliamiEventFilter.BOTH) },
                    label = { Text(stringResource(R.string.consigliami_filter_both)) },
                )
            }
            SwitchRow(
                label = stringResource(R.string.consigliami_include_nearby),
                checked = state.includeNearby,
                onCheckedChange = viewModel::onIncludeNearbyChanged,
            )
            SwitchRow(
                label = stringResource(R.string.consigliami_only_useful),
                checked = state.onlyUseful,
                onCheckedChange = viewModel::onOnlyUsefulChanged,
            )
            SwitchRow(
                label = stringResource(R.string.consigliami_weigh_results),
                checked = state.resultsEnabled,
                onCheckedChange = viewModel::onResultsEnabledChanged,
            )
            if (state.resultsEnabled) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        stringResource(R.string.consigliami_weight_label, (state.weight * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Slider(
                        value = state.weight.toFloat(),
                        onValueChange = { viewModel.onWeightChanged(it.toDouble()) },
                        valueRange = 0f..1f,
                    )
                    Text(stringResource(R.string.consigliami_reference_cc), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        Cc.entries.forEach { cc ->
                            FilterChip(
                                selected = state.referenceCc == cc,
                                onClick = { viewModel.onReferenceCcChanged(cc) },
                                label = { Text(ccLabel(cc)) },
                            )
                        }
                    }
                }
            }

            if (state.groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.consigliami_no_recommendations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(state.groups, key = { it.position }) { group ->
                        GroupSection(
                            group = group,
                            characterNames = state.characterNames,
                            courseNames = state.courseNames,
                            foodGroupNames = state.foodGroupNames,
                            bestResultByEvent = if (state.resultsEnabled) state.bestResultByEvent else emptyMap(),
                            onEventClick = { eventId -> onEventClick(eventId, state.includeNearby) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GroupSection(
    group: RecommendationGroup,
    characterNames: Map<String, String>,
    courseNames: Map<String, String>,
    foodGroupNames: Map<String, String>,
    bestResultByEvent: Map<String, RaceResultEntity>,
    onEventClick: (String) -> Unit,
) {
    var expanded by rememberSaveable(group.position) { mutableStateOf(group.events.size <= 1) }

    if (group.events.size > 1) {
        val title = if (group.commonCourseIds.isNotEmpty()) {
            val names = group.commonCourseIds.mapNotNull { courseNames[it] }.sorted().joinToString(", ")
            stringResource(R.string.consigliami_group_title_common, group.events.size, names)
        } else {
            stringResource(R.string.consigliami_group_title_generic, group.events.size)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { expanded = !expanded }) {
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
        }
        if (expanded) {
            group.events.forEach { event ->
                EventCard(event, group.position, characterNames, courseNames, foodGroupNames, bestResultByEvent[event.event.id], onClick = { onEventClick(event.event.id) })
            }
        }
    } else {
        val event = group.events.single()
        EventCard(event, group.position, characterNames, courseNames, foodGroupNames, bestResultByEvent[event.event.id], onClick = { onEventClick(event.event.id) })
    }
}

@Composable
private fun EventCard(
    eventScore: EventScore,
    position: Int,
    characterNames: Map<String, String>,
    courseNames: Map<String, String>,
    foodGroupNames: Map<String, String>,
    bestResult: RaceResultEntity?,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(eventScore.event.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(if (eventScore.event.type == EventType.CUP) R.string.consigliami_type_cup else R.string.consigliami_type_rally),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.consigliami_position_format, position),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (bestResult != null) {
                val placementText = bestResult.placement?.let { stringResource(R.string.consigliami_history_placement_format, it) }
                    ?: bestResult.eliminatedAt?.let { stringResource(R.string.consigliami_history_eliminated_format, it) }
                    ?: ""
                Text(
                    stringResource(R.string.consigliami_best_result_format, bestResult.stars, placementText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val best = eventScore.best
            if (best != null) {
                Text(
                    stringResource(R.string.consigliami_recommended_character),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                CharacterGainRow(best, characterNames, emphasized = true)
                if (eventScore.runnersUp.isNotEmpty()) {
                    Text(
                        stringResource(R.string.consigliami_runners_up),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    eventScore.runnersUp.forEach { CharacterGainRow(it, characterNames, emphasized = false) }
                }
                if (eventScore.relevantFoods.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        eventScore.relevantFoods.forEach { food ->
                            val presenceLabel = stringResource(
                                if (food.presence == Presence.ON_COURSE) R.string.consigliami_on_course else R.string.consigliami_nearby
                            )
                            val foodName = foodGroupNames[food.foodGroupId] ?: food.foodGroupId
                            val courseName = courseNames[food.courseId] ?: food.courseId
                            Text("$foodName · $courseName ($presenceLabel)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterGainRow(gain: CharacterGain, characterNames: Map<String, String>, emphasized: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = characterNames[gain.characterId] ?: gain.characterId,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.consigliami_gain_format, gain.gain),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

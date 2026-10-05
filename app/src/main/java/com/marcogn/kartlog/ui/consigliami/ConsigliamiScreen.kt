package com.marcogn.kartlog.ui.consigliami

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.consigliami.CharacterGain
import com.marcogn.kartlog.domain.consigliami.EventScore
import com.marcogn.kartlog.domain.consigliami.RecommendationGroup
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.TrophyRank
import com.marcogn.kartlog.ui.common.CharacterAvatar
import com.marcogn.kartlog.ui.common.EventIcon
import com.marcogn.kartlog.ui.common.KartChoiceButton
import com.marcogn.kartlog.ui.common.KartCounterPill
import com.marcogn.kartlog.ui.common.KartInfoButton
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.common.KartPanel
import com.marcogn.kartlog.ui.common.KartPopup
import com.marcogn.kartlog.ui.common.KartPopupText
import com.marcogn.kartlog.ui.common.KartSwitchRow
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.ccLabel
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.common.rankLabel
import com.marcogn.kartlog.ui.theme.KartFont
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

internal val ConsigliamiBodyStyle = TextStyle(fontFamily = KartFont, fontSize = 15.sp, lineHeight = 19.sp, color = Color.White)

@Composable
fun ConsigliamiScreen(
    onMenuClick: () -> Unit,
    onEventClick: (eventId: String) -> Unit,
    viewModel: ConsigliamiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ConsigliamiContent(
        state = state,
        onMenuClick = onMenuClick,
        onEventClick = onEventClick,
        onEventFilterChanged = viewModel::onEventFilterChanged,
        onOnlyUsefulChanged = viewModel::onOnlyUsefulChanged,
        onResultsEnabledChanged = viewModel::onResultsEnabledChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onReferenceCcChanged = viewModel::onReferenceCcChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConsigliamiContent(
    state: ConsigliamiUiState,
    onMenuClick: () -> Unit,
    onEventClick: (eventId: String) -> Unit,
    onEventFilterChanged: (ConsigliamiEventFilter) -> Unit,
    onOnlyUsefulChanged: (Boolean) -> Unit,
    onResultsEnabledChanged: (Boolean) -> Unit,
    onWeightChanged: (Double) -> Unit,
    onReferenceCcChanged: (Cc) -> Unit,
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(stringResource(R.string.consigliami_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "controls") {
                ControlsPanel(
                    state = state,
                    onEventFilterChanged = onEventFilterChanged,
                    onOnlyUsefulChanged = onOnlyUsefulChanged,
                    onResultsEnabledChanged = onResultsEnabledChanged,
                    onWeightChanged = onWeightChanged,
                    onReferenceCcChanged = onReferenceCcChanged,
                    onInfoClick = { showInfo = true },
                )
            }
            if (state.groups.isEmpty()) {
                item(key = "empty") {
                    OutlinedTitle(
                        stringResource(R.string.consigliami_no_recommendations),
                        fontSize = 19.sp,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            } else {
                items(state.groups, key = { it.position }) { group ->
                    GroupSection(
                        group = group,
                        characterNames = state.characterNames,
                        characterImages = state.characterImages,
                        eventImages = state.eventImages,
                        courseNames = state.courseNames,
                        foodGroupNames = state.foodGroupNames,
                        bestRankByEvent = if (state.resultsEnabled) state.bestRankByEvent else emptyMap(),
                        referenceCc = state.referenceCc,
                        onEventClick = onEventClick,
                    )
                }
            }
        }
    }

    if (showInfo) {
        KartPopup(title = stringResource(R.string.consigliami_info_title), onDismiss = { showInfo = false }) {
            KartPopupText(stringResource(R.string.consigliami_banner))
        }
    }
}

/** Filtri, switch e peso dei risultati in un'unica tessera blu; la "i" apre l'avviso sugli stand. */
@Composable
private fun ControlsPanel(
    state: ConsigliamiUiState,
    onEventFilterChanged: (ConsigliamiEventFilter) -> Unit,
    onOnlyUsefulChanged: (Boolean) -> Unit,
    onResultsEnabledChanged: (Boolean) -> Unit,
    onWeightChanged: (Double) -> Unit,
    onReferenceCcChanged: (Cc) -> Unit,
    onInfoClick: () -> Unit,
) {
    KartPanel(colors = KartTiles.Blue, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConsigliamiEventFilter.entries.forEach { filter ->
                        KartChoiceButton(
                            text = stringResource(
                                when (filter) {
                                    ConsigliamiEventFilter.CUP -> R.string.consigliami_filter_cup
                                    ConsigliamiEventFilter.RALLY -> R.string.consigliami_filter_rally
                                    ConsigliamiEventFilter.BOTH -> R.string.consigliami_filter_both
                                },
                            ),
                            selected = state.eventFilter == filter,
                            onClick = { onEventFilterChanged(filter) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                KartInfoButton(stringResource(R.string.consigliami_info_cd), onClick = onInfoClick)
            }
            KartSwitchRow(
                label = stringResource(R.string.consigliami_only_useful),
                checked = state.onlyUseful,
                onCheckedChange = onOnlyUsefulChanged,
                modifier = Modifier.fillMaxWidth(),
            )
            KartSwitchRow(
                label = stringResource(R.string.consigliami_weigh_results),
                checked = state.resultsEnabled,
                onCheckedChange = onResultsEnabledChanged,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.resultsEnabled) {
                OutlinedTitle(
                    stringResource(R.string.consigliami_weight_label, (state.weight * 100).toInt()),
                    fontSize = 17.sp,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                )
                Slider(
                    value = state.weight.toFloat(),
                    onValueChange = { onWeightChanged(it.toDouble()) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = KartTiles.Green.base,
                        inactiveTrackColor = KartTiles.Blue.dark,
                    ),
                )
                OutlinedTitle(
                    stringResource(R.string.consigliami_reference_cc),
                    fontSize = 17.sp,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Cc.entries.forEach { cc ->
                        KartChoiceButton(
                            text = ccLabel(cc),
                            selected = state.referenceCc == cc,
                            onClick = { onReferenceCcChanged(cc) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupSection(
    group: RecommendationGroup,
    characterNames: Map<String, String>,
    characterImages: Map<String, String>,
    eventImages: Map<String, String>,
    courseNames: Map<String, String>,
    foodGroupNames: Map<String, String>,
    bestRankByEvent: Map<String, TrophyRank>,
    referenceCc: Cc,
    onEventClick: (String) -> Unit,
) {
    var expanded by rememberSaveable(group.position) { mutableStateOf(group.events.size <= 1) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (group.events.size > 1) {
            val title = if (group.commonCourseIds.isNotEmpty()) {
                val names = group.commonCourseIds.mapNotNull { courseNames[it] }.sorted().joinToString(", ")
                stringResource(R.string.consigliami_group_title_common, group.events.size, names)
            } else {
                stringResource(R.string.consigliami_group_title_generic, group.events.size)
            }
            // Tessera del gruppo con la sua posizione: chiuso, senza il numero sembrerebbe che la
            // classifica parta dalla card successiva (1, 1, 3: "competition ranking", SPEC §6.4).
            KartPanel(colors = KartTiles.Pink, onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KartCounterPill(stringResource(R.string.consigliami_position_format, group.position), KartTiles.Pink)
                    OutlinedTitle(title, Modifier.weight(1f), fontSize = 18.sp, textAlign = TextAlign.Start, maxLines = 3)
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(
                                if (expanded) R.string.consigliami_group_collapse_cd else R.string.consigliami_group_expand_cd,
                            ),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
        if (group.events.size <= 1 || expanded) {
            group.events.forEach { event ->
                EventCard(
                    eventScore = event,
                    position = group.position,
                    characterNames = characterNames,
                    characterImages = characterImages,
                    eventImageUrl = eventImages[event.event.id],
                    courseNames = courseNames,
                    foodGroupNames = foodGroupNames,
                    bestRank = bestRankByEvent[event.event.id],
                    referenceCc = referenceCc,
                    onClick = { onEventClick(event.event.id) },
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    eventScore: EventScore,
    position: Int,
    characterNames: Map<String, String>,
    characterImages: Map<String, String>,
    eventImageUrl: String?,
    courseNames: Map<String, String>,
    foodGroupNames: Map<String, String>,
    bestRank: TrophyRank?,
    referenceCc: Cc,
    onClick: () -> Unit,
) {
    val isCup = eventScore.event.type == EventType.CUP
    val colors = if (isCup) KartTiles.Orange else KartTiles.Green
    KartPanel(colors = colors, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EventIcon(name = eventScore.event.name, imageUrl = eventImageUrl, size = 52.dp, outlined = true)
                Column(Modifier.weight(1f)) {
                    OutlinedTitle(eventScore.event.name, fontSize = 20.sp, textAlign = TextAlign.Start, maxLines = 2)
                    Text(
                        text = stringResource(if (isCup) R.string.consigliami_type_cup else R.string.consigliami_type_rally),
                        style = ConsigliamiBodyStyle.copy(fontSize = 13.sp),
                    )
                }
                KartCounterPill(stringResource(R.string.consigliami_position_format, position), colors)
            }

            if (bestRank != null) {
                Text(
                    stringResource(R.string.consigliami_best_result_format, ccLabel(referenceCc), rankLabel(bestRank)),
                    style = ConsigliamiBodyStyle.copy(fontSize = 14.sp),
                )
            }

            val best = eventScore.best
            if (best != null) {
                SubTitle(stringResource(R.string.consigliami_recommended_character))
                CharacterGainRow(best, characterNames, characterImages, emphasized = true)
                if (eventScore.runnersUp.isNotEmpty()) {
                    SubTitle(stringResource(R.string.consigliami_runners_up))
                    eventScore.runnersUp.forEach { CharacterGainRow(it, characterNames, characterImages, emphasized = false) }
                }
                if (eventScore.relevantFoods.isNotEmpty()) {
                    // Possibilità, non promessa: lo stand è nell'area del percorso (fase C1).
                    SubTitle(stringResource(R.string.consigliami_foods_maybe))
                    eventScore.relevantFoods.forEach { food ->
                        val foodName = foodGroupNames[food.foodGroupId] ?: food.foodGroupId
                        val courseName = courseNames[food.courseId] ?: food.courseId
                        Text("$foodName · $courseName", style = ConsigliamiBodyStyle)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubTitle(text: String) {
    OutlinedTitle(text, fontSize = 15.sp, textAlign = TextAlign.Start, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun CharacterGainRow(
    gain: CharacterGain,
    characterNames: Map<String, String>,
    characterImages: Map<String, String>,
    emphasized: Boolean,
) {
    val name = characterNames[gain.characterId] ?: gain.characterId
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CharacterAvatar(
            name = name,
            imageUrl = characterImages[gain.characterId],
            size = if (emphasized) 44.dp else 32.dp,
            modifier = Modifier.border(2.dp, KartInk, CircleShape),
        )
        OutlinedTitle(
            text = name,
            modifier = Modifier.weight(1f),
            fontSize = if (emphasized) 18.sp else 15.sp,
            textAlign = TextAlign.Start,
            maxLines = 2,
        )
        Text(
            text = stringResource(R.string.consigliami_gain_format, gain.gain),
            style = ConsigliamiBodyStyle.copy(fontSize = if (emphasized) 16.sp else 14.sp),
        )
    }
}

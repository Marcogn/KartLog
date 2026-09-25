package com.marcogn.kartlog.ui.consigliami

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.EventType

/** Form di registrazione risultato (SPEC §2.6): bottom sheet, evento fisso (schermata di dettaglio). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceResultBottomSheet(
    eventType: EventType,
    characterNames: Map<String, String>,
    onSave: (cc: Cc, stars: Int, placement: Int?, eliminatedAt: Int?, characterId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var cc by remember { mutableStateOf(Cc.CC_150) }
    var stars by remember { mutableStateOf(0) }
    // Il GP corre sempre un giro intero: solo posizione. Il KO può finire con un'eliminazione.
    var eliminatedMode by remember { mutableStateOf(false) }
    var placementText by remember { mutableStateOf("") }
    var checkpointText by remember { mutableStateOf("") }
    var characterId by remember { mutableStateOf<String?>(null) }
    var characterMenuExpanded by remember { mutableStateOf(false) }

    val placement = placementText.toIntOrNull()?.takeIf { it in 1..24 }
    val checkpoint = checkpointText.toIntOrNull()?.takeIf { it > 0 }
    val canSave = if (eliminatedMode) checkpoint != null else placement != null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.consigliami_register_result), style = MaterialTheme.typography.titleLarge)

            Text(stringResource(R.string.consigliami_reference_cc), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cc.entries.forEach { option ->
                    FilterChip(selected = cc == option, onClick = { cc = option }, label = { Text(ccLabel(option)) })
                }
            }

            Text(stringResource(R.string.consigliami_result_stars_label), style = MaterialTheme.typography.labelMedium)
            Row {
                (0..3).forEach { value ->
                    IconButton(onClick = { stars = value }) {
                        Icon(
                            imageVector = if (value <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (eventType == EventType.RALLY) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !eliminatedMode,
                        onClick = { eliminatedMode = false },
                        label = { Text(stringResource(R.string.consigliami_result_placement_mode)) },
                    )
                    FilterChip(
                        selected = eliminatedMode,
                        onClick = { eliminatedMode = true },
                        label = { Text(stringResource(R.string.consigliami_result_eliminated_mode)) },
                    )
                }
            }

            if (eliminatedMode) {
                OutlinedTextField(
                    value = checkpointText,
                    onValueChange = { checkpointText = it },
                    label = { Text(stringResource(R.string.consigliami_result_checkpoint_label)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = placementText,
                    onValueChange = { placementText = it },
                    label = { Text(stringResource(R.string.consigliami_result_placement_label)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ExposedDropdownMenuBox(expanded = characterMenuExpanded, onExpandedChange = { characterMenuExpanded = it }) {
                OutlinedTextField(
                    value = characterId?.let { characterNames[it] } ?: stringResource(R.string.consigliami_result_character_none),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.consigliami_result_character_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = characterMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = characterMenuExpanded, onDismissRequest = { characterMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.consigliami_result_character_none)) },
                        onClick = { characterId = null; characterMenuExpanded = false },
                    )
                    characterNames.toSortedMap().forEach { (id, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { characterId = id; characterMenuExpanded = false })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.consigliami_result_cancel)) }
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(cc, stars, if (eliminatedMode) null else placement, if (eliminatedMode) checkpoint else null, characterId)
                    },
                ) { Text(stringResource(R.string.consigliami_result_save)) }
            }
        }
    }
}

@Composable
fun ccLabel(cc: Cc): String = when (cc) {
    Cc.CC_50 -> stringResource(R.string.consigliami_cc_50)
    Cc.CC_100 -> stringResource(R.string.consigliami_cc_100)
    Cc.CC_150 -> stringResource(R.string.consigliami_cc_150)
    Cc.MIRROR -> stringResource(R.string.consigliami_cc_mirror)
}

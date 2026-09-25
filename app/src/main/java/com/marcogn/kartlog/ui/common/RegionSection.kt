package com.marcogn.kartlog.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcogn.kartlog.R

/** Intestazione di sezione collassabile per regione (SPEC §2.4): nome, contatore, "segna tutti". */
@Composable
fun RegionHeader(
    name: String,
    collected: Int,
    total: Int,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onMarkAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpandToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.home_counter_format, collected, total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onMarkAllClick, enabled = collected < total) {
            Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.collectible_mark_all))
        }
    }
}

@Composable
fun MarkAllConfirmationDialog(regionName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collectible_mark_all)) },
        text = { Text(stringResource(R.string.collectible_mark_all_confirm, regionName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.collectible_mark_all_confirm_button)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.collectible_cancel)) } },
    )
}

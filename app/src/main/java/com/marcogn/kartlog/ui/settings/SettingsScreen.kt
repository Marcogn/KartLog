package com.marcogn.kartlog.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.marcogn.kartlog.domain.model.ThemeMode
import com.marcogn.kartlog.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeMode by themeViewModel.themeMode.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let(viewModel::onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::onImport)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreferencesSection(themeMode = themeMode, onThemeModeSelected = themeViewModel::onThemeModeSelected)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.settings_backup_section), style = MaterialTheme.typography.titleMedium)
            Button(onClick = { exportLauncher.launch("kartlog-backup.json") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export_backup))
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_import_backup))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.settings_info_section), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_images_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.settings_images_disclaimer), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.settings_food_names_note), style = MaterialTheme.typography.bodySmall)
            viewModel.meta?.let { meta ->
                Text(
                    "${meta.license.attribution} — ${meta.license.name}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(meta.license.url))) }) {
                    Text(meta.license.url, style = MaterialTheme.typography.bodySmall)
                }
                meta.sources.forEach { source ->
                    // Stesso titolo su due wiki ("Mario Kart World"): il dominio li distingue.
                    val title = "${source.title} (${source.url.toUri().host?.removePrefix("www.")})"
                    val label = source.revid?.let { stringResource(R.string.settings_source_revid_format, title, it) } ?: title
                    Text("• $label", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Tema e lingua, stesso schema di ThePatientGamerHelper (vedi AppLanguage.kt e ThemePreferences). */
@Composable
private fun PreferencesSection(themeMode: ThemeMode, onThemeModeSelected: (ThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.settings_preferences_section), style = MaterialTheme.typography.titleMedium)

        Text(stringResource(R.string.settings_theme_label), style = MaterialTheme.typography.bodyLarge)
        Column(modifier = Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                RadioOptionRow(
                    label = stringResource(mode.labelRes()),
                    selected = themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                )
            }
        }

        Text(stringResource(R.string.settings_language_label), style = MaterialTheme.typography.bodyLarge)
        var selectedLanguage by remember { mutableStateOf(currentAppLanguage()) }
        Column(modifier = Modifier.selectableGroup()) {
            AppLanguage.entries.forEach { language ->
                RadioOptionRow(
                    label = stringResource(language.labelRes()),
                    selected = selectedLanguage == language,
                    onClick = {
                        selectedLanguage = language
                        applyAppLanguage(language)
                    },
                )
            }
        }
    }
}

@Composable
private fun RadioOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SISTEMA -> R.string.theme_system
    ThemeMode.CHIARO -> R.string.theme_light
    ThemeMode.SCURO -> R.string.theme_dark
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SISTEMA -> R.string.language_system
    AppLanguage.ITALIANO -> R.string.language_italian
    AppLanguage.ENGLISH -> R.string.language_english
}

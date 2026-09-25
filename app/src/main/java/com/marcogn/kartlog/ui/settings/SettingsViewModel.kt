package com.marcogn.kartlog.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.backup.BackupRepository
import com.marcogn.kartlog.data.backup.ImportResult
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedMetaDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    assets: SeedAssetLoader,
) : ViewModel() {

    val meta: SeedMetaDto? = assets.readMeta()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun onExport(destination: Uri) {
        viewModelScope.launch {
            runCatching { backupRepository.export(destination) }
                .onSuccess { _messages.tryEmit("Backup esportato.") }
                .onFailure { _messages.tryEmit("Esportazione non riuscita: ${it.message}") }
        }
    }

    fun onImport(source: Uri) {
        viewModelScope.launch {
            runCatching { backupRepository.import(source) }
                .onSuccess { _messages.tryEmit(formatResult(it)) }
                .onFailure { _messages.tryEmit("Importazione non riuscita: ${it.message}") }
        }
    }

    private fun formatResult(result: ImportResult): String {
        val applied = result.appliedCounts.values.sum()
        val unknownCount = result.unknownIds.values.sumOf { it.size }
        return if (unknownCount == 0) {
            "Importati $applied elementi."
        } else {
            "Importati $applied elementi. $unknownCount ID sconosciuti ignorati (${result.unknownIds.keys.joinToString()})."
        }
    }
}

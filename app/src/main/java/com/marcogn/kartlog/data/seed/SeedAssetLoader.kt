package com.marcogn.kartlog.data.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Legge i JSON copiati in `assets/seed/` dal task Gradle `copySeedAssets`
 * (SPEC §5.4) — mai `seed/` direttamente: a runtime esiste solo l'asset impacchettato.
 */
@Singleton
class SeedAssetLoader @Inject constructor(@ApplicationContext private val context: Context) {

    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    fun readText(fileName: String): String? =
        try {
            context.assets.open("seed/$fileName").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            null
        }

    inline fun <reified T> readItems(fileName: String): List<T> {
        val text = readText(fileName) ?: return emptyList()
        return json.decodeFromString<SeedFile<T>>(text).items
    }

    fun readMeta(): SeedMetaDto? = readText("meta.json")?.let { json.decodeFromString(it) }

    /**
     * L'URL `source` di un file seed (SPEC §2.4, "Apri guida" dei Peach Medallions) — a differenza
     * di [readItems], non un campo del [SeedFile] condiviso: lì è stato tolto perché non sempre
     * una stringa (`events.json` ce l'ha come array). Qui serve solo per file dove è una stringa.
     */
    fun readSourceUrl(fileName: String): String? =
        readText(fileName)?.let { runCatching { json.decodeFromString<SourceOnlyDto>(it).source }.getOrNull() }
}

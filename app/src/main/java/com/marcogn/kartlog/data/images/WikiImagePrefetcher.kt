package com.marcogn.kartlog.data.images

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.marcogn.kartlog.data.local.dao.SeedDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import okio.Path.Companion.toOkioPath

/**
 * Le immagini di personaggi, outfit ed eventi non sono nell'APK: si scaricano dal CDN di Super
 * Mario Wiki (URL in `seed/`, estratti da tools/seedgen/seedgen/images.py) e restano nella cache
 * su disco di Coil. [prefetchAll] le richiede tutte all'avvio, così dopo il primo avvio con rete
 * le schermate le mostrano anche offline; quelle già in cache non generano traffico.
 */
@Singleton
class WikiImagePrefetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val seedDao: SeedDao,
) {

    suspend fun prefetchAll() {
        val loader = SingletonImageLoader.get(context)
        seedDao.allImageUrls().forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    // Solo su disco: tenere 170 bitmap in memoria all'avvio non serve a nessuna schermata.
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            )
        }
    }
}

/** ~170 immagini da 50–100 KB: 64 MB bastano con ampio margine e restano in `cacheDir`. */
private const val DISK_CACHE_BYTES = 64L * 1024 * 1024

fun newWikiImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("wiki_images").toOkioPath())
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .build()

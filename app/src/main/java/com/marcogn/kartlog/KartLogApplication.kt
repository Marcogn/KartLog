package com.marcogn.kartlog

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.marcogn.kartlog.data.images.WikiImagePrefetcher
import com.marcogn.kartlog.data.images.newWikiImageLoader
import com.marcogn.kartlog.data.seed.SeedRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class KartLogApplication : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var seedRepository: SeedRepository
    @Inject lateinit var imagePrefetcher: WikiImagePrefetcher

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedRepository.reseedIfNeeded()
            // Dopo il reseed: gli URL arrivano dalle tabelle seed appena (ri)caricate.
            imagePrefetcher.prefetchAll()
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = newWikiImageLoader(context)
}

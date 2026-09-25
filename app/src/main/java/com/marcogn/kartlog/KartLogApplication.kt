package com.marcogn.kartlog

import android.app.Application
import com.marcogn.kartlog.data.seed.SeedRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class KartLogApplication : Application() {

    @Inject lateinit var seedRepository: SeedRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { seedRepository.reseedIfNeeded() }
    }
}

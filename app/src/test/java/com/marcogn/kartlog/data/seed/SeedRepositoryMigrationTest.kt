package com.marcogn.kartlog.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.local.entity.SeedMetaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * SPEC §8, fase 3: "stato utente presente -> reseed -> stato intatto". Il reseed sostituisce solo
 * le tabelle di [com.marcogn.kartlog.data.local.dao.SeedDao.replaceAll], mai quelle di stato utente.
 */
@RunWith(RobolectricTestRunner::class)
class SeedRepositoryMigrationTest {

    private lateinit var db: KartLogDatabase
    private lateinit var repository: SeedRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KartLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SeedRepository(SeedAssetLoader(context), db.seedDao(), db.seedMetaDao(), db.userStateDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `il reseed non tocca lo stato utente`() = runBlocking {
        // Simula un'installazione con un seedVersion precedente e uno stato utente già presente.
        db.seedMetaDao().setSeedVersion(SeedMetaEntity(seedVersion = 0))
        db.userStateDao().markOutfitOwned(OwnedOutfitEntity(outfitId = "mario__touring"))

        repository.reseedIfNeeded()

        // L'outfit segnato a mano resta posseduto, più i 24 outfit di default (uno per
        // personaggio), sempre posseduti automaticamente (SPEC §2.3, ensureDefaultOutfitsOwned()).
        assertEquals(25, db.userStateDao().countOwnedOutfits().first())
        assertTrue("il reseed deve popolare i dati statici", db.seedDao().countOutfits().first() > 0)
    }

    @Test
    fun `il reseed e un no-op se il seedVersion non cambia`() = runBlocking {
        repository.reseedIfNeeded()
        val outfitsAfterFirstSeed = db.seedDao().countOutfits().first()

        repository.reseedIfNeeded()

        assertEquals(outfitsAfterFirstSeed, db.seedDao().countOutfits().first())
    }
}

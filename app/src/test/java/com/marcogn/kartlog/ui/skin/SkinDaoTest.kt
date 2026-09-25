package com.marcogn.kartlog.ui.skin

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedRepository
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
 * SPEC §8, fase 4, "Fatto quando": spuntare l'ultimo outfit di un personaggio lo attenua subito
 * e aggiorna il contatore in Home (SPEC §9). Non è un vero test UI/instrumentato — nessun
 * emulatore/dispositivo disponibile in questo ambiente — ma esercita lo stesso stato osservato
 * dalle due schermate (query di [com.marcogn.kartlog.data.local.dao.SkinDao] e
 * [com.marcogn.kartlog.data.local.dao.UserStateDao.countOwnedOutfits], usata da
 * [com.marcogn.kartlog.ui.home.HomeViewModel]).
 */
@RunWith(RobolectricTestRunner::class)
class SkinDaoTest {

    private lateinit var db: KartLogDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KartLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = SeedRepository(SeedAssetLoader(context), db.seedDao(), db.seedMetaDao(), db.userStateDao())
        runBlocking { repository.reseedIfNeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `a seed appena caricato ogni personaggio ha solo il default posseduto`() = runBlocking {
        val characters = db.skinDao().charactersWithProgress().first()

        assertEquals(24, characters.size)
        characters.forEach { character ->
            assertEquals("${character.name}: solo il default dovrebbe essere posseduto", 1, character.ownedOutfits)
            assertTrue("${character.name} non dovrebbe risultare completo", !character.isComplete)
        }
    }

    @Test
    fun `spuntare un outfit aggiorna il contatore del personaggio e quello globale`() = runBlocking {
        val outfits = db.skinDao().outfitsForCharacter("mario").first()
        val alternative = outfits.first { !it.isDefault }
        val globalBefore = db.userStateDao().countOwnedOutfits().first()

        db.userStateDao().markOutfitOwned(OwnedOutfitEntity(alternative.outfitId))

        val marioAfter = db.skinDao().charactersWithProgress().first().first { it.id == "mario" }
        assertEquals(2, marioAfter.ownedOutfits)
        // Stesso conteggio che HomeViewModel usa per il contatore Skin della Home (SPEC §9).
        assertEquals(globalBefore + 1, db.userStateDao().countOwnedOutfits().first())

        db.userStateDao().markOutfitNotOwned(alternative.outfitId)
        assertEquals(globalBefore, db.userStateDao().countOwnedOutfits().first())
    }

    @Test
    fun `un personaggio con tutti gli outfit risulta completo`() = runBlocking {
        val outfits = db.skinDao().outfitsForCharacter("mario").first()
        outfits.filterNot { it.isDefault }.forEach { db.userStateDao().markOutfitOwned(OwnedOutfitEntity(it.outfitId)) }

        val mario = db.skinDao().charactersWithProgress().first().first { it.id == "mario" }

        assertEquals(mario.totalOutfits, mario.ownedOutfits)
        assertTrue(mario.isComplete)
    }

    @Test
    fun `il seed reale porta il nome ufficiale in italiano fino al DAO`() = runBlocking {
        val mario = db.skinDao().charactersWithProgress().first().first { it.id == "mario" }
        assertEquals("Mario", mario.nameIt)

        val outfits = db.skinDao().outfitsForCharacter("toad").first()
        val explorer = outfits.first { it.outfitName == "Explorer" }
        assertEquals("Esploratore", explorer.outfitNameIt)  // maschile (Toad)

        // Outfit di default: mai un nome, in nessuna lingua (SPEC §2.3, mostrato come "Standard").
        assertEquals(null, outfits.first { it.isDefault }.outfitNameIt)
    }
}

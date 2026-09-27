package com.marcogn.kartlog.ui.skin

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.dao.CharacterProgress
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedRepository
import com.marcogn.kartlog.domain.model.localizedName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Ordinamenti e filtri della lista Personaggi sul seed reale (nessun nome di pilota nel test). */
@RunWith(RobolectricTestRunner::class)
class SkinOrderTest {

    private lateinit var db: KartLogDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KartLogDatabase::class.java).allowMainThreadQueries().build()
        val repository = SeedRepository(SeedAssetLoader(context), db.seedDao(), db.seedMetaDao(), db.userStateDao())
        runBlocking { repository.reseedIfNeeded() }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun characters(): List<CharacterProgress> = db.skinDao().charactersWithProgress().first()

    private fun names(list: List<CharacterProgress>) = list.map { localizedName(it.name, it.nameIt) }

    @Test
    fun `i piloti di base senza outfit si ordinano insieme agli altri, non in fondo`() = runBlocking {
        val all = characters()
        val neutral = all.filter { !it.hasProgress }
        assertTrue(neutral.isNotEmpty())

        // Nulla completato: l'ordine alfabetico è su tutti i 50, senza gruppi a parte.
        val alphabetical = orderCharacters(all, SkinSortMode.ALPHABETICAL, SkinFilterMode.ALL)
        assertEquals(all.size, alphabetical.size)
        assertEquals(names(alphabetical).sorted(), names(alphabetical))
        // Il roster pure: nessun pilota di base senza outfit finisce dopo chi è da sbloccare.
        val roster = orderCharacters(all, SkinSortMode.ROSTER, SkinFilterMode.ALL)
        assertEquals(all.sortedBy { it.rosterOrder }.map { it.id }, roster.map { it.id })

        // "Incompleti" mostra solo chi ha qualcosa da fare.
        val incomplete = orderCharacters(all, SkinSortMode.ALPHABETICAL, SkinFilterMode.INCOMPLETE)
        assertTrue(incomplete.none { it.id in neutral.map { n -> n.id } })
        assertEquals(all.count { it.hasProgress }, incomplete.size)
    }

    @Test
    fun `chi completa qualcosa va in fondo, i piloti di base senza outfit no`() = runBlocking {
        val initial = characters()
        // Completa un pilota con outfit (tutti gli outfit) e sblocca uno sbloccabile senza outfit.
        val withOutfits = initial.first { it.hasOutfits }
        db.skinDao().outfitsForCharacter(withOutfits.id).first().forEach {
            db.userStateDao().markOutfitOwned(OwnedOutfitEntity(it.outfitId))
        }
        val unlockable = initial.first { it.isUnlockable && !it.hasOutfits }
        db.userStateDao().setCharacterUnlock(CharacterUnlockEntity(characterId = unlockable.id, unlocked = true))

        val ordered = orderCharacters(characters(), SkinSortMode.ALPHABETICAL, SkinFilterMode.ALL)
        assertEquals(setOf(withOutfits.id, unlockable.id), ordered.takeLast(2).map { it.id }.toSet())
        val head = ordered.dropLast(2)
        assertEquals(names(head).sorted(), names(head))
    }
}

package com.marcogn.kartlog.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.local.entity.RaceResultEntity
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedRepository
import com.marcogn.kartlog.domain.model.Cc
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** SPEC §8, "fatto quando": export -> reinstallazione -> import ripristina tutto; ID sconosciuti segnalati. */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private lateinit var db: KartLogDatabase
    private lateinit var repository: BackupRepository
    private lateinit var context: Context
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, KartLogDatabase::class.java).allowMainThreadQueries().build()
        val seedRepository = SeedRepository(SeedAssetLoader(context), db.seedDao(), db.seedMetaDao(), db.userStateDao())
        runBlocking { seedRepository.reseedIfNeeded() }
        repository = BackupRepository(db.backupDao(), context)
    }

    @After
    fun tearDown() {
        db.close()
        tempFiles.forEach { it.delete() }
    }

    private fun tempBackupUri(): Uri {
        val file = File.createTempFile("kartlog-backup-test", ".json")
        tempFiles += file
        return Uri.fromFile(file)
    }

    @Test
    fun `esportare poi importare su uno stato azzerato ripristina tutto`() = runBlocking {
        val userStateDao = db.userStateDao()
        val characters = db.consigliamiDao().characters().first()
        val outfitId = db.skinDao().outfitsForCharacter(characters.first().id).first().first { !it.isDefault }.outfitId
        val lockedCharacterId = characters[1].id
        val medallionId = db.medallionsDao().allMedallions().first().first().medallionId
        val pSwitchId = db.pSwitchesDao().allPSwitches().first().first().pSwitchId
        val eventId = db.consigliamiDao().events().first().first().id

        userStateDao.markOutfitOwned(OwnedOutfitEntity(outfitId))
        userStateDao.setCharacterUnlock(CharacterUnlockEntity(lockedCharacterId, unlocked = false))
        userStateDao.markMedallionCollected(CollectedMedallionEntity(medallionId))
        userStateDao.markPSwitchCompleted(CompletedPSwitchEntity(pSwitchId))
        userStateDao.insertRaceResult(
            RaceResultEntity(eventId = eventId, cc = Cc.CC_150, stars = 2, placement = 5, eliminatedAt = null, characterId = null, timestamp = 123L)
        )
        val ownedBefore = userStateDao.countOwnedOutfits().first()

        val uri = tempBackupUri()
        repository.export(uri)

        // "Reinstallazione": stato utente azzerato, i dati di gioco restano (mai toccati dal backup).
        db.backupDao().replaceUserState(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(0, userStateDao.countOwnedOutfits().first())

        val result = repository.import(uri)

        assertTrue("nessun ID dovrebbe risultare sconosciuto", result.unknownIds.isEmpty())
        assertEquals(ownedBefore, userStateDao.countOwnedOutfits().first())
        assertEquals(1, userStateDao.countCollectedMedallions().first())
        assertEquals(1, userStateDao.countCompletedPSwitches().first())
        assertEquals(false, userStateDao.observeCharacterUnlock(lockedCharacterId).first())
        val history = userStateDao.raceResultsForEvent(eventId).first()
        assertEquals(1, history.size)
        assertEquals(2, history.single().stars)
    }

    @Test
    fun `un ID non piu esistente viene segnalato, non scartato in silenzio`() = runBlocking {
        val payload = BackupPayload(exportedAt = 0L, ownedOutfitIds = listOf("outfit-che-non-esiste-piu"))
        val file = File.createTempFile("kartlog-backup-unknown", ".json").also { tempFiles += it }
        file.writeText(Json.encodeToString(BackupPayload.serializer(), payload))

        val result = repository.import(Uri.fromFile(file))

        assertEquals(listOf("outfit-che-non-esiste-piu"), result.unknownIds["outfits"])
        assertEquals(0, db.userStateDao().countOwnedOutfits().first())
    }
}

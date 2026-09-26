package com.marcogn.kartlog.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v1 -> v2 (colonna `nameIt`, seedgen/i18n.py): una riga di stato utente scritta a schema v1 deve
 * sopravvivere intatta alla migrazione — è quello che un utente con l'app già installata subisce
 * davvero al prossimo avvio, non solo le tabelle seed (che il reseed sovrascrive comunque).
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KartLogDatabase::class.java,
        emptyList(),
    )

    @Test
    fun `migrazione 1 a 2 aggiunge nameIt e preserva lo stato utente`() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO characters (id, name, rosterOrder, imageRes) VALUES ('mario', 'Mario', 0, NULL)")
            execSQL("INSERT INTO courses (id, name, regionId) VALUES ('mario_circuit', 'Mario Circuit', NULL)")
            execSQL(
                "INSERT INTO outfits (id, characterId, name, isDefault) VALUES ('mario__default', 'mario', NULL, 1)"
            )
            execSQL("INSERT INTO owned_outfits (outfitId) VALUES ('mario__default')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        migrated.query("SELECT nameIt FROM characters WHERE id = 'mario'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(true, cursor.isNull(0))
        }
        migrated.query("SELECT outfitId FROM owned_outfits").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("mario__default", cursor.getString(0))
        }
    }

    @Test
    fun `migrazione 2 a 3 converte lo storico dei risultati nel miglior trofeo per cilindrata`() {
        helper.createDatabase(dbName, 2).apply {
            val insert = "INSERT INTO race_results (eventId, cc, stars, placement, eliminatedAt, characterId, timestamp) VALUES "
            // cup_a 150cc: argento (stelle impossibili ignorate) e oro 2 stelle -> oro 2 stelle.
            execSQL(insert + "('cup_a', 'CC_150', 3, 2, NULL, NULL, 1)")
            execSQL(insert + "('cup_a', 'CC_150', 2, 1, NULL, 'mario', 2)")
            // cup_a 100cc: bronzo.
            execSQL(insert + "('cup_a', 'CC_100', 0, 3, NULL, NULL, 3)")
            // rally_b Mirror: eliminato, e 4° -> nessun trofeo, nessuna riga.
            execSQL(insert + "('rally_b', 'MIRROR', 0, NULL, 3, NULL, 4)")
            execSQL(insert + "('rally_b', 'MIRROR', 0, 4, NULL, NULL, 5)")
            // Stelle fuori scala (dati scritti a mano) -> limitate a 3.
            execSQL(insert + "('cup_c', 'CC_50', 9, 1, NULL, NULL, 6)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        val rows = mutableMapOf<String, String>()
        migrated.query("SELECT eventId, cc, rank FROM best_results").use { cursor ->
            while (cursor.moveToNext()) rows["${cursor.getString(0)}/${cursor.getString(1)}"] = cursor.getString(2)
        }
        assertEquals(
            mapOf(
                "cup_a/CC_150" to "GOLD_2_STARS",
                "cup_a/CC_100" to "BRONZE",
                "cup_c/CC_50" to "GOLD_3_STARS",
            ),
            rows,
        )
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'race_results'").use { cursor ->
            assertEquals(false, cursor.moveToFirst())
        }
    }

    @Test
    fun `migrazione 3 a 4 aggiunge imageUrl e preserva lo stato utente`() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("INSERT INTO characters (id, name, nameIt, rosterOrder, imageRes) VALUES ('mario', 'Mario', NULL, 0, NULL)")
            execSQL(
                "INSERT INTO outfits (id, characterId, name, nameIt, isDefault) VALUES ('mario__default', 'mario', NULL, NULL, 1)"
            )
            execSQL("INSERT INTO events (id, type, name, `order`) VALUES ('cup_a', 'CUP', 'Cup A', 0)")
            execSQL("INSERT INTO owned_outfits (outfitId) VALUES ('mario__default')")
            execSQL("INSERT INTO best_results (eventId, cc, rank) VALUES ('cup_a', 'CC_150', 'GOLD')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4)

        for (table in listOf("characters", "outfits", "events")) {
            migrated.query("SELECT imageUrl FROM $table").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(true, cursor.isNull(0))
            }
        }
        migrated.query("SELECT outfitId FROM owned_outfits").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("mario__default", cursor.getString(0))
        }
        migrated.query("SELECT rank FROM best_results WHERE eventId = 'cup_a'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("GOLD", cursor.getString(0))
        }
    }
}

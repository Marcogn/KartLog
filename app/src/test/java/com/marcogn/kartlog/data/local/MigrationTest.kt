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
}

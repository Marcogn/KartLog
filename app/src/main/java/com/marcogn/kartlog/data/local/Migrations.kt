package com.marcogn.kartlog.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: colonna `nameIt` (nome ufficiale in italiano, seedgen/i18n.py) su `characters`,
 * `outfits`, `courses`. Solo tabelle seed (read-only, sovrascritte da [com.marcogn.kartlog.data.seed.SeedRepository]
 * al prossimo reseed): l'ALTER TABLE qui serve solo a far coincidere lo schema, i valori arrivano
 * comunque dal reseed. Le tabelle di stato utente non sono toccate.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE characters ADD COLUMN nameIt TEXT")
        db.execSQL("ALTER TABLE outfits ADD COLUMN nameIt TEXT")
        db.execSQL("ALTER TABLE courses ADD COLUMN nameIt TEXT")
    }
}

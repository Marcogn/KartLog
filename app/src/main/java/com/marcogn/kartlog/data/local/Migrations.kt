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

/**
 * v2 -> v3: lo storico `race_results` (stelle 0–3 e posizione come campi indipendenti) diventa
 * `best_results`, un solo trofeo per evento e cilindrata sulla scala di
 * [com.marcogn.kartlog.domain.model.TrophyRank]. È stato utente di utenti reali (0.1.x): si
 * converte, non si butta. Per ogni (evento, cilindrata) si tiene il livello più alto, con la
 * stessa regola di `TrophyRank.fromLegacy`: 1° -> oro + stelle, 2° -> argento, 3° -> bronzo,
 * il resto (4° o oltre, eliminazioni KO) non è un trofeo e non genera righe.
 * `characterId` e `timestamp` del vecchio storico non hanno più un posto nel modello e si perdono.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `best_results` (`eventId` TEXT NOT NULL, `cc` TEXT NOT NULL, " +
                "`rank` TEXT NOT NULL, PRIMARY KEY(`eventId`, `cc`))"
        )
        db.execSQL(
            """
            INSERT INTO best_results (eventId, cc, rank)
            SELECT eventId, cc,
                CASE MAX(level)
                    WHEN 1 THEN 'BRONZE'
                    WHEN 2 THEN 'SILVER'
                    WHEN 3 THEN 'GOLD'
                    WHEN 4 THEN 'GOLD_1_STAR'
                    WHEN 5 THEN 'GOLD_2_STARS'
                    ELSE 'GOLD_3_STARS'
                END
            FROM (
                SELECT eventId, cc,
                    CASE placement
                        WHEN 1 THEN 3 + MIN(MAX(stars, 0), 3)
                        WHEN 2 THEN 2
                        WHEN 3 THEN 1
                        ELSE 0
                    END AS level
                FROM race_results
            )
            GROUP BY eventId, cc
            HAVING MAX(level) > 0
            """.trimIndent()
        )
        db.execSQL("DROP TABLE race_results")
    }
}

/**
 * v3 -> v4: colonna `imageUrl` (URL dell'immagine sul CDN di Super Mario Wiki, seedgen/images.py)
 * su `characters`, `outfits`, `events`. Come per v1 -> v2, solo tabelle seed: i valori arrivano dal
 * reseed (seedVersion cambiato), lo stato utente non è toccato.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE characters ADD COLUMN imageUrl TEXT")
        db.execSQL("ALTER TABLE outfits ADD COLUMN imageUrl TEXT")
        db.execSQL("ALTER TABLE events ADD COLUMN imageUrl TEXT")
    }
}

/**
 * v4 -> v5: `starter` su `characters` (disponibile dall'inizio o da sbloccare, seedgen/images.py) e
 * `nameIt` su `regions`, `events`, `food_groups` (mariowiki.it e traduzione manuale dei cibi).
 * Solo tabelle seed: i valori arrivano dal reseed. `character_unlocks` non si tocca: chi aveva già
 * segnato un personaggio mantiene la sua scelta, gli altri seguono il nuovo `starter`.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE characters ADD COLUMN starter INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE regions ADD COLUMN nameIt TEXT")
        db.execSQL("ALTER TABLE events ADD COLUMN nameIt TEXT")
        db.execSQL("ALTER TABLE food_groups ADD COLUMN nameIt TEXT")
    }
}

/** v6: criteri di sblocco dei piloti. Colonne di dati seed: il reseed (seedVersion 6) le riempie. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE characters ADD COLUMN unlockCriteria TEXT")
        db.execSQL("ALTER TABLE characters ADD COLUMN unlockCriteriaIt TEXT")
    }
}

/** Id della riga di `pending_notices` scritta da [MIGRATION_6_7]. */
const val NOTICE_MEDALLION_COUNTS_RESET = "medallion_counts_reset"

/**
 * v7: mappa dei collezionabili da mkworld-checklist (scelta dell'autore, 28/09/2026).
 * - `peach_medallions` cambia forma: da "posti" anonimi per bioma a 200 monete con posizione, senza
 *   bioma. Tabella seed, si ricrea vuota e la riempie il reseed (seedVersion 7).
 * - Le monete segnate (`collected_medallions`) non si possono convertire: un conteggio per bioma non dice
 *   quali monete. Si cancellano, lasciando in `pending_notices` quante erano, per avvisare l'utente.
 * - `p_switches` prende posizione, istruzioni e video; nuove `question_panels` e `activated_question_panels`.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_notices` (`id` TEXT NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL(
            """
            INSERT INTO pending_notices (id, count)
            SELECT '$NOTICE_MEDALLION_COUNTS_RESET', (SELECT COUNT(*) FROM collected_medallions)
            WHERE EXISTS (SELECT 1 FROM collected_medallions)
            """.trimIndent()
        )
        db.execSQL("DELETE FROM collected_medallions")
        db.execSQL("DROP TABLE peach_medallions")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `peach_medallions` (`id` TEXT NOT NULL, `index` INTEGER NOT NULL, " +
                "`x` REAL NOT NULL, `y` REAL NOT NULL, `hint` TEXT, `youtubeId` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `question_panels` (`id` TEXT NOT NULL, `index` INTEGER NOT NULL, " +
                "`x` REAL NOT NULL, `y` REAL NOT NULL, `hint` TEXT, `youtubeId` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS `activated_question_panels` (`panelId` TEXT NOT NULL, PRIMARY KEY(`panelId`))")
        db.execSQL("ALTER TABLE p_switches ADD COLUMN x REAL")
        db.execSQL("ALTER TABLE p_switches ADD COLUMN y REAL")
        db.execSQL("ALTER TABLE p_switches ADD COLUMN hint TEXT")
        db.execSQL("ALTER TABLE p_switches ADD COLUMN youtubeId TEXT")
    }
}

/**
 * v8 (fase C1): gli stand Yoshi's e i cibi uno per uno.
 * - `food_group_courses` (presenza ON_COURSE/NEARBY per corso) è sostituita da `yoshi_stands` +
 *   `yoshi_stand_foods`, tutti gli stand di "List of Yoshi's locations", anche sulle strade.
 * - Nuova `food_variants`: ogni cibo della tabella di Dash Food, con livello di boost e immagine.
 * Solo tabelle seed, nate vuote: le riempie il reseed (seedVersion 9). Nessuno stato utente nuovo
 * né toccato (il backup resta alla v3).
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS food_group_courses")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_variants` (`id` TEXT NOT NULL, `foodGroupId` TEXT NOT NULL, " +
                "`order` INTEGER NOT NULL, `name` TEXT NOT NULL, `nameIt` TEXT, `boost` TEXT NOT NULL, " +
                "`imageUrl` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `yoshi_stands` (`id` TEXT NOT NULL, `order` INTEGER NOT NULL, " +
                "`courseId` TEXT, `regionId` TEXT, `establishment` TEXT, `establishmentIt` TEXT, " +
                "`location` TEXT, `locationIt` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `yoshi_stand_foods` (`standId` TEXT NOT NULL, `foodGroupId` TEXT NOT NULL, " +
                "`food` TEXT, PRIMARY KEY(`standId`, `foodGroupId`))"
        )
    }
}

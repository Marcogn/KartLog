package com.marcogn.kartlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.Presence

/**
 * Dati seed (SPEC §3): read-only, ricaricati a ogni cambio di [com.marcogn.kartlog.data.local.entity.SeedMetaEntity.seedVersion].
 * Mai popolati/modificati a mano: arrivano solo da [com.marcogn.kartlog.data.seed.SeedRepository].
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameIt: String?,
    val rosterOrder: Int,
    val imageRes: String? = null,
    /** Immagine sul CDN di Super Mario Wiki (seedgen/images.py), scaricata a runtime: mai nell'APK. */
    val imageUrl: String? = null,
    /**
     * Disponibile dall'inizio (galleria "Default drivers" del wiki) o da sbloccare giocando. Vale
     * finché l'utente non registra uno stato proprio in `character_unlocks`.
     */
    @ColumnInfo(defaultValue = "1") val starter: Boolean = true,
)

@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val name: String?,
    val nameIt: String?,
    val isDefault: Boolean,
    /** Vedi [CharacterEntity.imageUrl]. Per l'outfit di default è l'immagine del personaggio. */
    val imageUrl: String? = null,
)

@Entity(tableName = "food_groups")
data class FoodGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val foods: List<String>,
    val revertsToDefault: Boolean,
    /** Traduzione NON ufficiale (tools/seedgen/manual/food_names_it.yaml): nessuna fonte ha i nomi italiani. */
    val nameIt: String? = null,
)

@Entity(tableName = "outfit_food_rules", primaryKeys = ["outfitId", "foodGroupId"])
data class OutfitFoodRuleEntity(
    val outfitId: String,
    val foodGroupId: String,
)

@Entity(tableName = "food_group_courses", primaryKeys = ["foodGroupId", "courseId"])
data class FoodGroupCourseEntity(
    val foodGroupId: String,
    val courseId: String,
    val presence: Presence,
    val listedInDashFood: Boolean,
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameIt: String?,
    val regionId: String?,
)

@Entity(tableName = "regions")
data class RegionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val order: Int,
    /** Nome del bioma su mariowiki.it (seedgen/it_wiki.py). */
    val nameIt: String? = null,
)

@Entity(tableName = "areas")
data class AreaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val regionId: String,
)

@Entity(tableName = "peach_medallions")
data class PeachMedallionEntity(
    @PrimaryKey val id: String,
    val regionId: String,
    val index: Int,
)

@Entity(tableName = "p_switches")
data class PSwitchEntity(
    @PrimaryKey val id: String,
    val index: Int,
    val regionId: String,
    val courseId: String?,
    val areaId: String?,
    val name: String,
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val type: EventType,
    val name: String,
    val order: Int,
    /** Icona della cup/del rally, vedi [CharacterEntity.imageUrl]. */
    val imageUrl: String? = null,
    /** "Trofeo Fungo", "Rally Turbo"… da mariowiki.it (seedgen/it_wiki.py). */
    val nameIt: String? = null,
)

@Entity(tableName = "event_stops", primaryKeys = ["eventId", "position"])
data class EventStopEntity(
    val eventId: String,
    val position: Int,
    val courseId: String,
)

/** Riga singola (id sempre 0): tiene traccia del `seedVersion` attualmente caricato in Room. */
@Entity(tableName = "seed_meta")
data class SeedMetaEntity(
    @PrimaryKey val id: Int = 0,
    val seedVersion: Int,
)

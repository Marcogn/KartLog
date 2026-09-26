package com.marcogn.kartlog.data.seed

import com.marcogn.kartlog.data.local.dao.SeedContent
import com.marcogn.kartlog.data.local.dao.SeedDao
import com.marcogn.kartlog.data.local.dao.SeedMetaDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.AreaEntity
import com.marcogn.kartlog.data.local.entity.CharacterEntity
import com.marcogn.kartlog.data.local.entity.CourseEntity
import com.marcogn.kartlog.data.local.entity.EventEntity
import com.marcogn.kartlog.data.local.entity.EventStopEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupCourseEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupEntity
import com.marcogn.kartlog.data.local.entity.OutfitEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import com.marcogn.kartlog.data.local.entity.PSwitchEntity
import com.marcogn.kartlog.data.local.entity.PeachMedallionEntity
import com.marcogn.kartlog.data.local.entity.RegionEntity
import com.marcogn.kartlog.data.local.entity.SeedMetaEntity
import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.Presence
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reseed dei dati di gioco (SPEC §3): confronta il `seedVersion` degli asset con quello già
 * caricato in Room e, se diverso, sostituisce le tabelle seed **senza mai toccare lo stato
 * utente** (tabelle diverse, [SeedDao.replaceAll] non le include).
 */
@Singleton
class SeedRepository @Inject constructor(
    private val assets: SeedAssetLoader,
    private val seedDao: SeedDao,
    private val seedMetaDao: SeedMetaDao,
    private val userStateDao: UserStateDao,
) {

    /**
     * Sostituisce i dati seed se il `seedVersion` degli asset è cambiato, poi garantisce
     * comunque che ogni outfit di default risulti posseduto (SPEC §2.3) — anche quando il
     * reseed è un no-op, per coprire la primissima esecuzione dopo l'installazione.
     */
    suspend fun reseedIfNeeded() {
        val meta = assets.readMeta() ?: return
        val loadedVersion = seedMetaDao.getSeedVersion()
        if (loadedVersion != meta.seedVersion) {
            seedDao.replaceAll(buildSeedContent())
            seedMetaDao.setSeedVersion(SeedMetaEntity(seedVersion = meta.seedVersion))
        }
        userStateDao.ensureDefaultOutfitsOwned()
    }

    private fun buildSeedContent(): SeedContent = SeedContent(
        characters = assets.readItems<CharacterDto>("characters.json").map {
            CharacterEntity(
                id = it.id,
                name = it.name,
                nameIt = it.nameIt,
                rosterOrder = it.rosterOrder,
                imageUrl = it.imageUrl,
                starter = it.starter ?: true,
            )
        },
        outfits = assets.readItems<OutfitDto>("outfits.json").map {
            OutfitEntity(
                id = it.id,
                characterId = it.characterId,
                name = it.name,
                nameIt = it.nameIt,
                isDefault = it.isDefault,
                imageUrl = it.imageUrl,
            )
        },
        foodGroups = assets.readItems<FoodGroupDto>("food_groups.json").map {
            FoodGroupEntity(
                id = it.id,
                name = it.name,
                foods = it.foods,
                revertsToDefault = it.revertsToDefault,
                nameIt = it.nameIt,
            )
        },
        outfitFoodRules = assets.readItems<OutfitFoodRuleDto>("outfit_food_rules.json").map {
            OutfitFoodRuleEntity(outfitId = it.outfitId, foodGroupId = it.foodGroupId)
        },
        foodGroupCourses = assets.readItems<FoodGroupCourseDto>("food_group_courses.json").map {
            FoodGroupCourseEntity(
                foodGroupId = it.foodGroupId,
                courseId = it.courseId,
                presence = Presence.valueOf(it.presence),
                listedInDashFood = it.listedInDashFood,
            )
        },
        courses = assets.readItems<CourseDto>("courses.json").map {
            CourseEntity(id = it.id, name = it.name, nameIt = it.nameIt, regionId = it.regionId)
        },
        regions = assets.readItems<RegionDto>("regions.json").map {
            RegionEntity(id = it.id, name = it.name, order = it.order, nameIt = it.nameIt)
        },
        areas = assets.readItems<AreaDto>("areas.json").map {
            AreaEntity(id = it.id, name = it.name, regionId = it.regionId)
        },
        peachMedallions = assets.readItems<PeachMedallionDto>("peach_medallions.json").map {
            PeachMedallionEntity(id = it.id, regionId = it.regionId, index = it.index)
        },
        // Assente finché la fase 2 di seedgen non gira (SPEC §5.3 "Stato"): readItems torna
        // una lista vuota se il file manca, mai un errore.
        pSwitches = assets.readItems<PSwitchDto>("p_switches.json").map {
            PSwitchEntity(
                id = it.id,
                index = it.index,
                regionId = it.regionId,
                courseId = it.courseId,
                areaId = it.areaId,
                name = it.name,
            )
        },
        events = assets.readItems<EventDto>("events.json").map {
            EventEntity(
                id = it.id,
                type = EventType.valueOf(it.type),
                name = it.name,
                order = it.order,
                imageUrl = it.imageUrl,
                nameIt = it.nameIt,
            )
        },
        eventStops = assets.readItems<EventDto>("events.json").flatMap { event ->
            event.stops.mapIndexed { position, courseId ->
                EventStopEntity(eventId = event.id, position = position, courseId = courseId)
            }
        },
    )
}

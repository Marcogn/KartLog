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
import com.marcogn.kartlog.data.local.entity.FoodGroupEntity
import com.marcogn.kartlog.data.local.entity.FoodVariantEntity
import com.marcogn.kartlog.data.local.entity.OutfitEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import com.marcogn.kartlog.data.local.entity.PSwitchEntity
import com.marcogn.kartlog.data.local.entity.PeachMedallionEntity
import com.marcogn.kartlog.data.local.entity.QuestionPanelEntity
import com.marcogn.kartlog.data.local.entity.RegionEntity
import com.marcogn.kartlog.data.local.entity.SeedMetaEntity
import com.marcogn.kartlog.data.local.entity.YoshiStandEntity
import com.marcogn.kartlog.data.local.entity.YoshiStandFoodEntity
import com.marcogn.kartlog.domain.model.EventType
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

    private fun buildSeedContent(
        stands: List<YoshiStandDto> = assets.readItems<YoshiStandDto>("yoshi_stands.json"),
    ): SeedContent = SeedContent(
        characters = assets.readItems<CharacterDto>("characters.json").map {
            CharacterEntity(
                id = it.id,
                name = it.name,
                nameIt = it.nameIt,
                rosterOrder = it.rosterOrder,
                imageUrl = it.imageUrl,
                starter = it.starter ?: true,
                unlockCriteria = it.unlockCriteria,
                unlockCriteriaIt = it.unlockCriteriaIt,
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
        foodVariants = assets.readItems<FoodVariantDto>("food_variants.json").map {
            FoodVariantEntity(
                id = it.id,
                foodGroupId = it.foodGroupId,
                order = it.order,
                name = it.name,
                nameIt = it.nameIt,
                boost = it.boost,
                imageUrl = it.imageUrl,
            )
        },
        // L'ordine della pagina del wiki (percorsi, poi strade) è quello in cui si mostrano.
        yoshiStands = stands.mapIndexed { order, it ->
            YoshiStandEntity(
                id = it.id,
                order = order,
                courseId = it.courseId,
                regionId = it.regionId,
                establishment = it.establishment,
                establishmentIt = it.establishmentIt,
                location = it.location,
                locationIt = it.locationIt,
            )
        },
        yoshiStandFoods = stands.flatMap { stand ->
            stand.foods.map { YoshiStandFoodEntity(standId = stand.id, foodGroupId = it.foodGroupId, food = it.food) }
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
        peachMedallions = assets.readItems<MapPointDto>("peach_medallions.json").map {
            PeachMedallionEntity(id = it.id, index = it.index, x = it.x, y = it.y, hint = it.hint, youtubeId = it.youtubeId)
        },
        questionPanels = assets.readItems<MapPointDto>("question_panels.json").map {
            QuestionPanelEntity(id = it.id, index = it.index, x = it.x, y = it.y, hint = it.hint, youtubeId = it.youtubeId)
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
                x = it.x,
                y = it.y,
                hint = it.hint,
                youtubeId = it.youtubeId,
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

package com.marcogn.kartlog.data.seed

import kotlinx.serialization.Serializable

/**
 * Forma comune di ogni file in `seed/` (SPEC §5.1): `{"source": ..., "items": [...]}`.
 * `source` non serve all'app (solo attribuzione, letta da `meta.json` per la schermata Info) ed
 * è a volte una stringa a volte un array: si ignora del tutto (`ignoreUnknownKeys` in [SeedAssetLoader]).
 */
@Serializable
data class SeedFile<T>(
    val items: List<T>,
)

@Serializable
data class CharacterDto(val id: String, val name: String, val rosterOrder: Int)

@Serializable
data class OutfitDto(val id: String, val characterId: String, val name: String? = null, val isDefault: Boolean)

@Serializable
data class FoodGroupDto(val id: String, val name: String, val foods: List<String>, val revertsToDefault: Boolean)

@Serializable
data class OutfitFoodRuleDto(val outfitId: String, val foodGroupId: String)

@Serializable
data class FoodGroupCourseDto(
    val foodGroupId: String,
    val courseId: String,
    val presence: String,
    val listedInDashFood: Boolean,
)

@Serializable
data class CourseDto(val id: String, val name: String, val regionId: String? = null)

@Serializable
data class RegionDto(val id: String, val name: String, val order: Int)

@Serializable
data class AreaDto(val id: String, val name: String, val regionId: String)

@Serializable
data class PeachMedallionDto(val id: String, val regionId: String, val index: Int)

@Serializable
data class PSwitchDto(
    val id: String,
    val index: Int,
    val regionId: String,
    val courseId: String? = null,
    val areaId: String? = null,
    val name: String,
)

@Serializable
data class EventDto(val id: String, val type: String, val name: String, val order: Int, val stops: List<String>)

@Serializable
data class SeedMetaSourceDto(val title: String, val url: String, val revid: Int? = null)

@Serializable
data class SeedMetaLicenseDto(val name: String, val url: String, val attribution: String)

@Serializable
data class SeedMetaDto(
    val seedVersion: Int,
    val gameVersion: String,
    val extractedOn: String,
    val origin: String,
    val note: String = "",
    val warnings: List<String> = emptyList(),
    val license: SeedMetaLicenseDto,
    val sources: List<SeedMetaSourceDto> = emptyList(),
)

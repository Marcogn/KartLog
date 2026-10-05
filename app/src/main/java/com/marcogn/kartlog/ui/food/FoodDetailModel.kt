package com.marcogn.kartlog.ui.food

import com.marcogn.kartlog.data.local.dao.FoodCharacterRow
import com.marcogn.kartlog.data.local.dao.FoodOutfitRow
import com.marcogn.kartlog.data.local.dao.FoodStandRow
import com.marcogn.kartlog.data.local.entity.FoodVariantEntity
import com.marcogn.kartlog.domain.model.localizedName

/** Un percorso con gli stand che hanno il cibo: "bioma › percorso" e i luoghi, nell'ordine della fonte. */
data class FoodCourseStands(val courseId: String, val title: String, val places: List<String>)

/**
 * I luoghi di un cibo. [variantName] è valorizzato solo nel gruppo sushi, dove la fonte nomina la
 * variante; per gli altri gruppi c'è una sola sezione, valida per tutte le taglie (CLAUDE.md, handoff C1).
 */
data class FoodStandSection(
    val variantName: String?,
    val courses: List<FoodCourseStands>,
    /** Stand sulle strade: bioma se la fonte lo dice, poi il luogo. */
    val roads: List<String>,
)

data class FoodCharacterOutfits(
    val characterId: String,
    val name: String,
    val imageUrl: String?,
    val outfits: List<FoodOutfitRow>,
)

/** Testo di uno stand: tipo di locale (se c'è) e luogo, nella lingua dell'app. */
internal fun standText(row: FoodStandRow): String =
    listOfNotNull(
        row.establishment?.let { localizedName(it, row.establishmentIt) },
        row.location?.let { localizedName(it, row.locationIt) },
    ).joinToString(" – ")

/**
 * Divide gli stand in sezioni. Dove la fonte nomina il cibo preciso ([FoodStandRow.food], gruppo
 * sushi) c'è una sezione per variante, nell'ordine della tabella; gli stand senza nome di variante
 * vanno in una sezione senza titolo in testa. Altrimenti una sola sezione.
 */
internal fun buildStandSections(stands: List<FoodStandRow>, variants: List<FoodVariantEntity>): List<FoodStandSection> {
    if (stands.isEmpty()) return emptyList()
    if (stands.none { it.food != null }) return listOf(sectionOf(null, stands))
    val known = variants.map { it.name }
    val unnamed = stands.filter { it.food == null }
    // Un cibo che non corrisponde a nessuna variante non va perso: ha la sua sezione in coda.
    val foods = known + stands.mapNotNull { it.food }.distinct().filter { it !in known }
    val sections = foods.mapNotNull { food ->
        val rows = stands.filter { it.food == food }
        if (rows.isEmpty()) null else sectionOf(variants.firstOrNull { it.name == food }?.let { localizedName(it.name, it.nameIt) } ?: food, rows)
    }
    return (if (unnamed.isEmpty()) emptyList() else listOf(sectionOf(null, unnamed))) + sections
}

private fun sectionOf(variantName: String?, rows: List<FoodStandRow>): FoodStandSection {
    val (onCourse, onRoad) = rows.partition { it.courseId != null }
    val courses = onCourse.groupBy { it.courseId!! }.map { (courseId, group) ->
        val first = group.first()
        val course = localizedName(first.courseName.orEmpty(), first.courseNameIt)
        val region = first.regionName?.let { localizedName(it, first.regionNameIt) }
        FoodCourseStands(
            courseId = courseId,
            title = listOfNotNull(region, course).joinToString(" › "),
            places = group.map(::standText),
        )
    }
    val roads = onRoad.map { row ->
        val region = row.regionName?.let { localizedName(it, row.regionNameIt) }
        listOfNotNull(region, standText(row).ifBlank { null }).joinToString(": ")
    }
    return FoodStandSection(variantName, courses, roads)
}

/**
 * Gli outfit del cibo per personaggio (prima i mancanti, poi in ordine di roster) e, a parte, i nomi dei personaggi che hanno
 * outfit ma non ne ottengono nessuno da questo cibo.
 */
internal fun buildCharacterOutfits(
    outfits: List<FoodOutfitRow>,
    charactersWithOutfits: List<FoodCharacterRow>,
): Pair<List<FoodCharacterOutfits>, List<String>> {
    val grouped = outfits.groupBy { it.characterId }
    // Prima chi ha ancora outfit da ottenere, poi gli altri; a parità resta l'ordine di roster (sortedBy è stabile).
    val withFood = charactersWithOutfits.filter { it.id in grouped }.map { ch ->
        FoodCharacterOutfits(ch.id, localizedName(ch.name, ch.nameIt), ch.imageUrl, grouped.getValue(ch.id))
    }.sortedBy { entry -> entry.outfits.all { it.owned } }
    val without = charactersWithOutfits.filter { it.id !in grouped }.map { localizedName(it.name, it.nameIt) }
    return withFood to without
}

package com.marcogn.kartlog.ui.consigliami

import com.marcogn.kartlog.domain.consigliami.CharacterDetail

/** Un personaggio del dettaglio evento, già con nomi e immagini localizzati. */
data class DetailCharacterUi(
    val characterId: String,
    val name: String,
    val imageUrl: String?,
    val gain: Int,
    val outfits: List<DetailOutfitUi>,
)

/** Una riga "outfit – cibo · percorsi": [foods] nell'ordine di tappa del primo percorso utile. */
data class DetailOutfitUi(val outfitId: String, val name: String, val foods: List<DetailFoodUi>)

/** Un cibo che sblocca l'outfit e i percorsi dell'evento dove c'è uno stand che lo vende. */
data class DetailFoodUi(val foodGroupId: String, val name: String, val imageUrl: String?, val courseNames: List<String>)

/**
 * Trasforma i [CharacterDetail] del dominio in righe pronte da disegnare: per ogni outfit, le fonti
 * (cibo, percorso) raggruppate per cibo, così lo stesso cibo su più percorsi sta in una riga sola.
 * Puro, senza Android: testato in `ConsigliamiDetailModelTest`.
 */
fun detailCharactersUi(
    details: List<CharacterDetail>,
    characterNames: Map<String, String>,
    characterImages: Map<String, String>,
    outfitNames: Map<String, String>,
    foodGroupNames: Map<String, String>,
    foodGroupImages: Map<String, String>,
    courseNames: Map<String, String>,
): List<DetailCharacterUi> = details.map { detail ->
    DetailCharacterUi(
        characterId = detail.characterId,
        name = characterNames[detail.characterId] ?: detail.characterId,
        imageUrl = characterImages[detail.characterId],
        gain = detail.gain,
        outfits = detail.outfits.map { outfit ->
            DetailOutfitUi(
                outfitId = outfit.outfitId,
                name = outfitNames[outfit.outfitId] ?: outfit.outfitId,
                // groupBy mantiene l'ordine della prima comparsa: quello di tappa dato dal dominio.
                foods = outfit.sources.groupBy { it.foodGroupId }.map { (foodGroupId, sources) ->
                    DetailFoodUi(
                        foodGroupId = foodGroupId,
                        name = foodGroupNames[foodGroupId] ?: foodGroupId,
                        imageUrl = foodGroupImages[foodGroupId],
                        courseNames = sources.map { courseNames[it.courseId] ?: it.courseId }.distinct(),
                    )
                },
            )
        },
    )
}

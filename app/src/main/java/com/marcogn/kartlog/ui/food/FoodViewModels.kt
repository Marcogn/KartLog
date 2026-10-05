package com.marcogn.kartlog.ui.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.FoodDao
import com.marcogn.kartlog.data.local.entity.FoodVariantEntity
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.domain.model.relocalizing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Una tessera della griglia: i cibi non si ingrigiscono mai, il contatore dice quanti outfit ti mancano. */
data class FoodTile(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val missingOutfits: Int,
    val totalOutfits: Int,
)

@HiltViewModel
class FoodListViewModel @Inject constructor(foodDao: FoodDao) : ViewModel() {

    val tiles: StateFlow<List<FoodTile>> = foodDao.foodGroups().relocalizing().map { groups ->
        groups.map { FoodTile(it.id, localizedName(it.name, it.nameIt), it.imageUrl, it.missingOutfits, it.totalOutfits) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class FoodVariantUi(val name: String, val imageUrl: String?, val boost: List<String>)

data class FoodDetailUiState(
    val name: String = "",
    val variants: List<FoodVariantUi> = emptyList(),
    val sections: List<FoodStandSection> = emptyList(),
    val characters: List<FoodCharacterOutfits> = emptyList(),
    /** Personaggi con outfit a cui questo cibo non ne dà nessuno (o li riporta all'abito base). */
    val withoutOutfit: List<String> = emptyList(),
)

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    foodDao: FoodDao,
) : ViewModel() {

    private val foodGroupId: String = checkNotNull(savedStateHandle["foodGroupId"])

    val uiState: StateFlow<FoodDetailUiState> = combine(
        foodDao.foodGroups().relocalizing(),
        foodDao.variants(foodGroupId),
        foodDao.stands(foodGroupId),
        foodDao.outfits(foodGroupId),
        foodDao.charactersWithOutfits(),
    ) { groups, variants, stands, outfits, characters ->
        val group = groups.firstOrNull { it.id == foodGroupId }
        val (withFood, without) = buildCharacterOutfits(outfits, characters)
        FoodDetailUiState(
            name = group?.let { localizedName(it.name, it.nameIt) }.orEmpty(),
            variants = variants.map(::variantUi),
            sections = buildStandSections(stands, variants),
            characters = withFood,
            withoutOutfit = without,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodDetailUiState())

    private fun variantUi(v: FoodVariantEntity) = FoodVariantUi(localizedName(v.name, v.nameIt), v.imageUrl, v.boost)
}

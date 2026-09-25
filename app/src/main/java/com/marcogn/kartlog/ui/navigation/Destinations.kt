package com.marcogn.kartlog.ui.navigation

import kotlinx.serialization.Serializable

/** Voci del drawer, SPEC §2.1, più le schermate di dettaglio raggiunte da esse. */
sealed interface Destination {

    @Serializable
    data object Home : Destination

    @Serializable
    data object Skin : Destination

    /** Dettaglio personaggio (SPEC §2.3), raggiunto da [Skin]. */
    @Serializable
    data class SkinDetail(val characterId: String) : Destination

    @Serializable
    data object PeachMedallions : Destination

    @Serializable
    data object PSwitches : Destination

    @Serializable
    data object Consigliami : Destination

    @Serializable
    data object Settings : Destination
}

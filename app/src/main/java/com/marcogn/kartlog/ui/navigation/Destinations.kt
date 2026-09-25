package com.marcogn.kartlog.ui.navigation

import kotlinx.serialization.Serializable

/** Voci del drawer, SPEC §2.1. Tutte schermate placeholder in questa fase. */
sealed interface Destination {

    @Serializable
    data object Home : Destination

    @Serializable
    data object Skin : Destination

    @Serializable
    data object PeachMedallions : Destination

    @Serializable
    data object PSwitches : Destination

    @Serializable
    data object Consigliami : Destination

    @Serializable
    data object Settings : Destination
}

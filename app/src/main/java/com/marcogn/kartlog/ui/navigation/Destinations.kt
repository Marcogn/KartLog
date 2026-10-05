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

    /** Pannelli "?" uno per uno, come le Monete Peach (sezione "Collezionabili" del drawer). */
    @Serializable
    data object QuestionPanels : Destination

    /**
     * Mappa dei collezionabili (dati di mkworld-checklist): voce del drawer, e pulsantone in Monete
     * Peach e Pulsanti P. [type] = nome di un [com.marcogn.kartlog.domain.model.MapPointType] da
     * mostrare da solo all'apertura (null = tutti), [focusId] = punto su cui centrare la mappa.
     */
    @Serializable
    data class CollectibleMap(val type: String? = null, val focusId: String? = null) : Destination

    @Serializable
    data object Consigliami : Destination

    /** Dettaglio evento (SPEC §2.5), raggiunto da [Consigliami]. */
    @Serializable
    data class ConsigliamiDetail(val eventId: String) : Destination

    /** Miglior trofeo per evento e cilindrata (SPEC §2.6), raggiunto dal pulsante in Home e dal drawer. */
    @Serializable
    data object Results : Destination

    @Serializable
    data object Settings : Destination
}

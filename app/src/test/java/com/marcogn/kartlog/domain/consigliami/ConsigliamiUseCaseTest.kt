package com.marcogn.kartlog.domain.consigliami

import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.TrophyRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §6.5: test richiesti per l'algoritmo Consigliami, scritti prima della UI (fase 6). */
class ConsigliamiUseCaseTest {

    @Test
    fun `nessun outfit mancante lascia la lista vuota`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = true))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val standFoods = listOf(ConsigliamiStandFood("course1", "fg1"))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        assertTrue(ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events).isEmpty())
    }

    @Test
    fun `un evento senza outfit resta fuori anche con i risultati attivi`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val standFoods = listOf(ConsigliamiStandFood("course1", "fg1"))
        val events = listOf(
            ConsigliamiEvent("useful", EventType.CUP, "Utile", 0, listOf("course1")),
            // Nessun trofeo: improvement 1, quindi con w > 0 il suo score sarebbe positivo.
            ConsigliamiEvent("useless", EventType.CUP, "Inutile", 1, listOf("course2")),
        )

        val ranked = ConsigliamiUseCase.compute(
            characters, outfits, rules, standFoods, events,
            resultsEnabled = true, weight = 0.3,
        )

        assertEquals(listOf("useful"), ranked.map { it.event.id })
    }

    @Test
    fun `un personaggio bloccato non viene mai consigliato`() {
        val characters = listOf(
            ConsigliamiCharacter("mario", 0, unlocked = false), // avrebbe il gain migliore, ma è bloccato
            ConsigliamiCharacter("luigi", 1, unlocked = true),
        )
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
            ConsigliamiOutfit("luigi_a", "luigi", owned = false),
        )
        val rules = listOf(
            ConsigliamiRule("mario_a", "fg1"),
            ConsigliamiRule("mario_b", "fg1"),
            ConsigliamiRule("luigi_a", "fg1"),
        )
        val standFoods = listOf(ConsigliamiStandFood("course1", "fg1"))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)
        val event = groups.single()

        assertEquals("luigi", event.best?.characterId)
        assertEquals(1, event.score.toInt())
    }

    @Test
    fun `un outfit posseduto non conta nel gain`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = true),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_b", "fg1"))
        val standFoods = listOf(ConsigliamiStandFood("course1", "fg1"))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        assertEquals(1, groups.single().score.toInt())
    }

    @Test
    fun `un cibo su due corsi dello stesso evento conta una sola volta per outfit`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val standFoods = listOf(
            ConsigliamiStandFood("course1", "fg1"),
            ConsigliamiStandFood("course2", "fg1"),
        )
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1", "course2")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        assertEquals(1, groups.single().score.toInt())
    }

    @Test
    fun `il tie-break del miglior personaggio guarda prima gli outfit mancanti totali`() {
        val characters = listOf(
            ConsigliamiCharacter("mario", 5, unlocked = true),
            ConsigliamiCharacter("luigi", 2, unlocked = true),
        )
        // Stesso gain (1) per l'evento, ma luigi ha più outfit mancanti in totale -> vince luigi.
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("luigi_a", "luigi", owned = false),
            ConsigliamiOutfit("luigi_b", "luigi", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("luigi_a", "fg1"))
        val standFoods = listOf(ConsigliamiStandFood("course1", "fg1"))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        assertEquals("luigi", groups.single().best?.characterId)
    }

    @Test
    fun `a parita di gain e outfit mancanti il tie-break usa il rosterOrder`() {
        val characters = listOf(
            ConsigliamiCharacter("luigi", 5, unlocked = true),
            ConsigliamiCharacter("mario", 0, unlocked = true),
        )
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("luigi_a", "luigi", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("luigi_a", "fg1"))
        val standFoods = listOf(ConsigliamiStandFood("course1", "fg1"))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        assertEquals("mario", groups.single().best?.characterId)
    }

    @Test
    fun `a parita di score e total gli eventi restano tutti in lista uno dopo l'altro`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val standFoods = listOf(ConsigliamiStandFood("rich_course", "fg1"))
        val events = listOf(
            ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("rich_course", "empty1")),
            ConsigliamiEvent("cup2", EventType.CUP, "Cup 2", 1, listOf("rich_course", "empty2")),
            ConsigliamiEvent("rally1", EventType.RALLY, "Rally 1", 2, listOf("rich_course", "empty3")),
        )

        val ranked = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        assertEquals(listOf("cup1", "cup2", "rally1"), ranked.map { it.event.id })
    }

    @Test
    fun `a parita di tutto una cup precede un rally`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val standFoods = listOf(ConsigliamiStandFood("rich_course", "fg1"))
        val events = listOf(
            ConsigliamiEvent("rally1", EventType.RALLY, "Rally 1", 0, listOf("rich_course")),
            ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 1, listOf("rich_course")),
        )

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        assertEquals(listOf("cup1", "rally1"), groups.map { it.event.id })
    }

    @Test
    fun `più corsi utili precede a parita di score e total`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val standFoods = listOf(
            ConsigliamiStandFood("courseA", "fg1"),
            ConsigliamiStandFood("courseB", "fg1"),
        )
        val events = listOf(
            ConsigliamiEvent("oneStop", EventType.CUP, "One", 0, listOf("courseA", "empty2")),
            ConsigliamiEvent("manyStops", EventType.CUP, "Many", 1, listOf("courseA", "courseB", "empty")),
        )

        val ranked = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        assertEquals(listOf("manyStops", "oneStop"), ranked.map { it.event.id })
    }

    @Test
    fun `l'evento con piu outfit viene prima`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_b", "fg2"))
        val standFoods = listOf(
            ConsigliamiStandFood("course1", "fg1"),
            ConsigliamiStandFood("course2", "fg2"),
        )
        val events = listOf(
            ConsigliamiEvent("e3", EventType.CUP, "E3", 0, listOf("course1")),
            ConsigliamiEvent("e1", EventType.CUP, "E1", 1, listOf("course1", "course2")),
            ConsigliamiEvent("e2", EventType.CUP, "E2", 2, listOf("course1", "course2")),
        )

        val ranked = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)

        // Posizione = indice + 1: 1, 2, 3 anche se e1 ed e2 hanno lo stesso guadagno.
        assertEquals(listOf("e1", "e2", "e3"), ranked.map { it.event.id })
    }

    @Test
    fun `uno stand su una strada non da gain a nessun evento`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        // Lo stand è sulla strada tra i due percorsi dell'evento: nessun percorso lo "possiede".
        val standFoods = listOf(ConsigliamiStandFood(courseId = null, foodGroupId = "fg1"))
        val events = listOf(
            ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1", "course2")),
            ConsigliamiEvent("rally1", EventType.RALLY, "Rally 1", 1, listOf("course1", "course2")),
        )

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events)
        val event = events.first()
        val details = ConsigliamiUseCase.detailFor(event, characters, outfits, rules, standFoods)

        assertTrue(groups.isEmpty())
        assertTrue(details.isEmpty())
    }

    @Test
    fun `piu stand con lo stesso cibo sullo stesso percorso contano una volta`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val standFoods = List(3) { ConsigliamiStandFood("course1", "fg1") }
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val score = ConsigliamiUseCase.compute(characters, outfits, rules, standFoods, events).single()

        assertEquals(1, score.best?.gain)
        assertEquals(listOf(RelevantFood("fg1", "course1")), score.relevantFoods)
    }

    @Test
    fun `il dettaglio elenca solo i personaggi con gain positivo e i loro outfit specifici`() {
        val characters = listOf(
            ConsigliamiCharacter("mario", 0, unlocked = true),
            ConsigliamiCharacter("luigi", 1, unlocked = true), // nessun outfit sbloccabile da questo evento
            ConsigliamiCharacter("peach", 2, unlocked = false), // bloccato, escluso a prescindere dal gain
        )
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
            ConsigliamiOutfit("luigi_a", "luigi", owned = false),
            ConsigliamiOutfit("peach_a", "peach", owned = false),
        )
        val rules = listOf(
            ConsigliamiRule("mario_a", "fg1"),
            ConsigliamiRule("mario_b", "fg2"),
            ConsigliamiRule("luigi_a", "fg_altro"),
            ConsigliamiRule("peach_a", "fg1"),
        )
        val standFoods = listOf(
            ConsigliamiStandFood("course1", "fg1"),
            ConsigliamiStandFood("course1", "fg2"),
        )
        val event = ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1"))

        val details = ConsigliamiUseCase.detailFor(event, characters, outfits, rules, standFoods)

        assertEquals(listOf("mario"), details.map { it.characterId })
        assertEquals(2, details.single().gain)
        assertEquals(setOf("mario_a", "mario_b"), details.single().unlockableOutfitIds.toSet())
    }

    @Test
    fun `il dettaglio dice per ogni outfit con quali cibi e su quali percorsi in ordine di tappa`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        // mario_a si ottiene da due cibi diversi (come Mario Touring).
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_a", "fg2"))
        val standFoods = listOf(
            ConsigliamiStandFood("course2", "fg1"),
            ConsigliamiStandFood("course1", "fg2"),
            ConsigliamiStandFood("course1", "fg_altro"),
            ConsigliamiStandFood("course3", "fg1"),     // percorso fuori dall'evento
            ConsigliamiStandFood(null, "fg1"),          // strada
        )
        val event = ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1", "course2"))

        val outfit = ConsigliamiUseCase.detailFor(event, characters, outfits, rules, standFoods).single().outfits.single()

        assertEquals("mario_a", outfit.outfitId)
        assertEquals(listOf(RelevantFood("fg2", "course1"), RelevantFood("fg1", "course2")), outfit.sources)
    }

    @Test
    fun `con w=1 l'ordinamento dipende solo da improvement`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_b", "fg2"))
        val standFoods = listOf(
            ConsigliamiStandFood("course1", "fg1"),
            ConsigliamiStandFood("course2", "fg2"),
        )
        // e1: gain più alto (2) ma nessun risultato (improvement = 1). e2: gain più basso (1) ma
        // già oro 3 stelle (improvement = 1 - 6/6 = 0). Con w=1 conta solo improvement.
        val events = listOf(
            ConsigliamiEvent("e1", EventType.CUP, "E1", 0, listOf("course1", "course2")),
            ConsigliamiEvent("e2", EventType.CUP, "E2", 1, listOf("course1")),
        )
        val bestRank: (String) -> TrophyRank? = { id -> if (id == "e2") TrophyRank.GOLD_3_STARS else null }

        val groups = ConsigliamiUseCase.compute(
            characters, outfits, rules, standFoods, events,
            resultsEnabled = true, weight = 1.0, bestRankForEvent = bestRank,
        )

        assertEquals(listOf("e1", "e2"), groups.map { it.event.id })
    }

    @Test
    fun `con i risultati disattivati i trofei registrati non contano`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_b", "fg2"))
        val standFoods = listOf(
            ConsigliamiStandFood("course1", "fg1"),
            ConsigliamiStandFood("course2", "fg2"),
        )
        // e1: gain minore (1) ma oro 3 stelle. e2: gain maggiore (2) ma solo bronzo.
        // Risultati disattivati (default): deve vincere il gain, non le stelle.
        val events = listOf(
            ConsigliamiEvent("e1", EventType.CUP, "E1", 0, listOf("course1")),
            ConsigliamiEvent("e2", EventType.CUP, "E2", 1, listOf("course1", "course2")),
        )
        val bestRank: (String) -> TrophyRank? = { id -> if (id == "e1") TrophyRank.GOLD_3_STARS else TrophyRank.BRONZE }

        val groups = ConsigliamiUseCase.compute(
            characters, outfits, rules, standFoods, events,
            bestRankForEvent = bestRank, // resultsEnabled di default è false
        )

        assertEquals(listOf("e2", "e1"), groups.map { it.event.id })
    }

    @Test
    fun `a parita di gain un argento precede un oro 3 stelle (esempio dell'autore)`() {
        // Stesso personaggio, stesso guadagno (1 outfit) su due eventi diversi: il KO fatto solo
        // in argento ha più margine di miglioramento del GP già a oro 3 stelle, quindi va prima.
        val characters = listOf(ConsigliamiCharacter("wario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("wario_a", "wario", owned = false),
            ConsigliamiOutfit("wario_b", "wario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("wario_a", "fg1"), ConsigliamiRule("wario_b", "fg2"))
        val standFoods = listOf(
            ConsigliamiStandFood("course1", "fg1"),
            ConsigliamiStandFood("course2", "fg2"),
        )
        val events = listOf(
            ConsigliamiEvent("cup", EventType.CUP, "Cup", 0, listOf("course1")),
            ConsigliamiEvent("rally", EventType.RALLY, "Rally", 1, listOf("course2")),
        )
        val bestRank: (String) -> TrophyRank? = { id ->
            if (id == "cup") TrophyRank.GOLD_3_STARS else TrophyRank.SILVER
        }

        val groups = ConsigliamiUseCase.compute(
            characters, outfits, rules, standFoods, events,
            resultsEnabled = true, weight = 0.3, bestRankForEvent = bestRank,
        )

        // Senza risultati la Cup vincerebbe lo spareggio typeRank; con i trofei vince il Rally.
        assertEquals(listOf("rally", "cup"), groups.map { it.event.id })
        assertEquals(1.0 - 2.0 / 6.0, groups.first().improvement, 1e-9)
    }
}

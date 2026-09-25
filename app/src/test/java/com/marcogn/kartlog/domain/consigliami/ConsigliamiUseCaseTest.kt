package com.marcogn.kartlog.domain.consigliami

import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.Presence
import com.marcogn.kartlog.domain.model.TrophyRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §6.5: test richiesti per l'algoritmo Consigliami, scritti prima della UI (fase 6). */
class ConsigliamiUseCaseTest {

    @Test
    fun `nessun outfit mancante azzera tutti i gain e la lista e vuota con solo utili`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = true))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)
        val allEvents = groups.flatMap { it.events }

        assertTrue(allEvents.all { it.score == 0.0 && it.best == null })
        assertTrue("il filtro \"Solo utili\" deve svuotare la lista", allEvents.none { it.score > 0 })
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
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)
        val event = groups.single().events.single()

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
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)

        assertEquals(1, groups.single().events.single().score.toInt())
    }

    @Test
    fun `un cibo su due corsi dello stesso evento conta una sola volta per outfit`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val foodCourses = listOf(
            ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE),
            ConsigliamiFoodCourse("fg1", "course2", Presence.ON_COURSE),
        )
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1", "course2")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)

        assertEquals(1, groups.single().events.single().score.toInt())
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
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)

        assertEquals("luigi", groups.single().events.single().best?.characterId)
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
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)

        assertEquals("mario", groups.single().events.single().best?.characterId)
    }

    @Test
    fun `eventi che condividono lo stesso corso ricco finiscono nello stesso gruppo`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "rich_course", Presence.ON_COURSE))
        val events = listOf(
            ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("rich_course", "empty1")),
            ConsigliamiEvent("cup2", EventType.CUP, "Cup 2", 1, listOf("rich_course", "empty2")),
            ConsigliamiEvent("rally1", EventType.RALLY, "Rally 1", 2, listOf("rich_course", "empty3")),
        )

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)

        assertEquals(1, groups.size)
        val group = groups.single()
        assertEquals(1, group.position)
        assertEquals(3, group.events.size)
        assertEquals(setOf("rich_course"), group.commonCourseIds)
    }

    @Test
    fun `a parita di tutto una cup precede un rally`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "rich_course", Presence.ON_COURSE))
        val events = listOf(
            ConsigliamiEvent("rally1", EventType.RALLY, "Rally 1", 0, listOf("rich_course")),
            ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 1, listOf("rich_course")),
        )

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)

        assertEquals(listOf("cup1", "rally1"), groups.single().events.map { it.event.id })
    }

    @Test
    fun `più corsi utili precede a parita di posizione`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val foodCourses = listOf(
            ConsigliamiFoodCourse("fg1", "courseA", Presence.ON_COURSE),
            ConsigliamiFoodCourse("fg1", "courseB", Presence.ON_COURSE),
        )
        val events = listOf(
            ConsigliamiEvent("manyStops", EventType.CUP, "Many", 0, listOf("courseA", "courseB", "empty")),
            ConsigliamiEvent("oneStop", EventType.CUP, "One", 1, listOf("courseA", "empty2")),
        )

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)

        assertEquals("stessa posizione (stesso score e total)", 1, groups.size)
        assertEquals(listOf("manyStops", "oneStop"), groups.single().events.map { it.event.id })
    }

    @Test
    fun `numerazione competition ranking corretta 1 1 3`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_b", "fg2"))
        val foodCourses = listOf(
            ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE),
            ConsigliamiFoodCourse("fg2", "course2", Presence.ON_COURSE),
        )
        val events = listOf(
            ConsigliamiEvent("e1", EventType.CUP, "E1", 0, listOf("course1", "course2")),
            ConsigliamiEvent("e2", EventType.CUP, "E2", 1, listOf("course1", "course2")),
            ConsigliamiEvent("e3", EventType.CUP, "E3", 2, listOf("course1")),
        )

        val groups = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)
        val positionById = groups.flatMap { g -> g.events.map { it.event.id to g.position } }.toMap()

        assertEquals(1, positionById["e1"])
        assertEquals(1, positionById["e2"])
        assertEquals(3, positionById["e3"])
    }

    @Test
    fun `un cibo nearby genera gain solo se includeNearby e attivo`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(ConsigliamiOutfit("mario_a", "mario", owned = false))
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"))
        val foodCourses = listOf(ConsigliamiFoodCourse("fg1", "course1", Presence.NEARBY))
        val events = listOf(ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1")))

        val withoutNearby = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = false)
        val withNearby = ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby = true)

        assertEquals(0, withoutNearby.single().events.single().score.toInt())
        assertEquals(1, withNearby.single().events.single().score.toInt())
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
        val foodCourses = listOf(
            ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE),
            ConsigliamiFoodCourse("fg2", "course1", Presence.ON_COURSE),
        )
        val event = ConsigliamiEvent("cup1", EventType.CUP, "Cup 1", 0, listOf("course1"))

        val details = ConsigliamiUseCase.detailFor(event, characters, outfits, rules, foodCourses, includeNearby = false)

        assertEquals(listOf("mario"), details.map { it.characterId })
        assertEquals(2, details.single().gain)
        assertEquals(setOf("mario_a", "mario_b"), details.single().unlockableOutfitIds.toSet())
    }

    @Test
    fun `con w=1 l'ordinamento dipende solo da improvement`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_b", "fg2"))
        val foodCourses = listOf(
            ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE),
            ConsigliamiFoodCourse("fg2", "course2", Presence.ON_COURSE),
        )
        // e1: gain più alto (2) ma nessun risultato (improvement = 1). e2: gain più basso (1) ma
        // già oro 3 stelle (improvement = 1 - 6/6 = 0). Con w=1 conta solo improvement.
        val events = listOf(
            ConsigliamiEvent("e1", EventType.CUP, "E1", 0, listOf("course1", "course2")),
            ConsigliamiEvent("e2", EventType.CUP, "E2", 1, listOf("course1")),
        )
        val bestRank: (String) -> TrophyRank? = { id -> if (id == "e2") TrophyRank.GOLD_3_STARS else null }

        val groups = ConsigliamiUseCase.compute(
            characters, outfits, rules, foodCourses, events, includeNearby = false,
            resultsEnabled = true, weight = 1.0, bestRankForEvent = bestRank,
        )

        assertEquals(listOf("e1", "e2"), groups.flatMap { it.events }.map { it.event.id })
    }

    @Test
    fun `con i risultati disattivati i trofei registrati non contano`() {
        val characters = listOf(ConsigliamiCharacter("mario", 0, unlocked = true))
        val outfits = listOf(
            ConsigliamiOutfit("mario_a", "mario", owned = false),
            ConsigliamiOutfit("mario_b", "mario", owned = false),
        )
        val rules = listOf(ConsigliamiRule("mario_a", "fg1"), ConsigliamiRule("mario_b", "fg2"))
        val foodCourses = listOf(
            ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE),
            ConsigliamiFoodCourse("fg2", "course2", Presence.ON_COURSE),
        )
        // e1: gain minore (1) ma oro 3 stelle. e2: gain maggiore (2) ma solo bronzo.
        // Risultati disattivati (default): deve vincere il gain, non le stelle.
        val events = listOf(
            ConsigliamiEvent("e1", EventType.CUP, "E1", 0, listOf("course1")),
            ConsigliamiEvent("e2", EventType.CUP, "E2", 1, listOf("course1", "course2")),
        )
        val bestRank: (String) -> TrophyRank? = { id -> if (id == "e1") TrophyRank.GOLD_3_STARS else TrophyRank.BRONZE }

        val groups = ConsigliamiUseCase.compute(
            characters, outfits, rules, foodCourses, events, includeNearby = false,
            bestRankForEvent = bestRank, // resultsEnabled di default è false
        )

        assertEquals(listOf("e2", "e1"), groups.flatMap { it.events }.map { it.event.id })
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
        val foodCourses = listOf(
            ConsigliamiFoodCourse("fg1", "course1", Presence.ON_COURSE),
            ConsigliamiFoodCourse("fg2", "course2", Presence.ON_COURSE),
        )
        val events = listOf(
            ConsigliamiEvent("cup", EventType.CUP, "Cup", 0, listOf("course1")),
            ConsigliamiEvent("rally", EventType.RALLY, "Rally", 1, listOf("course2")),
        )
        val bestRank: (String) -> TrophyRank? = { id ->
            if (id == "cup") TrophyRank.GOLD_3_STARS else TrophyRank.SILVER
        }

        val groups = ConsigliamiUseCase.compute(
            characters, outfits, rules, foodCourses, events, includeNearby = false,
            resultsEnabled = true, weight = 0.3, bestRankForEvent = bestRank,
        )

        // Senza risultati la Cup vincerebbe lo spareggio typeRank; con i trofei vince il Rally.
        assertEquals(listOf("rally", "cup"), groups.flatMap { it.events }.map { it.event.id })
        assertEquals(1.0 - 2.0 / 6.0, groups.first().events.single().improvement, 1e-9)
    }
}

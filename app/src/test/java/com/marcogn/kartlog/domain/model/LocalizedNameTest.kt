package com.marcogn.kartlog.domain.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedNameTest {

    @Test
    fun `lingua italiana con nome italiano disponibile mostra l'italiano`() {
        assertEquals("Esploratore", localizedName("Explorer", "Esploratore", Locale.ITALIAN))
    }

    @Test
    fun `lingua italiana senza nome italiano mostra l'inglese`() {
        assertEquals("Explorer", localizedName("Explorer", null, Locale.ITALIAN))
    }

    @Test
    fun `nome italiano vuoto o solo spazi si comporta come assente`() {
        assertEquals("Explorer", localizedName("Explorer", "", Locale.ITALIAN))
        assertEquals("Explorer", localizedName("Explorer", "   ", Locale.ITALIAN))
    }

    @Test
    fun `lingua non italiana mostra sempre l'inglese, anche se un nome italiano esiste`() {
        assertEquals("Explorer", localizedName("Explorer", "Esploratore", Locale.ENGLISH))
        assertEquals("Explorer", localizedName("Explorer", "Esploratore", Locale.FRENCH))
    }
}

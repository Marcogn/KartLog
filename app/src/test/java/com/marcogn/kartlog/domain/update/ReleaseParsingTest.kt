package com.marcogn.kartlog.domain.update

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixtures: `releases-latest-v1.2.0.json` is the real `GET /repos/Marcogn/KartLog/
 * releases/latest` response of 2026-10-12, with the author's URL fields left out; the `-no-digest-no-body` one is the same with
 * `body` and every asset `digest` set to JSON `null` (what older releases and assets look like).
 */
class ReleaseParsingTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("update/$name")) { "missing fixture $name" }.readText()

    @Test
    fun `parses the real latest-release response`() {
        val release = parseLatestRelease(fixture("releases-latest-v1.2.0.json"))

        assertEquals("v1.2.0", release.tagName)
        assertEquals("KartLog v1.2.0", release.name)
        assertEquals("https://github.com/Marcogn/KartLog/releases/tag/v1.2.0", release.htmlUrl)
        assertFalse(release.isDraft)
        assertFalse(release.isPrerelease)
        val asset = release.assets.single()
        assertEquals("KartLog-1.2.0.apk", asset.name)
        assertEquals(
            "https://github.com/Marcogn/KartLog/releases/download/v1.2.0/KartLog-1.2.0.apk",
            asset.downloadUrl,
        )
        assertEquals(16_311_720L, asset.sizeBytes)
        assertEquals("sha256:c1388a53db1677935c3ff24d158a9bd127de34b0d28c7026a4de8a0bf1133acd", asset.digest)
        assertTrue(release.body!!.startsWith("- **Risultati con la grafica del gioco.**"))
    }

    @Test
    fun `explicit null body and digest decode as null`() {
        val release = parseLatestRelease(fixture("releases-latest-v1.2.0-no-digest-no-body.json"))

        assertNull(release.body)
        assertNull(release.assets.single().digest)
        assertNull(sha256FromDigest(release.assets.single().digest))
    }

    @Test
    fun `the real body becomes headlines and a full changelog link`() {
        val notes = parseReleaseNotes(parseLatestRelease(fixture("releases-latest-v1.2.0.json")).body)

        assertEquals(
            listOf(
                "Risultati con la grafica del gioco.",
                "Dal cibo dell'outfit alla sezione Cibi.",
                "Corretto il \"?\" di \"Rovine del blocco ?\".",
                "Consigliami con la grafica del gioco.",
                "Classifica di Consigliami numerata 1, 2, 3.",
                "Tolto \"Solo utili\".",
                "Dettaglio dell'evento rifatto.",
                "Nuova sezione Cibi.",
                "Consigliami parla di possibilità.",
                "Tolto \"Includi cibi nei dintorni\".",
                "Tutti gli stand Yoshi's e i cibi uno per uno nei dati (seedVersion 9).",
            ),
            notes.headlines,
        )
        assertEquals(
            "https://github.com/Marcogn/KartLog/blob/main/CHANGELOG.md#120---2026-10-05",
            notes.fullChangelogUrl,
        )
    }

    @Test(expected = SerializationException::class)
    fun `a response without tag_name fails instead of guessing`() {
        parseLatestRelease("""{"html_url": "https://example.com", "assets": []}""")
    }

    @Test(expected = SerializationException::class)
    fun `an error body is not a release`() {
        parseLatestRelease("""{"message": "Not Found", "documentation_url": "https://docs.github.com"}""")
    }

    @Test
    fun `null assets decode as an empty list`() {
        val release = parseLatestRelease("""{"tag_name": "v1.0.0", "html_url": "https://x", "assets": null}""")
        assertTrue(release.assets.isEmpty())
    }

    @Test
    fun `sha256FromDigest accepts only a well-formed sha256 digest`() {
        val hex = "C1388A53DB1677935C3FF24D158A9BD127DE34B0D28C7026A4DE8A0BF1133ACD"
        assertEquals(hex.lowercase(), sha256FromDigest("sha256:$hex"))
        assertNull(sha256FromDigest(null))
        assertNull(sha256FromDigest("sha512:$hex"))
        assertNull(sha256FromDigest("sha256:abc"))
        assertNull(sha256FromDigest(hex))
    }
}

package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.RollModifier
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// I libri Project Aon si versionano senza prosa (README §15). Qui si
// verifica che togliere i testi non tocchi le meccaniche, e che
// rimetterli restituisca il libro di partenza.
class TestiEsterniTest {

    private fun scena(id: String) = Scene(
        id = id,
        sceneType = SceneType.TRANSITION,
        genre = "fantasy",
        narrativeText = "Il monastero brucia alle tue spalle.",
        locationName = "Sommerlund",
        choices = listOf(
            Choice(id = "c0", choiceText = "Prosegui verso nord", nextSceneId = "85", minRoll = 0, maxRoll = 4),
            Choice(id = "c1", choiceText = "Nasconditi nel bosco", nextSceneId = "212"),
        ),
        combat = Combat(
            enemyName = "Giak",
            enemyCombatSkill = 14,
            enemyEndurance = 20,
            winSceneId = "90",
        ),
        rollModifiers = listOf(RollModifier(amount = 2)),
    )

    private fun libro(vararg scene: Scene) = Manifest(
        id = "01fftd",
        version = "1.0.0",
        title = "Flight from the Dark",
        description = "",
        language = "en",
        genre = "fantasy",
        scenes = scene.toList(),
    )

    @Test
    fun `lo scheletro non ha piu prosa`() {
        val s = TestiEsterni.scheletro(libro(scena("1")), "01fftd").scenes.single()
        assertEquals("", s.narrativeText)
        assertNull(s.locationName)
        assertTrue(s.choices.all { it.choiceText.isEmpty() })
        assertEquals("", s.combat?.enemyName)
    }

    // Il punto di tutta l'operazione: le meccaniche sono il lavoro di
    // mesi di correzioni e devono sopravvivere intatte.
    @Test
    fun `lo scheletro conserva tutte le meccaniche`() {
        val s = TestiEsterni.scheletro(libro(scena("12")), "01fftd").scenes.single()
        assertEquals("12", s.id)
        assertEquals(listOf("85", "212"), s.choices.map { it.nextSceneId })
        assertEquals(0, s.choices[0].minRoll)
        assertEquals(4, s.choices[0].maxRoll)
        assertEquals(14, s.combat?.enemyCombatSkill)
        assertEquals(20, s.combat?.enemyEndurance)
        assertEquals("90", s.combat?.winSceneId)
        assertEquals(listOf(2), s.rollModifiers.map { it.amount })
    }

    @Test
    fun `lo scheletro dice da dove vengono i testi`() {
        val scheletro = TestiEsterni.scheletro(libro(scena("1")), "01fftd")
        assertEquals("projectaon:01fftd", scheletro.textsFrom)
        assertTrue(TestiEsterni.haTestiEsterni(scheletro))
        assertEquals("01fftd", TestiEsterni.idProjectAonDi(scheletro))
    }

    @Test
    fun `ogni scena numerata porta il link al paragrafo originale`() {
        val s = TestiEsterni.scheletro(libro(scena("42")), "01fftd").scenes.single()
        assertEquals("https://www.projectaon.org/en/xhtml/lw/01fftd/sect42.htm", s.source)
    }

    // Un finale fabbricato o un ramo aggiunto a mano non ha un paragrafo
    // originale: nessun link inventato.
    @Test
    fun `una scena senza id numerico non ha link`() {
        val s = TestiEsterni.scheletro(libro(scena("17-vittoria")), "01fftd").scenes.single()
        assertNull(s.source)
    }

    @Test
    fun `svuotare e riempire restituisce il libro di partenza`() {
        val originale = libro(scena("1"), scena("2"))
        val scheletro = TestiEsterni.scheletro(originale, "01fftd")
        val esito = TestiEsterni.riempi(scheletro, originale)

        assertTrue(esito.note.isEmpty(), "attese zero discrepanze, trovate: ${esito.note}")
        esito.manifest.scenes.forEachIndexed { i, scena ->
            val atteso = originale.scenes[i]
            assertEquals(atteso.narrativeText, scena.narrativeText)
            assertEquals(atteso.locationName, scena.locationName)
            assertEquals(atteso.choices.map { it.choiceText }, scena.choices.map { it.choiceText })
            assertEquals(atteso.combat?.enemyName, scena.combat?.enemyName)
        }
    }

    // Il riempimento prende i TESTI dal libro scaricato ma le MECCANICHE
    // dallo scheletro: se il parser cambia idea su un salto, o se una
    // correzione fatta a mano non c'è nel sorgente, vince lo scheletro.
    @Test
    fun `le meccaniche corrette a mano vincono su quelle del sorgente`() {
        val corretto = TestiEsterni.scheletro(libro(scena("343")), "04tcod")
        // Il libro appena scaricato: stesso testo, ma senza il
        // modificatore e con un salto diverso.
        val grezzo = libro(
            scena("343").copy(
                choices = listOf(Choice(id = "c0", choiceText = "Prosegui verso nord", nextSceneId = "999")),
                rollModifiers = emptyList(),
            ),
        )
        val esito = TestiEsterni.riempi(corretto, grezzo)
        val scena = esito.manifest.scenes.single()

        assertEquals("85", scena.choices[0].nextSceneId, "il salto corretto a mano non deve essere sovrascritto")
        assertEquals(listOf(2), scena.rollModifiers.map { it.amount }, "il modificatore non deve sparire")
        assertEquals("Prosegui verso nord", scena.choices[0].choiceText, "il testo invece viene dal sorgente")
    }

    @Test
    fun `una scena mancante nel sorgente finisce nelle note`() {
        val scheletro = TestiEsterni.scheletro(libro(scena("1"), scena("2")), "01fftd")
        val esito = TestiEsterni.riempi(scheletro, libro(scena("1")))

        assertEquals(1, esito.note.size)
        assertTrue(esito.note.single().contains("scena 2"), esito.note.single())
        assertEquals("", esito.manifest.scenes.last().narrativeText)
    }

    // Riempito non è più uno scheletro: il gioco non deve scambiarlo per
    // un libro muto.
    @Test
    fun `il libro riempito non e piu marcato come esterno`() {
        val scheletro = TestiEsterni.scheletro(libro(scena("1")), "01fftd")
        val esito = TestiEsterni.riempi(scheletro, libro(scena("1")))

        assertNull(esito.manifest.textsFrom)
        assertTrue(!TestiEsterni.haTestiEsterni(esito.manifest))
        // Il link al paragrafo resta: dice comunque da dove viene.
        assertNotNull(esito.manifest.scenes.single().source)
    }
}

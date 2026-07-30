package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRule
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRuleType
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NuovaScenaTest {

    private fun scene(id: String, vararg destinazioni: String) = Scene(
        id = id,
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "testo",
        choices = destinazioni.mapIndexed { i, dest -> Choice("c$i", "vai", nextSceneId = dest) },
    )

    private fun manifest(scenes: List<Scene>, deathSceneId: String? = "morte") = Manifest(
        id = "test", version = "1.0.0", title = "Test", description = "Test",
        language = "en", genre = "FANTASY", deathSceneId = deathSceneId, scenes = scenes,
    )

    @Test
    fun ilProssimoIdENumericoESuccessivoAlPiuAlto() {
        val m = manifest(listOf(scene("1", "2"), scene("2"), scene("7")))
        assertEquals("8", nuovaScenaVuota(m).id)
    }

    @Test
    fun senzaScenePartendoDaZeroIlProssimoIdE1() {
        val m = manifest(emptyList())
        assertEquals("1", nuovaScenaVuota(m).id)
    }

    @Test
    fun unaScenaSoloDiSconfittaNonBloccaLaNumerazione() {
        val m = manifest(listOf(Scene("morte", SceneType.ENDING, "FANTASY", narrativeText = "fine")))
        assertEquals("1", nuovaScenaVuota(m).id)
    }

    @Test
    fun laNuovaScenaEDiTipoTransizioneESenzaTesto() {
        val nuova = nuovaScenaVuota(manifest(emptyList()))
        assertEquals(SceneType.TRANSITION, nuova.sceneType)
        assertTrue(nuova.narrativeText.isEmpty())
    }

    @Test
    fun laReteDiSicurezzaAgganciaAllaSceneDiSconfittaSeNonCESonoUscite() {
        val vuota = scene("5")
        val risultato = conReteDiSicurezza(vuota, "morte")
        assertEquals(listOf("morte"), risultato.choices.map { it.nextSceneId })
    }

    @Test
    fun laReteDiSicurezzaNonTocaUnaScenaConGiaUnaScelta() {
        val conScelta = scene("5", "6")
        val risultato = conReteDiSicurezza(conScelta, "morte")
        assertEquals(conScelta, risultato)
    }

    @Test
    fun laReteDiSicurezzaNonFaNullaSenzaSceneDiSconfittaDichiarata() {
        val vuota = scene("5")
        val risultato = conReteDiSicurezza(vuota, deathSceneId = null)
        assertEquals(vuota, risultato)
    }

    // §19.5: il ricollegamento riscrive OGNI tipo di collegamento del
    // libro che punta a una scena eliminata, lasciando invariato tutto
    // il resto.
    @Test
    fun ilRicollegamentoRiscriveChoiceDisciplineChoiceECombat() {
        val bersaglio = scene("1", "2").copy(
            disciplineChoices = listOf(DisciplineChoice("d1", "SIXTH_SENSE", "testo", nextSceneId = "2")),
            combat = Combat(enemyName = "Nemico", enemyCombatSkill = 10, enemyEndurance = 10, winSceneId = "2", loseSceneId = "2", evadeSceneId = "2"),
        )
        val eliminata = scene("2")
        val m = manifest(listOf(bersaglio, eliminata))

        val risultato = ricollegaRiferimenti(m, daIds = setOf("2"), aId = "9")

        val scenaRicollegata = risultato.scenes.first { it.id == "1" }
        assertEquals("9", scenaRicollegata.choices.single().nextSceneId)
        assertEquals("9", scenaRicollegata.disciplineChoices.single().nextSceneId)
        assertEquals("9", scenaRicollegata.combat!!.winSceneId)
        assertEquals("9", scenaRicollegata.combat!!.loseSceneId)
        assertEquals("9", scenaRicollegata.combat!!.evadeSceneId)
    }

    @Test
    fun ilRicollegamentoRiscriveDeathSceneIdEGlobalRules() {
        val start = scene("1")
        val m = manifest(listOf(start), deathSceneId = "morte").copy(
            globalRules = listOf(GlobalRule(GlobalRuleType.FLAG, "vittoria", ComparisonOperator.EQ, "true", targetSceneId = "morte")),
        )

        val risultato = ricollegaRiferimenti(m, daIds = setOf("morte"), aId = "9")

        assertEquals("9", risultato.deathSceneId)
        assertEquals("9", risultato.globalRules.single().targetSceneId)
    }

    @Test
    fun ilRicollegamentoNonToccaIRiferimentiVersoAltreScene() {
        val invariata = scene("1", "3")
        val m = manifest(listOf(invariata, scene("2"), scene("3")))

        val risultato = ricollegaRiferimenti(m, daIds = setOf("2"), aId = "9")

        assertEquals("3", risultato.scenes.first { it.id == "1" }.choices.single().nextSceneId)
    }
}

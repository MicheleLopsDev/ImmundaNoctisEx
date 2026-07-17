package io.github.luposolitario.immundanoctisex.core.engine

import io.github.luposolitario.immundanoctisex.core.data.model.AutoJumpReason
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.CombatOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.dice.FixedDiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.transition.TransitionEngine
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// MILESTONE FASE 2 (doc/PIANO-SVILUPPO.md): una partita completa del libro
// di esempio SIMULATA DA TERMINALE — il test percorre il grafo, combatte,
// muore e vince, senza alcuna dipendenza Android. Il test recita la parte
// della UI: sceglie le porte e registra le voci del diario-grafo come farà
// l'app.
class GiocataCompletaDelSampleTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun manifest(): Manifest {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("scenes.sample.json")) {
            "content/scenes.sample.json non trovato sul classpath di test"
        }
        return json.decodeFromString(stream.bufferedReader().use { it.readText() })
    }

    private fun newGame(hero: Character) = GameState(
        SessionData(
            saveFormatVersion = 1,
            packageId = "sample-adventure",
            packageVersion = "1.0.0",
            difficulty = Difficulty.NORMAL,
            currentSceneId = "1",
            characters = listOf(hero),
            lastUpdate = 0L,
        ),
    )

    private fun hero(
        combatSkill: Int,
        endurance: Int = 24,
        disciplines: List<String> = emptyList(),
    ) = Character(
        role = CharacterRole.HERO,
        name = "Lupo di prova",
        baseCombatSkill = combatSkill,
        currentEndurance = endurance,
        maxEndurance = endurance,
        kaiDisciplines = disciplines,
    )

    // Segue una scelta normale come farebbe la UI: transizione + voce diario.
    private fun takeChoice(
        state: GameState,
        engine: TransitionEngine,
        manifest: Manifest,
        choiceIndex: Int = 0,
    ) {
        val scene = manifest.scenes.first { it.id == state.currentSceneId }
        val choice = scene.choices[choiceIndex]
        state.addJourneyEntry(
            JourneyEntry(scene.id, scene.narrativeText, Transition.ChoiceTaken(choice.id)),
        )
        engine.transitionTo(state, choice.nextSceneId)
    }

    @Test
    fun percorsoDiVittoriaAttraversoIlCombattimento() {
        val manifest = manifest()
        // Tiri: 0,0 per il combattimento (colpi migliori).
        val dice = FixedDiceRoller(listOf(0, 0))
        val engine = TransitionEngine(manifest, MechanicsExecutor(dice))
        val state = newGame(hero(combatSkill = 19))

        takeChoice(state, engine, manifest) // 1 -> 2
        takeChoice(state, engine, manifest) // 2 -> 3
        takeChoice(state, engine, manifest) // 3 -> 4 (mano sull'arma)

        // Scena 4: combattimento. CS 19 vs 16 -> rapporto +3; tiro 0 -> 16
        // danni al nemico: due round e i teppisti (24) cadono.
        val scene4 = manifest.scenes.first { it.id == "4" }
        val session = CombatSession(state.hero, requireNotNull(scene4.combat), dice)
        val chronicle = session.quickResolve()

        assertEquals(CombatStatus.WIN, session.status)
        assertEquals(2, chronicle.size)
        state.updateHero { session.playerAfterCombat }
        state.addJourneyEntry(
            JourneyEntry(scene4.id, scene4.narrativeText, Transition.CombatResolved(CombatOutcome.WIN)),
        )
        engine.transitionTo(state, requireNotNull(session.destinationSceneId))

        assertEquals("6", state.currentSceneId)
        assertEquals(SceneType.ENDING, manifest.scenes.first { it.id == "6" }.sceneType)
        assertEquals(4, state.snapshot().journey.size)
    }

    @Test
    fun percorsoDiMorteInCombattimento() {
        val manifest = manifest()
        // Eroe debolissimo: CS 5 vs 16 -> rapporto -11; tiro 1 -> morte istantanea.
        val dice = FixedDiceRoller(listOf(1))
        val engine = TransitionEngine(manifest, MechanicsExecutor(dice))
        val state = newGame(hero(combatSkill = 5))

        takeChoice(state, engine, manifest)
        takeChoice(state, engine, manifest)
        takeChoice(state, engine, manifest)

        val scene4 = manifest.scenes.first { it.id == "4" }
        val session = CombatSession(state.hero, requireNotNull(scene4.combat), dice)
        session.quickResolve()

        assertEquals(CombatStatus.LOSE, session.status)
        state.updateHero { session.playerAfterCombat }
        // loseSceneId "7" della scena batte il deathSceneId globale (qui coincidono).
        engine.transitionTo(state, requireNotNull(session.destinationSceneId))

        assertEquals("7", state.currentSceneId)
        assertEquals(manifest.deathSceneId, state.currentSceneId)
        assertEquals(0, state.hero.currentEndurance)
        assertEquals(SceneType.ENDING, manifest.scenes.first { it.id == "7" }.sceneType)
    }

    @Test
    fun percorsoFurtivoConDisciplinaEHealing() {
        val manifest = manifest()
        val engine = TransitionEngine(manifest, MechanicsExecutor(FixedDiceRoller(emptyList())))
        // Eroe ferito con SIXTH_SENSE e HEALING: evita l'imboscata e si cura strada facendo.
        val state = newGame(hero(combatSkill = 15, endurance = 24, disciplines = listOf("SIXTH_SENSE", "HEALING")))
        state.updateHero { it.copy(currentEndurance = 20) }

        takeChoice(state, engine, manifest) // 1 -> 2 (+1 HEALING)
        takeChoice(state, engine, manifest) // 2 -> 3 (+1)

        // Scena 3: la UI mostra la scelta-disciplina solo se posseduta.
        val scene3 = manifest.scenes.first { it.id == "3" }
        val stealth = scene3.disciplineChoices.first { state.hero.kaiDisciplines.contains(it.disciplineId) }
        assertEquals("SIXTH_SENSE", stealth.disciplineId)
        state.addJourneyEntry(
            JourneyEntry(scene3.id, scene3.narrativeText, Transition.DisciplineUsed(stealth.disciplineId, stealth.id)),
        )
        engine.transitionTo(state, stealth.nextSceneId) // 3 -> 5 (+1)
        takeChoice(state, engine, manifest) // 5 -> 6 (+1)

        assertEquals("6", state.currentSceneId)
        assertEquals(24, state.hero.currentEndurance) // 20 + 4 transizioni senza combat
        val transitions = state.snapshot().journey.map { it.transition }
        assertTrue(transitions.any { it is Transition.DisciplineUsed })
    }

    @Test
    fun laMorteBuiltInFuoriDalCombattimentoPortaAllaScenaDiMorte() {
        val manifest = manifest()
        val engine = TransitionEngine(manifest, MechanicsExecutor(FixedDiceRoller(emptyList())))
        val state = newGame(hero(combatSkill = 15, endurance = 24))
        // Ferito a morte da eventi narrativi prima della transizione.
        state.updateHero { it.copy(currentEndurance = 0) }

        val result = engine.transitionTo(state, "2")

        assertEquals("7", result.sceneId)
        assertEquals(AutoJumpReason.BUILT_IN_DEATH, result.autoJumps.single().reason)
    }
}

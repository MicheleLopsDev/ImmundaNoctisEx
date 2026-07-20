package io.github.luposolitario.immundanoctisex.core.engine.inventory

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import io.github.luposolitario.immundanoctisex.core.engine.dice.FixedDiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// offerItem (21/07/2026, Michele: "il pick deve sempre essere di una
// singola cosa per volta, addItem non può funzionare in maniera
// silenziosa"): a differenza di addItem, non è eseguito automaticamente
// dal motore all'arrivo in scena — resta "sul banco" finché il
// giocatore non lo sceglie esplicitamente (AdventureState.pickItem).
class ItemOffersTest {

    private fun scene(mechanics: List<GameMechanic>) = Scene(
        id = "1",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "Prova",
        gameMechanics = mechanics,
    )

    @Test
    fun offerItemVieneEstrattoComeGameItem() {
        val params = buildJsonObject {
            put("itemName", "Broadsword"); put("itemType", "WEAPON"); put("weaponType", "BROADSWORD")
        }
        val offered = ItemOffers.offeredItems(scene(listOf(GameMechanic("offerItem", params))))

        assertEquals(1, offered.size)
        assertEquals("Broadsword", offered.first().name)
        assertEquals(ItemType.WEAPON, offered.first().type)
        assertEquals(WeaponType.BROADSWORD, offered.first().weaponType)
    }

    @Test
    fun addItemNonContaComeOfferta() {
        val params = buildJsonObject { put("itemName", "Meal"); put("itemType", "BACKPACK_ITEM") }
        val offered = ItemOffers.offeredItems(scene(listOf(GameMechanic("addItem", params))))

        assertTrue(offered.isEmpty())
    }

    @Test
    fun tresArmiOfferteRestanoTutteETreDisponibili() {
        val weapons = listOf("Broadsword", "Mace", "Dagger").map { name ->
            GameMechanic("offerItem", buildJsonObject { put("itemName", name); put("itemType", "WEAPON") })
        }

        val offered = ItemOffers.offeredItems(scene(weapons))

        assertEquals(3, offered.size) // sta al giocatore scegliere quali 2, non al motore scartare la terza
    }

    // Il punto centrale del design: MechanicsExecutor non deve applicare
    // offerItem da solo. "offerItem" non è nel `when` di executeSingle,
    // quindi cade nel ramo di default (nessun effetto) — questo test lo
    // blinda: se in futuro qualcuno lo aggiunge per sbaglio al when,
    // rompendo la scelta esplicita, questo test lo segnala.
    @Test
    fun mechanicsExecutorIgnoraOfferItem() {
        val hero = Character(
            role = CharacterRole.HERO,
            name = "Eroe di prova",
            baseCombatSkill = 15,
            currentEndurance = 20,
            maxEndurance = 20,
        )
        val state = GameState(
            SessionData(
                saveFormatVersion = 1,
                packageId = "sample",
                packageVersion = "1.0",
                difficulty = Difficulty.NORMAL,
                currentSceneId = "1",
                characters = listOf(hero),
                lastUpdate = 0L,
            ),
        )
        val params = buildJsonObject { put("itemName", "Broadsword"); put("itemType", "WEAPON") }

        MechanicsExecutor(FixedDiceRoller(emptyList())).execute(state, listOf(GameMechanic("offerItem", params)))

        assertTrue(state.hero.inventory.isEmpty())
    }
}

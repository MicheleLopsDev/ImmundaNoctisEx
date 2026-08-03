package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.RollCondition
import io.github.luposolitario.immundanoctisex.core.data.model.RollConditionType
import io.github.luposolitario.immundanoctisex.core.data.model.RollModifier
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Il controllo locale della maschera dei modificatori al tiro. Il
// disegno si guarda a mano nell'editor: qui c'è solo cosa passa e cosa
// no al salvataggio.
class RollModifiersCardTest {

    @Test
    fun `nessun modificatore non e un errore`() {
        assertNull(erroreNeiModificatori(emptyList()))
    }

    // Il caso senza condizione esiste davvero nei libri ("Pick a number
    // from the Random Number Table and add 5 to it").
    @Test
    fun `un modificatore incondizionato va bene`() {
        assertNull(erroreNeiModificatori(listOf(RollModifier(amount = 5))))
    }

    @Test
    fun `un malus e valido quanto un bonus`() {
        assertNull(erroreNeiModificatori(listOf(RollModifier(amount = -3))))
    }

    @Test
    fun `una disciplina scelta va bene`() {
        val modificatore = RollModifier(
            amount = 2,
            condition = RollCondition(RollConditionType.DISCIPLINE, values = listOf("SIXTH_SENSE")),
        )
        assertNull(erroreNeiModificatori(listOf(modificatore)))
    }

    @Test
    fun `due discipline in OR vanno bene`() {
        val modificatore = RollModifier(
            amount = 2,
            condition = RollCondition(
                RollConditionType.DISCIPLINE,
                values = listOf("MIND_OVER_MATTER", "MINDBLAST"),
            ),
        )
        assertNull(erroreNeiModificatori(listOf(modificatore)))
    }

    @Test
    fun `un valore lasciato vuoto e un errore`() {
        val modificatore = RollModifier(
            amount = 2,
            condition = RollCondition(RollConditionType.ITEM, values = listOf("")),
        )
        val errore = erroreNeiModificatori(listOf(modificatore))
        assertNotNull(errore)
        assertTrue(errore.contains("modificatore 1"))
    }

    @Test
    fun `una lista di valori vuota e un errore`() {
        val modificatore = RollModifier(
            amount = 2,
            condition = RollCondition(RollConditionType.FLAG, values = emptyList()),
        )
        assertNotNull(erroreNeiModificatori(listOf(modificatore)))
    }

    @Test
    fun `la Resistenza vuole operatore e soglia`() {
        val senzaSoglia = RollModifier(
            amount = -2,
            condition = RollCondition(RollConditionType.ENDURANCE, operator = ComparisonOperator.LT),
        )
        assertNotNull(erroreNeiModificatori(listOf(senzaSoglia)))

        val senzaOperatore = RollModifier(
            amount = -2,
            condition = RollCondition(RollConditionType.ENDURANCE, threshold = 10),
        )
        assertNotNull(erroreNeiModificatori(listOf(senzaOperatore)))

        val completo = RollModifier(
            amount = -2,
            condition = RollCondition(
                RollConditionType.ENDURANCE,
                operator = ComparisonOperator.LT,
                threshold = 10,
            ),
        )
        assertNull(erroreNeiModificatori(listOf(completo)))
    }

    // Il messaggio deve dire QUALE riga: con tre modificatori a schermo,
    // "c'è un errore" da solo non aiuta a trovarlo.
    @Test
    fun `l'errore dice quale modificatore e sbagliato`() {
        val buono = RollModifier(amount = 2)
        val rotto = RollModifier(
            amount = 1,
            condition = RollCondition(RollConditionType.ITEM, values = listOf("  ")),
        )
        val errore = erroreNeiModificatori(listOf(buono, buono, rotto))
        assertNotNull(errore)
        assertTrue(errore.contains("modificatore 3"), "atteso il numero 3, trovato: $errore")
    }

    @Test
    fun `i simboli degli operatori sono quelli del JSON`() {
        assertTrue(simbolo(ComparisonOperator.LT) == "<")
        assertTrue(simbolo(ComparisonOperator.GTE) == ">=")
        assertTrue(simbolo(ComparisonOperator.NEQ) == "!=")
    }
}

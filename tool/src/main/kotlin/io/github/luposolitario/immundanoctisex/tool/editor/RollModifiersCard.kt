package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.RollCondition
import io.github.luposolitario.immundanoctisex.core.data.model.RollConditionType
import io.github.luposolitario.immundanoctisex.core.data.model.RollModifier

// Maschera dei `rollModifiers` di una scena: il bonus/malus che si somma
// al tiro della Tabella dei Numeri Casuali PRIMA di guardare quale
// intervallo lo copre. Rimandata di proposito quando la feature è nata
// (01/08/2026, "si correggono dall'editor JSON grezzo; se dopo il primo
// uso reale risultasse scomodo, una maschera è un lavoro a parte") —
// ripresa ora che i 5 libri convertiti ne portano 48 veri da rileggere.
//
// File a parte e non dentro `SceneEditorScreen.kt`: quello è già a 864
// righe, ben oltre la soglia d'allarme del progetto.

// L'etichetta del tipo di condizione. "Sempre" è `condition == null`:
// esiste davvero nei libri ("Pick a number ... and add 5 to it").
private const val SEMPRE = "Sempre"

private fun etichetta(tipo: RollConditionType?): String = when (tipo) {
    null -> SEMPRE
    RollConditionType.DISCIPLINE -> "Se hai la disciplina"
    RollConditionType.ITEM -> "Se possiedi l'oggetto"
    RollConditionType.FLAG -> "Se il flag è posto"
    RollConditionType.ENDURANCE -> "Se la Resistenza è"
}

// Il segno si legge dal numero, non da un controllo a parte: "-3" si
// scrive com'è scritto nel libro ("deduct 3").
@Composable
fun RollModifiersSection(
    modificatori: List<RollModifier>,
    scenaHaScelteATiro: Boolean,
    onChange: (List<RollModifier>) -> Unit,
) {
    SezioneScheda("Modificatori al tiro del dado") {
        if (modificatori.isNotEmpty() && !scenaHaScelteATiro) {
            // Non blocca: si scrive anche in avanti, e le scelte a tiro
            // possono arrivare dopo. Stesso principio della validazione
            // locale delle destinazioni (§7.3).
            Text(
                "Questa scena non ha scelte con un intervallo di tiro: " +
                    "così il modificatore non verrebbe mai applicato.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        modificatori.forEachIndexed { index, modificatore ->
            RigaModificatore(
                modificatore = modificatore,
                onChange = { nuovo ->
                    onChange(modificatori.toMutableList().also { it[index] = nuovo })
                },
                onRimuovi = { onChange(modificatori.filterIndexed { i, _ -> i != index }) },
            )
        }
        Button(onClick = { onChange(modificatori + RollModifier(amount = 2)) }) {
            Text("+ Aggiungi modificatore")
        }
    }
}

@Composable
private fun RigaModificatore(
    modificatore: RollModifier,
    onChange: (RollModifier) -> Unit,
    onRimuovi: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Testo libero e non un numero: mentre si scrive "-" da solo
            // non è un intero, e un campo che rifiuta il segno meno
            // renderebbe impossibile digitare un malus.
            var testoAmount by remember(modificatore) { mutableStateOf(modificatore.amount.toString()) }
            OutlinedTextField(
                value = testoAmount,
                onValueChange = { nuovo ->
                    testoAmount = nuovo
                    nuovo.toIntOrNull()?.let { onChange(modificatore.copy(amount = it)) }
                },
                modifier = Modifier.width(110.dp),
                label = { Text("Somma") },
                isError = testoAmount.toIntOrNull() == null,
            )
            TipoCondizioneDropdown(
                tipo = modificatore.condition?.type,
                onValueChange = { nuovo -> onChange(modificatore.copy(condition = condizioneVuota(nuovo))) },
            )
            Button(onClick = onRimuovi) { Text("✕") }
        }
        when (modificatore.condition?.type) {
            RollConditionType.DISCIPLINE, RollConditionType.ITEM, RollConditionType.FLAG ->
                ValoriInOr(
                    condizione = modificatore.condition!!,
                    onChange = { onChange(modificatore.copy(condition = it)) },
                )
            RollConditionType.ENDURANCE ->
                SogliaResistenza(
                    condizione = modificatore.condition!!,
                    onChange = { onChange(modificatore.copy(condition = it)) },
                )
            null -> Unit
        }
    }
}

// Cambiando tipo si riparte da una condizione vuota di quel tipo: i
// campi del tipo precedente non hanno senso nel nuovo (una soglia di
// Resistenza su una condizione DISCIPLINE sarebbe un dato morto nel
// JSON).
private fun condizioneVuota(tipo: RollConditionType?): RollCondition? = when (tipo) {
    null -> null
    RollConditionType.ENDURANCE -> RollCondition(type = tipo, operator = ComparisonOperator.LT, threshold = 10)
    RollConditionType.DISCIPLINE -> RollCondition(type = tipo, values = listOf(Discipline.entries.first().name))
    else -> RollCondition(type = tipo, values = listOf(""))
}

// I valori sono in OR: basta soddisfarne uno ("either Mind Over Matter
// or Mindblast").
@Composable
private fun ValoriInOr(condizione: RollCondition, onChange: (RollCondition) -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
        condizione.values.forEachIndexed { index, valore ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                if (index > 0) Text("oppure", style = MaterialTheme.typography.bodySmall)
                if (condizione.type == RollConditionType.DISCIPLINE) {
                    DisciplinaInOrDropdown(
                        valore = valore,
                        onValueChange = { nuovo ->
                            onChange(condizione.copy(values = condizione.values.toMutableList().also { it[index] = nuovo }))
                        },
                    )
                } else {
                    OutlinedTextField(
                        value = valore,
                        onValueChange = { nuovo ->
                            onChange(condizione.copy(values = condizione.values.toMutableList().also { it[index] = nuovo }))
                        },
                        modifier = Modifier.width(260.dp),
                        label = {
                            Text(if (condizione.type == RollConditionType.ITEM) "Nome oggetto" else "Nome flag")
                        },
                        isError = valore.isBlank(),
                    )
                }
                if (condizione.values.size > 1) {
                    Button(onClick = {
                        onChange(condizione.copy(values = condizione.values.filterIndexed { i, _ -> i != index }))
                    }) {
                        Text("✕")
                    }
                }
            }
        }
        Button(onClick = {
            val nuovo = if (condizione.type == RollConditionType.DISCIPLINE) Discipline.entries.first().name else ""
            onChange(condizione.copy(values = condizione.values + nuovo))
        }) {
            Text("+ oppure")
        }
    }
}

@Composable
private fun SogliaResistenza(condizione: RollCondition, onChange: (RollCondition) -> Unit) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OperatoreDropdown(
            valore = condizione.operator ?: ComparisonOperator.LT,
            onValueChange = { onChange(condizione.copy(operator = it)) },
        )
        var testoSoglia by remember(condizione) { mutableStateOf(condizione.threshold?.toString() ?: "") }
        OutlinedTextField(
            value = testoSoglia,
            onValueChange = { nuovo ->
                testoSoglia = nuovo
                nuovo.toIntOrNull()?.let { onChange(condizione.copy(threshold = it)) }
            },
            modifier = Modifier.width(120.dp),
            label = { Text("Soglia") },
            isError = testoSoglia.toIntOrNull() == null,
        )
    }
}

@Composable
private fun TipoCondizioneDropdown(tipo: RollConditionType?, onValueChange: (RollConditionType?) -> Unit) {
    var aperto by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { aperto = true }) { Text(etichetta(tipo)) }
        DropdownMenu(expanded = aperto, onDismissRequest = { aperto = false }) {
            // `null` in testa: "Sempre" è il caso più semplice, non una
            // voce nascosta in fondo.
            (listOf(null) + RollConditionType.entries).forEach { voce ->
                DropdownMenuItem(
                    text = { Text(etichetta(voce)) },
                    onClick = {
                        onValueChange(voce)
                        aperto = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DisciplinaInOrDropdown(valore: String, onValueChange: (String) -> Unit) {
    var aperto by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { aperto = true }) { Text(valore.ifBlank { "—" }) }
        DropdownMenu(expanded = aperto, onDismissRequest = { aperto = false }) {
            Discipline.entries.forEach { disciplina ->
                DropdownMenuItem(
                    text = { Text(disciplina.name) },
                    onClick = {
                        onValueChange(disciplina.name)
                        aperto = false
                    },
                )
            }
        }
    }
}

// I simboli (`<`, `>=`) sono quelli con cui l'operatore è serializzato
// nel JSON: chi legge la maschera vede la stessa cosa che troverebbe nel
// file, non una traduzione a parte da tenere allineata.
@Composable
private fun OperatoreDropdown(valore: ComparisonOperator, onValueChange: (ComparisonOperator) -> Unit) {
    var aperto by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { aperto = true }) { Text(simbolo(valore)) }
        DropdownMenu(expanded = aperto, onDismissRequest = { aperto = false }) {
            ComparisonOperator.entries.forEach { operatore ->
                DropdownMenuItem(
                    text = { Text(simbolo(operatore)) },
                    onClick = {
                        onValueChange(operatore)
                        aperto = false
                    },
                )
            }
        }
    }
}

// Controllo locale prima di salvare, sullo stampo di `salvaDaMaschera`:
// un messaggio per volta, in italiano, e solo su ciò che rende il
// modificatore inservibile. Il controllo completo (ID di disciplina
// esistente, ecc.) resta al `RollModifierValidator` di `:core:data`,
// che vale per qualunque libro comunque prodotto.
// Restituisce null se va tutto bene.
internal fun erroreNeiModificatori(modificatori: List<RollModifier>): String? {
    modificatori.forEachIndexed { index, modificatore ->
        val numero = index + 1
        val condizione = modificatore.condition ?: return@forEachIndexed
        when (condizione.type) {
            RollConditionType.ENDURANCE -> {
                if (condizione.operator == null || condizione.threshold == null) {
                    return "il modificatore $numero sulla Resistenza deve avere operatore e soglia"
                }
            }
            else -> {
                if (condizione.values.isEmpty() || condizione.values.any { it.isBlank() }) {
                    return "il modificatore $numero deve avere almeno un valore, e nessuno vuoto"
                }
            }
        }
    }
    return null
}

internal fun simbolo(operatore: ComparisonOperator): String = when (operatore) {
    ComparisonOperator.EQ -> "=="
    ComparisonOperator.NEQ -> "!="
    ComparisonOperator.GTE -> ">="
    ComparisonOperator.LTE -> "<="
    ComparisonOperator.GT -> ">"
    ComparisonOperator.LT -> "<"
}

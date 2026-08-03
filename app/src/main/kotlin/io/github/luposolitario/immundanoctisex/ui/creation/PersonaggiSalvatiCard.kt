package io.github.luposolitario.immundanoctisex.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.PersonaggioSalvato
import io.github.luposolitario.immundanoctisex.core.engine.character.TrasportoPersonaggio
import io.github.luposolitario.immundanoctisex.core.engine.rank.KaiRank
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// L'elenco dei personaggi che possono riprendere il viaggio in un altro
// libro (03/08/2026, Michele: "invece di iniziare con un nuovo pg
// importo il pg di un'altra avventura").
//
// Non compare affatto quando non ce n'è nessuno: alla prima partita la
// schermata di creazione resta identica a com'era.
@Composable
fun PersonaggiSalvatiCard(
    personaggi: List<PersonaggioSalvato>,
    onImporta: (PersonaggioSalvato, List<Discipline>) -> Unit,
    onElimina: (PersonaggioSalvato) -> Unit,
) {
    if (personaggi.isEmpty()) return

    var daImportare by remember { mutableStateOf<PersonaggioSalvato?>(null) }
    var daEliminare by remember { mutableStateOf<PersonaggioSalvato?>(null) }

    daImportare?.let { salvato ->
        ScegliDisciplineDialog(
            salvato = salvato,
            onConferma = { discipline ->
                daImportare = null
                onImporta(salvato, discipline)
            },
            onAnnulla = { daImportare = null },
        )
    }

    daEliminare?.let { salvato ->
        AlertDialog(
            onDismissRequest = { daEliminare = null },
            title = { Text("Eliminare ${salvato.nome}?") },
            text = {
                Text(
                    "Il personaggio sparisce con tutta la sua storia " +
                        "(${salvato.libriCompletati.size} libri completati). Le partite salvate restano.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onElimina(salvato)
                    daEliminare = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { daEliminare = null }) { Text("Annulla") }
            },
        )
    }

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Riprendi un personaggio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Porta con sé statistiche, discipline ed equipaggiamento. " +
                    "Le statistiche non si tirano di nuovo: sono le sue per tutta la serie.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            personaggi.forEach { salvato ->
                HorizontalDivider()
                RigaPersonaggio(
                    salvato = salvato,
                    onRiprendi = { daImportare = salvato },
                    onElimina = { daEliminare = salvato },
                )
            }
        }
    }
}

@Composable
private fun RigaPersonaggio(
    salvato: PersonaggioSalvato,
    onRiprendi: () -> Unit,
    onElimina: () -> Unit,
) {
    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val rango = KaiRank.fromDisciplineCount(salvato.personaggio.kaiDisciplines.size)
    val ultimo = salvato.libriCompletati.lastOrNull()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                // Nome, rango e data: due eroi possono chiamarsi uguale,
                // la data è ciò che li distingue a colpo d'occhio.
                "${salvato.nome} · ${rango.name} · creato il ${formato.format(Date(salvato.creatoIl))}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                buildString {
                    append("Da \"${salvato.libroOrigineTitolo}\"")
                    if (salvato.libriCompletati.isEmpty()) {
                        append(" · nessun libro completato")
                    } else {
                        append(" · ${salvato.libriCompletati.size} libri completati")
                        ultimo?.let { append(", ultimo \"${it.titolo}\"") }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (salvato.disciplineDaAssegnare > 0) {
                Text(
                    "Ha diritto a ${salvato.disciplineDaAssegnare} nuova/e Disciplina Kai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Button(onClick = onRiprendi) { Text("Riprendi") }
        TextButton(onClick = onElimina) { Text("✕") }
    }
}

// La scelta della disciplina guadagnata, al momento di iniziare il
// libro nuovo: è l'unico momento in cui si ha davanti l'elenco di
// quelle che mancano. Senza discipline da assegnare non chiede nulla e
// si parte diretti.
@Composable
private fun ScegliDisciplineDialog(
    salvato: PersonaggioSalvato,
    onConferma: (List<Discipline>) -> Unit,
    onAnnulla: () -> Unit,
) {
    val spettanti = salvato.disciplineDaAssegnare
    if (spettanti == 0) {
        onConferma(emptyList())
        return
    }
    val mancanti = remember(salvato.id) { TrasportoPersonaggio.disciplineMancanti(salvato.personaggio) }
    var scelte by remember(salvato.id) { mutableStateOf<List<Discipline>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onAnnulla,
        title = { Text("${salvato.nome} sale di grado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (spettanti == 1) {
                        "Ha completato un libro: scegli la Disciplina Kai che ha imparato."
                    } else {
                        "Ha completato ${salvato.libriCompletati.size} libri: scegli $spettanti Discipline Kai."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                // Una per riga, non a griglia: le discipline mancanti
                // sono al massimo cinque e i loro nomi sono lunghi —
                // incolonnate si leggono, affiancate si troncherebbero.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    mancanti.forEach { disciplina ->
                        val selezionata = disciplina in scelte
                        FilterChip(
                            selected = selezionata,
                            onClick = {
                                scelte = when {
                                    selezionata -> scelte - disciplina
                                    scelte.size < spettanti -> scelte + disciplina
                                    else -> scelte
                                }
                            },
                            label = { Text(disciplineNomeBreve(disciplina)) },
                        )
                    }
                }
                Text(
                    "Scelte ${scelte.size} di $spettanti",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(enabled = scelte.size == spettanti, onClick = { onConferma(scelte) }) {
                Text("Comincia l'avventura")
            }
        },
        dismissButton = { OutlinedButton(onClick = onAnnulla) { Text("Annulla") } },
    )
}

// Il nome leggibile viene da strings.xml tramite il catalogo della
// creazione; se manca si mostra l'id canonico invece di uno spazio
// vuoto.
@Composable
private fun disciplineNomeBreve(disciplina: Discipline): String =
    disciplineName(disciplina.name)?.let { androidx.compose.ui.res.stringResource(it) } ?: disciplina.name

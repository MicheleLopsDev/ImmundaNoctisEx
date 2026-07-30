package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Pannello di editing di una scena (doc/EDITOR.md §7). La vista Maschera
// copre narrativeText, choices, disciplineChoices, backgroundImage/
// npcImage e combat. backgroundImage/npcImage/combat.enemyImage restano
// campi di testo libero (serve poter scrivere `url:` a mano), ma con
// suggerimenti dal catalogo chiuso e anteprima grafica (§15.3,
// `CampoImmagineConAnteprima` più sotto) — il catalogo, prima confinato
// in `:app`, ora ha una copia in `:tool` letta da `StaticResourceCatalog`
// (§15.1).
// Validazione locale (§7.3): narrativeText, ogni scelta/scelta-disciplina
// (testo + destinazione, disciplina valida per le seconde) e i campi
// obbligatori di un eventuale combattimento devono essere valorizzati
// per salvare — una destinazione che punta a una scena non ancora
// creata NON blocca (si vede rossa sulla mappa dopo).
private val jsonScena = Json { prettyPrint = true }

@Composable
fun SceneEditorScreen(scene: Scene, tutteLeScene: List<Scene>, onSalva: (Scene) -> Unit, onAnnulla: () -> Unit) {
    var vistaJson by remember(scene.id) { mutableStateOf(false) }

    var narrativeText by remember(scene.id) { mutableStateOf(scene.narrativeText) }
    var choices by remember(scene.id) { mutableStateOf(scene.choices) }
    var disciplineChoices by remember(scene.id) { mutableStateOf(scene.disciplineChoices) }
    var backgroundImage by remember(scene.id) { mutableStateOf(scene.backgroundImage ?: "") }
    var npcImage by remember(scene.id) { mutableStateOf(scene.npcImage ?: "") }

    var haCombattimento by remember(scene.id) { mutableStateOf(scene.combat != null) }
    var combatEnemyName by remember(scene.id) { mutableStateOf(scene.combat?.enemyName ?: "") }
    var combatEnemyImage by remember(scene.id) { mutableStateOf(scene.combat?.enemyImage ?: "") }
    var combatSkill by remember(scene.id) { mutableStateOf(scene.combat?.enemyCombatSkill?.toString() ?: "") }
    var combatEndurance by remember(scene.id) { mutableStateOf(scene.combat?.enemyEndurance?.toString() ?: "") }
    var combatWinSceneId by remember(scene.id) { mutableStateOf(scene.combat?.winSceneId ?: "") }
    var combatLoseSceneId by remember(scene.id) { mutableStateOf(scene.combat?.loseSceneId ?: "") }
    var combatEvadeSceneId by remember(scene.id) { mutableStateOf(scene.combat?.evadeSceneId ?: "") }
    var combatImmune by remember(scene.id) { mutableStateOf(scene.combat?.immuneToMindblast ?: false) }
    var combatEvadeAfterRound by remember(scene.id) { mutableStateOf((scene.combat?.evadeAfterRound ?: 0).toString()) }

    var jsonTesto by remember(scene.id) { mutableStateOf(jsonScena.encodeToString(Scene.serializer(), scene)) }
    var errore by remember(scene.id) { mutableStateOf<String?>(null) }

    // Ricostruisce la scena dallo stato corrente della maschera — usata
    // sia per salvare sia per passare alla vista JSON senza perdere le
    // modifiche già fatte.
    fun sceneDallaMaschera(): Scene = scene.copy(
        narrativeText = narrativeText,
        choices = choices,
        disciplineChoices = disciplineChoices,
        backgroundImage = backgroundImage.ifBlank { null },
        npcImage = npcImage.ifBlank { null },
        combat = if (haCombattimento) {
            Combat(
                enemyName = combatEnemyName,
                enemyImage = combatEnemyImage.ifBlank { null },
                enemyCombatSkill = combatSkill.toIntOrNull() ?: 0,
                enemyEndurance = combatEndurance.toIntOrNull() ?: 0,
                immuneToMindblast = combatImmune,
                evadeAfterRound = combatEvadeAfterRound.toIntOrNull() ?: 0,
                winSceneId = combatWinSceneId,
                loseSceneId = combatLoseSceneId.ifBlank { null },
                evadeSceneId = combatEvadeSceneId.ifBlank { null },
            )
        } else {
            null
        },
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Scena ${scene.id}", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { vistaJson = false }) { Text("Maschera") }
                Button(onClick = {
                    // Passando alla vista JSON si riparte SEMPRE dallo stato
                    // corrente della maschera, non dalla scena originale —
                    // altrimenti le modifiche fatte lì sparirebbero.
                    jsonTesto = jsonScena.encodeToString(Scene.serializer(), sceneDallaMaschera())
                    vistaJson = true
                }) { Text("JSON") }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (errore != null) {
            Text(
                "Errore: $errore",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer).padding(8.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (vistaJson) {
                OutlinedTextField(
                    value = jsonTesto,
                    onValueChange = { jsonTesto = it },
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    label = { Text("Scena (JSON completo)") },
                )
            } else {
                OutlinedTextField(
                    value = narrativeText,
                    onValueChange = { narrativeText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    label = { Text("Testo narrato") },
                )
                Spacer(Modifier.height(16.dp))

                Text("Immagini", style = MaterialTheme.typography.titleMedium)
                // Un'anteprima per ciascuna, ognuna nel proprio posto
                // (§15.3, Michele: "se ci sono immagini nella scena
                // devono essere caricate e reindirizzate anche
                // graficamente nella scheda della scena") — a differenza
                // del nodo della mappa (§15.3, una sola immagine per
                // priorità), qui possono coesistere tutte e tre.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoImmagineConAnteprima(
                        valore = backgroundImage,
                        etichetta = "Sfondo (static:id o url:...)",
                        suggerimenti = StaticResourceCatalog.registry.locations.map { it.id },
                        onValueChange = { backgroundImage = it },
                        modifier = Modifier.weight(1f),
                    )
                    CampoImmagineConAnteprima(
                        valore = npcImage,
                        etichetta = "Ritratto NPC (static:id o url:...)",
                        suggerimenti = StaticResourceCatalog.registry.npcs,
                        onValueChange = { npcImage = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Combattimento", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { haCombattimento = !haCombattimento }) {
                        Text(if (haCombattimento) "✕ Rimuovi combattimento" else "+ Aggiungi combattimento")
                    }
                }
                if (haCombattimento) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = combatEnemyName,
                            onValueChange = { combatEnemyName = it },
                            modifier = Modifier.weight(2f),
                            label = { Text("Nome nemico") },
                        )
                        CampoImmagineConAnteprima(
                            valore = combatEnemyImage,
                            etichetta = "Immagine (static:/url:)",
                            suggerimenti = StaticResourceCatalog.registry.enemies,
                            onValueChange = { combatEnemyImage = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = combatSkill,
                            onValueChange = { combatSkill = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Combattività") },
                        )
                        OutlinedTextField(
                            value = combatEndurance,
                            onValueChange = { combatEndurance = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Resistenza") },
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = combatWinSceneId,
                            onValueChange = { combatWinSceneId = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Scena se vinci") },
                        )
                        OutlinedTextField(
                            value = combatLoseSceneId,
                            onValueChange = { combatLoseSceneId = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Scena se perdi (opz.)") },
                        )
                        OutlinedTextField(
                            value = combatEvadeSceneId,
                            onValueChange = { combatEvadeSceneId = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Scena se fuggi (opz.)") },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = combatEvadeAfterRound,
                            onValueChange = { combatEvadeAfterRound = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Fuga disponibile dopo N round") },
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = combatImmune, onCheckedChange = { combatImmune = it })
                            Text("Immune a MINDBLAST")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text("Scelte", style = MaterialTheme.typography.titleMedium)
                choices.forEachIndexed { index, choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = choice.choiceText,
                            onValueChange = { nuovo -> choices = choices.toMutableList().also { it[index] = choice.copy(choiceText = nuovo) } },
                            modifier = Modifier.weight(2f),
                            label = { Text("Testo scelta") },
                        )
                        DestinazioneField(
                            valore = choice.nextSceneId,
                            tutteLeScene = tutteLeScene,
                            onValueChange = { nuovo -> choices = choices.toMutableList().also { it[index] = choice.copy(nextSceneId = nuovo) } },
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { choices = choices.filterIndexed { i, _ -> i != index } }) {
                            Text("✕")
                        }
                    }
                }
                Button(onClick = {
                    choices = choices + Choice(id = "c${choices.size}", choiceText = "", nextSceneId = "")
                }) {
                    Text("+ Aggiungi scelta")
                }
                Spacer(Modifier.height(16.dp))

                Text("Scelte legate a una disciplina", style = MaterialTheme.typography.titleMedium)
                disciplineChoices.forEachIndexed { index, scelta ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DisciplinaDropdown(
                            valore = scelta.disciplineId,
                            onValueChange = { nuovo ->
                                disciplineChoices = disciplineChoices.toMutableList().also { it[index] = scelta.copy(disciplineId = nuovo) }
                            },
                        )
                        OutlinedTextField(
                            value = scelta.choiceText,
                            onValueChange = { nuovo ->
                                disciplineChoices = disciplineChoices.toMutableList().also { it[index] = scelta.copy(choiceText = nuovo) }
                            },
                            modifier = Modifier.weight(2f),
                            label = { Text("Testo scelta") },
                        )
                        DestinazioneField(
                            valore = scelta.nextSceneId,
                            tutteLeScene = tutteLeScene,
                            onValueChange = { nuovo ->
                                disciplineChoices = disciplineChoices.toMutableList().also { it[index] = scelta.copy(nextSceneId = nuovo) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { disciplineChoices = disciplineChoices.filterIndexed { i, _ -> i != index } }) {
                            Text("✕")
                        }
                    }
                }
                Button(onClick = {
                    disciplineChoices = disciplineChoices + DisciplineChoice(
                        id = "d${disciplineChoices.size}",
                        disciplineId = Discipline.entries.first().name,
                        choiceText = "",
                        nextSceneId = "",
                    )
                }) {
                    Text("+ Aggiungi scelta per disciplina")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAnnulla) { Text("Ritorna") }
            Button(onClick = {
                if (vistaJson) {
                    salvaDaJson(jsonTesto, onSalva) { errore = it }
                } else {
                    salvaDaMaschera(sceneDallaMaschera(), haCombattimento, combatSkill, combatEndurance, combatEvadeAfterRound, onSalva) { errore = it }
                }
            }) {
                Text("Salva scena")
            }
        }
    }
}

// Campo destinazione con ricerca (30/07/2026, Michele: "mentre scrivo
// penso che sono in una taverna... e potrebbero uscire quelli che fanno
// match"): mentre digiti, filtra le scene per ID o per codiceScena
// (SceneCode.kt) e mostra una lista cliccabile sotto il campo — niente
// popup flottante, solo una lista in linea, più semplice da tenere
// affidabile in Compose Desktop. Selezionare una riga riempie il campo
// con l'ID vero della scena.
@Composable
private fun DestinazioneField(
    valore: String,
    tutteLeScene: List<Scene>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostraSuggerimenti by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = valore,
            onValueChange = {
                onValueChange(it)
                mostraSuggerimenti = it.isNotBlank()
            },
            label = { Text("Scena destinazione") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (mostraSuggerimenti) {
            val corrispondenze = tutteLeScene.filter {
                it.id.contains(valore, ignoreCase = true) || codiceScena(it).contains(valore, ignoreCase = true)
            }.take(6)
            if (corrispondenze.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    corrispondenze.forEach { corrispondente ->
                        Text(
                            "${corrispondente.id} · ${codiceScena(corrispondente)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(corrispondente.id)
                                    mostraSuggerimenti = false
                                }
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

// Campo immagine con suggerimenti dal catalogo chiuso + anteprima
// grafica (§15.3, Michele: "i toni devono essere fissi... così come la
// parte delle immagini" + "devono essere caricate e reindirizzate
// anche graficamente"). Stesso pattern di `DestinazioneField` sopra
// (lista di suggerimenti in linea sotto il campo, non un menu
// flottante) — resta testo libero apposta, per non impedire `url:`
// scritto a mano: i suggerimenti riempiono il campo con
// `static:<id>`, selezionarne uno non è obbligatorio.
@Composable
private fun CampoImmagineConAnteprima(
    valore: String,
    etichetta: String,
    suggerimenti: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostraSuggerimenti by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = valore,
            onValueChange = {
                onValueChange(it)
                mostraSuggerimenti = it.isNotBlank()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(etichetta) },
        )
        if (mostraSuggerimenti) {
            val termine = valore.removePrefix(ImageReference.STATIC_PREFIX).removePrefix(ImageReference.URL_PREFIX)
            val corrispondenze = suggerimenti.filter { it.contains(termine, ignoreCase = true) }.take(6)
            if (corrispondenze.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    corrispondenze.forEach { id ->
                        Text(
                            "${ImageReference.STATIC_PREFIX}$id",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange("${ImageReference.STATIC_PREFIX}$id")
                                    mostraSuggerimenti = false
                                }
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
        if (valore.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            AnteprimaImmagineRisorsa(valore, Modifier.fillMaxWidth().height(90.dp))
        }
    }
}

// Menu a tendina sulle 10 discipline canoniche (Discipline, core/data) —
// a differenza delle immagini, qui il catalogo è già condiviso e chiuso
// per davvero, niente da rimandare.
@Composable
private fun DisciplinaDropdown(valore: String, onValueChange: (String) -> Unit) {
    var aperto by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { aperto = true }) { Text(valore) }
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

// Validazione locale (§7.3): non blocca su destinazioni ancora inesistenti,
// solo su campi obbligatori mancanti — è normale scrivere in avanti.
private fun salvaDaMaschera(
    scenaAggiornata: Scene,
    haCombattimento: Boolean,
    combatSkill: String,
    combatEndurance: String,
    combatEvadeAfterRound: String,
    onSalva: (Scene) -> Unit,
    onErrore: (String) -> Unit,
) {
    if (scenaAggiornata.narrativeText.isBlank()) {
        onErrore("il testo narrato non può essere vuoto")
        return
    }
    val vuota = scenaAggiornata.choices.firstOrNull { it.choiceText.isBlank() || it.nextSceneId.isBlank() }
    if (vuota != null) {
        onErrore("ogni scelta deve avere un testo e una scena di destinazione")
        return
    }
    val disciplinaVuota = scenaAggiornata.disciplineChoices.firstOrNull { it.choiceText.isBlank() || it.nextSceneId.isBlank() }
    if (disciplinaVuota != null) {
        onErrore("ogni scelta per disciplina deve avere un testo e una scena di destinazione")
        return
    }
    if (haCombattimento) {
        val combat = scenaAggiornata.combat
        if (combat == null || combat.enemyName.isBlank()) {
            onErrore("il combattimento deve avere un nome nemico")
            return
        }
        if (combatSkill.toIntOrNull() == null || combatEndurance.toIntOrNull() == null) {
            onErrore("combattività e resistenza del nemico devono essere numeri")
            return
        }
        if (combatEvadeAfterRound.toIntOrNull() == null) {
            onErrore("il numero di round prima della fuga deve essere un numero")
            return
        }
        if (combat.winSceneId.isBlank()) {
            onErrore("il combattimento deve avere una scena per la vittoria")
            return
        }
    }
    onSalva(scenaAggiornata)
}

private fun salvaDaJson(testo: String, onSalva: (Scene) -> Unit, onErrore: (String) -> Unit) {
    val scena = try {
        jsonScena.decodeFromString(Scene.serializer(), testo)
    } catch (e: SerializationException) {
        onErrore("JSON non valido — ${e.message}")
        return
    }
    if (scena.narrativeText.isBlank()) {
        onErrore("il testo narrato non può essere vuoto")
        return
    }
    onSalva(scena)
}

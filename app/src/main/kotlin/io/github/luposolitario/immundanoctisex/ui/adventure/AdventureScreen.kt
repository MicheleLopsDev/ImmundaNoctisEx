package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineIcon
import io.github.luposolitario.immundanoctisex.util.inkColor
import io.github.luposolitario.immundanoctisex.util.resolved

// La scena teatrale in forma minima (Fase 3, "prima funziona poi è
// bello"): header con titolo e numero scena, testo ORIGINALE del
// pacchetto come pagina di libro, zona scelte (normali + discipline),
// combat rapido, card di stato coi valori effettivi dell'engine.
@Composable
fun AdventureScreen(
    state: AdventureState,
    onExitToHome: () -> Unit,
    onReloadCheckpoint: (Int) -> Unit,
    // Scelto in Opzioni (UI.md schermata 7, FontPreferences): il flusso
    // centrale della scena, non il resto della UI. Default = Material
    // (quando la Route non lo passa, es. le @Preview).
    readingFont: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    // Grandezza del testo (Michele 21/07/2026: pulsante con una lente
    // nell'header, accanto a Home — non una schermata a parte, un ciclo
    // di tocchi). initial* dalla preference salvata, onChange la riscrive:
    // stesso pattern del font, stato vero qui dentro perché il pulsante
    // che lo cambia vive in questa schermata.
    initialTextScale: io.github.luposolitario.immundanoctisex.util.TextScale =
        io.github.luposolitario.immundanoctisex.util.TextScale.MEDIUM,
    onTextScaleChange: (io.github.luposolitario.immundanoctisex.util.TextScale) -> Unit = {},
    // Nessun controllo per cambiarlo dentro la scena (a differenza della
    // grandezza): si sceglie in Opzioni e basta, un semplice parametro.
    boldText: Boolean = false,
    // Sfondo della card di stato (Michele 21/07/2026, "un altro picker
    // per la barra di sotto" — questa card): stesso trattamento, un
    // semplice parametro, nessun controllo diretto nella scena.
    statusCardColor: io.github.luposolitario.immundanoctisex.util.StatusCardColor =
        io.github.luposolitario.immundanoctisex.util.StatusCardColor.DEFAULT,
    // Stile del Diario di Combattimento (22/07/2026, richiesta Michele:
    // "potremmo far scegliere nelle opzioni?") — stesso trattamento di
    // statusCardColor, un parametro in più, nessun controllo nella scena.
    parchmentStyle: io.github.luposolitario.immundanoctisex.util.ParchmentStyle =
        io.github.luposolitario.immundanoctisex.util.ParchmentStyle.OFF,
    // Serve solo a risolvere lo stile AUTO della pergamena (23/07/2026):
    // quale tema è EFFETTIVAMENTE in uso, override incluso.
    isDarkTheme: Boolean = false,
    // TTS (UI.md, Tappa 2): l'icona "leggi" è cliccabile solo se
    // l'auto-lettura è spenta in Opzioni — accesa, legge già da sé.
    autoReadEnabled: Boolean = false,
    onReadAloud: () -> Unit = {},
) {
    // Scheda e Diario come overlay dentro la route (stato condiviso;
    // diventeranno destinazioni proprie in Fase 5).
    var showSheet by remember { mutableStateOf(false) }
    var showJournal by remember { mutableStateOf(false) }
    var textScale by remember { mutableStateOf(initialTextScale) }
    // Conferma prima di uscire (Michele 20/07/2026: mancava un modo per
    // tornare al menu dalla scena). L'auto-save è sempre attivo, quindi
    // non si perde nulla — la conferma serve solo contro il tocco
    // accidentale che interrompe la lettura.
    var showExitConfirm by remember { mutableStateOf(false) }
    // Il numero di piazzamenti riusciti, non un booleano: incrementarlo
    // ad ogni salvataggio riavvia il LaunchedEffect anche se il
    // messaggio precedente non è ancora sparito.
    var checkpointsSaved by remember { mutableStateOf(0) }
    if (showSheet) {
        io.github.luposolitario.immundanoctisex.ui.sheet.CharacterSheetScreen(
            hero = state.hero,
            onEquipWeapon = state::equipWeapon,
            onConsumeItem = state::consumeItem,
            onDiscardItem = state::discardItem,
            onClose = { showSheet = false },
        )
        return
    }
    if (showJournal) {
        val context = androidx.compose.ui.platform.LocalContext.current
        io.github.luposolitario.immundanoctisex.ui.journal.JournalScreen(
            journey = state.gameState.session.journey,
            onExport = {
                val markdown = io.github.luposolitario.immundanoctisex.ui.journal.journeyToMarkdown(
                    state.bookTitle,
                    state.gameState.session.journey,
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, markdown)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Esporta diario"))
            },
            onClose = { showJournal = false },
        )
        return
    }

    if (showExitConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Tornare al menu?") },
            text = { Text("La partita è già salvata: puoi riprenderla da dove l'hai lasciata.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onExitToHome) { Text("Torna al menu") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitConfirm = false }) { Text("Annulla") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Header(
            state,
            onJournalClick = { showJournal = true },
            onExitClick = { showExitConfirm = true },
            textScale = textScale,
            onTextScaleCycle = {
                val next = textScale.next()
                textScale = next
                onTextScaleChange(next)
            },
        )

        // Il palcoscenico: sfondo + ritratti, col cerchio d'oro su chi
        // "parla" (il narratore mentre scrive, altrimenti l'eroe).
        AdventureBanner(
            heroName = state.hero.name,
            heroGender = state.hero.gender,
            // Stato del narratore unificato (UI.md): il cerchio d'oro resta
            // acceso sia mentre Gemma scrive sia mentre il TTS legge.
            narratorSpeaking = state.isGenerating || state.isSpeaking,
            // L'alone pulsa solo finché non c'è nulla da leggere: appena
            // arriva il primo pezzo di testo torna fermo.
            narratorThinking = state.isGenerating && state.narrative.isBlank(),
            backgroundImageName = state.backgroundImage,
        )

        // Stile pergamena (23/07/2026, richiesta Michele dopo aver visto lo
        // screenshot del pannello di narrazione ancora piatto: "estendilo
        // anche al testo, non solo al combattimento"; corretto lo stesso
        // giorno): Card Material3 di sempre se OFF, altrimenti Box +
        // Image(Modifier.matchParentSize()) — stesso pattern già
        // funzionante di AdventureBanner, non il Modifier.paint() del
        // primo giro (compilava ma non si vedeva affatto sul device,
        // vedi CombatDiaryPanel per il dettaglio). matchParentSize() non
        // si importa: è un membro di BoxScope, risolto dal contesto del
        // Box.
        val narrationStyle = parchmentStyle.resolved(isDarkTheme)
        val narrationDrawableRes = narrationStyle.drawableRes
        val narrationPanelModifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)
        val narrationContent: @Composable () -> Unit = {
            if (state.narrative.isBlank() && state.isGenerating) {
                // Il narratore sta scrivendo: nessun testo originale da
                // leggere, solo l'attesa RACCONTATA (UI.md §Flusso).
                NarratorThinking(loadingModel = state.isLoadingModel)
            } else {
                Column {
                    // Icona "leggi" (UI.md §Flusso centrale): grigia/
                    // disattivata se l'auto-lettura è già accesa in
                    // Opzioni, altrimenti un tocco fa leggere la scena.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onReadAloud, enabled = !autoReadEnabled) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Leggi ad alta voce",
                            )
                        }
                    }
                    Text(
                        // Il finale fabbricato dal motore nasce senza testo: lo
                        // scrive il narratore. Se non ha potuto (modello assente
                        // o generazione fallita) si mette quello fisso, perché
                        // una schermata vuota non è un finale.
                        text = state.narrative.ifBlank { stringResource(R.string.ending_synthetic_fallback) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = readingFont,
                        fontWeight = if (boldText) FontWeight.Bold else FontWeight.Normal,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * textScale.multiplier,
                        modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
        if (narrationDrawableRes == null) {
            Card(modifier = narrationPanelModifier, elevation = CardDefaults.cardElevation(2.dp)) {
                narrationContent()
            }
        } else {
            CompositionLocalProvider(LocalContentColor provides narrationStyle.inkColor()) {
                Box(modifier = narrationPanelModifier.clip(RoundedCornerShape(12.dp))) {
                    Image(
                        painter = painterResource(id = narrationDrawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Column {
                        narrationContent()
                    }
                }
            }
        }

        // Un incontro pacifico nella storia (Michele 22/07/2026: "npc o
        // beast se sono amichevoli vanno sotto il testo"), non un
        // avversario — quello ha il suo posto in CombatEntryZone. Solo
        // quando l'autore l'ha dichiarato (Scene.npcImage); altrimenti
        // niente, come prima di questa funzione.
        npcImageRes(state.currentScene.npcImage)?.let { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // 120dp (Michele 22/07/2026: 100 -> 110 -> "un altro 10% in
                // più e ci siamo" — arrotondato da 121 per un numero pulito).
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(8.dp))
        }

        StatusCard(hero = state.hero, onClick = { showSheet = true }, cardColor = statusCardColor)
        Spacer(Modifier.height(8.dp))

        when {
            state.combatSession != null -> CombatActiveZone(state, parchmentStyle, isDarkTheme)
            // Finché il narratore scrive non si mostrano scelte né nemico:
            // apparirebbero col testo originale per poi cambiare sotto gli
            // occhi (UI.md: prima lo streaming, POI i pulsanti).
            state.isGenerating -> Unit
            state.currentScene.combat != null -> CombatEntryZone(state)
            state.isEnding -> EndingZone(state, onExitToHome, onReloadCheckpoint)
            state.requiresRoll -> DiceZone(state)
            else -> {
                if (state.availableItems.isNotEmpty()) {
                    PickupZone(state)
                    Spacer(Modifier.height(6.dp))
                }
                ChoicesZone(state)
                // Piazzamento checkpoint dal menu (STATO.md Blocco 2): fuori
                // dal combattimento, col budget della difficoltà visibile.
                if (state.checkpointsRemaining > 0) {
                    Spacer(Modifier.height(6.dp))
                    // Prima non c'era NESSUN riscontro visivo: il file si
                    // scriveva ma la scritta restava uguale, e sembrava che
                    // il tasto non facesse nulla (Michele 20/07/2026).
                    if (checkpointsSaved > 0) {
                        SaveConfirmedRow(onExpired = { checkpointsSaved = 0 })
                    } else {
                        androidx.compose.material3.TextButton(
                            onClick = { if (state.placeCheckpoint()) checkpointsSaved++ },
                        ) {
                            Text("Piazza checkpoint (rimasti: ${state.checkpointsRemaining})")
                        }
                    }
                }
            }
        }
    }
}

// Spunta verde + testo per ~1,5s dopo un salvataggio riuscito, poi torna
// al pulsante normale. `onExpired` invece di un timer nella Screen: la
// UI non deve sapere quanto dura, solo che prima o poi finisce.
@Composable
private fun SaveConfirmedRow(onExpired: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onExpired()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Checkpoint salvato", color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
    }
}

@Composable
private fun Header(
    state: AdventureState,
    onJournalClick: () -> Unit,
    onExitClick: () -> Unit,
    textScale: io.github.luposolitario.immundanoctisex.util.TextScale,
    onTextScaleCycle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // PRIMA (20/07/2026): il titolo non aveva limite di larghezza. Con
        // un titolo lungo ("The Warehouse Letter") spingeva Diario/Scena/
        // Home fuori dallo schermo — non un tasto rotto, un tasto invisibile.
        // Ora questo gruppo ha weight(1f): si restringe lui, il gruppo dei
        // controlli a destra resta sempre intero e a portata di tocco.
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            // Semaforo di stato del motore (UI.md §Header): verde pronto,
            // giallo generazione, rosso contesto quasi pieno.
            val tokenInfo = state.tokenInfo
            val color = when {
                state.isGenerating -> MaterialTheme.colorScheme.tertiary
                tokenInfo != null && tokenInfo.percentage > 60 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = "Stato del narratore",
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                state.bookTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.TextButton(onClick = onJournalClick) {
                Text("Diario")
            }
            Text(
                "Scena ${state.currentScene.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            // Grandezza del testo (Michele 21/07/2026): un tocco = un
            // passo nel ciclo piccolo/medio/grande, niente menu a parte.
            // L'etichetta ("A-"/"A"/"A+") nell'icona dice già a che punto
            // del ciclo si è, senza bisogno di aprire nulla per saperlo.
            androidx.compose.material3.IconButton(onClick = onTextScaleCycle) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Cambia grandezza testo (${textScale.icon})",
                )
            }
            // Uscita al menu (Michele 20/07/2026: mancava del tutto). Con
            // conferma: un tocco qui non deve interrompere la lettura per
            // sbaglio, anche se l'auto-save rende l'uscita innocua.
            androidx.compose.material3.IconButton(onClick = onExitClick) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Torna al menu",
                )
            }
        }
    }
}

@Composable
private fun ChoicesZone(state: AdventureState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.availableChoices.forEach { choice ->
            Button(onClick = { state.takeChoice(choice) }, modifier = Modifier.fillMaxWidth()) {
                Text(state.choiceText(choice))
            }
        }
        state.availableDisciplineChoices.forEach { choice ->
            OutlinedCard(
                onClick = { state.useDiscipline(choice) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = disciplineIcon(choice.disciplineId),
                        contentDescription = choice.disciplineId,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(state.disciplineChoiceText(choice), fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Il Dado del Destino fuori dal combattimento (REGOLE.md Blocco 6): il
// gioco si ferma, le scelte spariscono, si tira e POI si va. v0.1 è un
// bottone; l'overlay animato è Fase 7.
@Composable
private fun DiceZone(state: AdventureState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val roll = state.lastChoiceRoll
        if (roll == null) {
            Text("Il destino decide: tira il Dado.", fontWeight = FontWeight.Bold)
            Button(onClick = { state.rollForChoice() }, modifier = Modifier.fillMaxWidth()) {
                Text("Tira il Dado del Destino")
            }
        } else {
            Text(
                "Hai tirato: $roll",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = { state.resolveRolledChoice() }, modifier = Modifier.fillMaxWidth()) {
                Text("Continua")
            }
        }
    }
}

@Composable
private fun EndingZone(
    state: AdventureState,
    onExitToHome: () -> Unit,
    onReloadCheckpoint: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // UN FINALE DICHIARA SEMPRE COM'È ANDATA (richiesta Michele
        // 20/07/2026): prima si arrivava in fondo al libro e si tornava al
        // menu senza sapere se si era vinto o perso.
        // Teschio per la morte, sole nascente per la vittoria (richiesta
        // Michele 20/07/2026). NEUTRAL non ha immagine: non c'è niente da
        // celebrare né da piangere.
        EndingBadge(
            outcome = state.endingOutcome,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.adventureDeleted) {
            Text(
                "Morte in IRON: la sessione è stata cancellata.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
        // Alla morte (fuori da IRON) si offrono i checkpoint piazzati:
        // ricaricabili illimitatamente, il diario si tronca alla fotografia.
        if (state.isDeathEnding && !state.adventureDeleted) {
            state.placedCheckpoints().forEach { slot ->
                Button(onClick = { onReloadCheckpoint(slot) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ricarica il checkpoint $slot")
                }
            }
        }
        Button(onClick = onExitToHome, modifier = Modifier.fillMaxWidth()) {
            Text("Torna alla Home")
        }
    }
}

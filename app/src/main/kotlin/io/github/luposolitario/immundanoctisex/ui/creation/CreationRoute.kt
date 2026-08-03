package io.github.luposolitario.immundanoctisex.ui.creation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.PersonaggioSalvato
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult
import io.github.luposolitario.immundanoctisex.core.engine.character.TrasportoPersonaggio
import java.util.UUID

// Raccordo della creazione personaggio: carica il pacchetto, costruisce la
// SessionData iniziale, la salva (primo auto-save) e consegna la partita.
@Composable
fun CreationRoute(
    container: AppContainer,
    isDarkTheme: Boolean,
    difficulty: Difficulty,
    onSessionCreated: (SessionData) -> Unit,
) {
    val loadResult = remember { container.packageRepository.load() }
    val state = remember { CreationState(container.diceRoller) }

    when (loadResult) {
        is PackageLoadResult.Success -> CharacterCreationScreen(
            isDarkTheme = isDarkTheme,
            state = state,
            onCreate = {
                val startSceneId = container.packageRepository.startScene()?.id ?: return@CharacterCreationScreen
                // Una nuova avventura riparte SEMPRE pulita: senza questo, i
                // checkpoint di una partita precedente per lo stesso libro
                // restavano sul device (scritti-una-volta, mai sovrascritti)
                // e bloccavano ogni piazzamento della partita nuova, in
                // silenzio — placeCheckpoint() falliva senza dirlo a nessuno
                // (Michele 20/07/2026: "rimasti: 2" che non scendeva mai).
                // SessionStore.deleteAdventure esisteva già per questo (il
                // suo stesso commento lo dichiara), semplicemente non veniva
                // chiamato qui.
                container.sessionStore.deleteAdventure(loadResult.manifest.id)

                // Il personaggio si salva DA SOLO alla creazione
                // (03/08/2026, Michele: "va in automatico il char alla
                // creazione e quando finisci il libro"): niente esporta,
                // niente scegli-dove. Da questo momento esiste e potrà
                // ricominciare da un altro libro.
                val idPersonaggio = UUID.randomUUID().toString()
                val session = state.buildSession(
                    loadResult.manifest,
                    difficulty,
                    startSceneId,
                    personaggioId = idPersonaggio,
                )
                container.sessionStore.saveSession(session)
                session.characters.firstOrNull { it.role == CharacterRole.HERO }?.let { eroe ->
                    container.personaggiStore.salva(
                        PersonaggioSalvato(
                            id = idPersonaggio,
                            creatoIl = System.currentTimeMillis(),
                            libroOrigineId = loadResult.manifest.id,
                            libroOrigineTitolo = loadResult.manifest.title,
                            personaggio = eroe,
                        ),
                    )
                }
                onSessionCreated(session)
            },
            // I personaggi già esistenti, per ricominciare da uno di loro
            // invece che da zero. Vuoto la prima volta: la schermata non
            // mostra nulla e resta identica a prima.
            personaggiDisponibili = container.personaggiStore.elenco(),
            onImporta = { salvato, discipline ->
                val startSceneId = container.packageRepository.startScene()?.id ?: return@CharacterCreationScreen
                container.sessionStore.deleteAdventure(loadResult.manifest.id)

                // Le discipline guadagnate si applicano PRIMA di entrare
                // in partita, e si scrivono subito nel file: se l'app si
                // chiudesse a metà avventura, la scelta non andrebbe
                // rifatta.
                val conDiscipline = TrasportoPersonaggio
                    .conDisciplineNuove(salvato, discipline)
                    .getOrDefault(salvato)
                container.personaggiStore.salva(conDiscipline)

                val session = state.buildSession(
                    loadResult.manifest,
                    difficulty,
                    startSceneId,
                    eroeImportato = TrasportoPersonaggio.preparaPerNuovoLibro(conDiscipline),
                    personaggioId = conDiscipline.id,
                )
                container.sessionStore.saveSession(session)
                onSessionCreated(session)
            },
            onEliminaPersonaggio = { container.personaggiStore.elimina(it.id) },
        )

        // Pacchetto rotto: messaggio semplice, il gioco non crasha mai.
        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Impossibile caricare il libro (pacchetto non valido).")
        }
    }
}

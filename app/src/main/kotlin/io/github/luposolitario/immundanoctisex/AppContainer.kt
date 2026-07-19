package io.github.luposolitario.immundanoctisex

import android.content.Context
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageRepository
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageSource
import io.github.luposolitario.immundanoctisex.core.data.session.FileSessionStore
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.dice.RandomDiceRoller
import io.github.luposolitario.immundanoctisex.inference.InferenceEngine
import io.github.luposolitario.immundanoctisex.inference.InferencePreferences
import io.github.luposolitario.immundanoctisex.inference.LiteRtLmEngine
import io.github.luposolitario.immundanoctisex.model.ModelPreferences
import io.github.luposolitario.immundanoctisex.util.ThemePreferences
import java.io.File
import java.io.InputStream

// DI leggera (ARCHITETTURA.md): un contenitore costruito una volta dalla
// MainActivity, al posto dei singleton con Context di v1. Qui vivono solo
// le dipendenze condivise; i ViewModel per schermata se le fanno passare.
class AppContainer(context: Context) {

    val themePreferences = ThemePreferences(context)

    val modelPreferences = ModelPreferences(context)

    val inferencePreferences = InferencePreferences(context)

    // Istanza unica a scope applicazione (ARCHITETTURA §istanze): il
    // modello costa GB e secondi di caricamento, si carica una volta.
    val inferenceEngine: InferenceEngine = LiteRtLmEngine(context)

    // Carica il modello selezionato se è già sul telefono. Restituisce
    // false senza rumore se non c'è: il gioco parte comunque, col testo
    // originale del pacchetto.
    suspend fun ensureModelLoaded(): Boolean {
        if (inferenceEngine.isLoaded) return true
        val model = modelPreferences.selectedModel
        if (!modelPreferences.isDownloaded(model)) return false
        return inferenceEngine
            .load(modelPreferences.fileFor(model), inferencePreferences.toConfig())
            .isSuccess
    }

    val sessionStore: SessionStore =
        FileSessionStore(File(context.filesDir, "saves"))

    val diceRoller: DiceRoller = RandomDiceRoller()

    // Il libro incluso nell'APK: scenes.sample.json dagli asset (content/
    // è montato come cartella asset dal build). Il side-load via picker
    // arriva più avanti nella Fase 3/5.
    val packageRepository = PackageRepository(
        AssetPackageSource(context, "scenes.sample.json"),
    )
}

// Implementazione Android di PackageSource: apre un asset dell'APK.
class AssetPackageSource(
    private val context: Context,
    private val assetName: String,
) : PackageSource {
    override fun open(): InputStream = context.assets.open(assetName)
}

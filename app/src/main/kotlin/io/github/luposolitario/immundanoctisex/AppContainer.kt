package io.github.luposolitario.immundanoctisex

import android.content.Context
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageRepository
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageSource
import io.github.luposolitario.immundanoctisex.core.data.session.FileSessionStore
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.dice.RandomDiceRoller
import io.github.luposolitario.immundanoctisex.inference.InferencePreferences
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

package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences

// Il benvenuto si mostra UNA VOLTA, al primo avvio (05/08/2026).
//
// Serve perché senza di esso chi apre l'app la prima volta non sa due
// cose: che può giocare subito, e che il modello è facoltativo. Prima si
// trovava davanti a una Home con una tile "Modelli LLM" e nessuno gli
// diceva se doveva scaricare 3-4 GB per cominciare.
class BenvenutoPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var benvenutoVisto: Boolean
        get() = prefs.getBoolean(KEY_VISTO, false)
        set(value) = prefs.edit().putBoolean(KEY_VISTO, value).apply()

    private companion object {
        const val PREFS_NAME = "benvenuto_preferences"
        const val KEY_VISTO = "benvenuto_visto"
    }
}

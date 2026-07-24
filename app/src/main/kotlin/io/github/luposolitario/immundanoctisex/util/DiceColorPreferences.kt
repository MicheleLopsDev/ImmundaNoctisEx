package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

// Colore del dado a 10 facce (24/07/2026, richiesta Michele: "fai che
// nelle preferenze si può scegliere il colore del dado" — dopo aver
// tolto il lupo dalla faccia zero, che "confondeva e basta"). Una rosa
// di preset (stesso principio di AccentColor: uno swatch col colore
// vero, non un color picker RGB libero) invece di 5 esadecimali fissi
// per opzione — le 5 sfumature delle facce si derivano da questo unico
// colore base (vedi TenSidedDie.kt). GRAY è il default: un dado neutro,
// non legato al tema/accento scelto altrove.
enum class DiceColor(val displayName: String, val base: Color) {
    GRAY("Grigio (default)", Color(0xFF9E9E9E)),
    RED("Rosso", Color(0xFFE53935)),
    GOLD("Oro", Color(0xFFFFC107)),
    BLUE("Blu", Color(0xFF2196F3)),
    GREEN("Verde", Color(0xFF4CAF50)),
}

class DiceColorPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var diceColor: DiceColor
        get() = prefs.getString(KEY_COLOR, null)
            ?.let { name -> runCatching { DiceColor.valueOf(name) }.getOrNull() }
            ?: DiceColor.GRAY
        set(value) = prefs.edit().putString(KEY_COLOR, value.name).apply()

    private companion object {
        const val PREFS_NAME = "dice_color_preferences"
        const val KEY_COLOR = "dice_color"
    }
}

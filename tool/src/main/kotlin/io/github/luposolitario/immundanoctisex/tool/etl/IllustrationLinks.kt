package io.github.luposolitario.immundanoctisex.tool.etl

// Numeri romani -> arabi per gli ID reali delle immagini su Project Aon
// (xhtml/lw/{libro}/ill{N}.png, N arabo) — l'etichetta HTML porta invece
// il romano (29/07/2026). NON è un contatore progressivo affidabile:
// almeno un libro (01fftd, sezione 267) ha un'illustrazione nell'indice
// del libro ma senza marcatore inline in questo file, quindi un +1
// avrebbe sfasato tutti i numeri successivi (vedi DIARIO.md).
private val romanValues = listOf(
    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
    100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
    10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
)

fun romanoInArabo(romano: String): Int? {
    if (romano.isEmpty()) return null
    var resto = romano.uppercase()
    var totale = 0
    for ((valore, simbolo) in romanValues) {
        while (resto.startsWith(simbolo)) {
            totale += valore
            resto = resto.removePrefix(simbolo)
        }
    }
    return if (resto.isEmpty()) totale else null
}

// URL reale della PNG originale su Project Aon per un IllustrationMarker,
// null se il numero romano dell'etichetta non è riconosciuto.
fun illustrationUrl(bookId: String, marker: ProjectAonHtmlParser.IllustrationMarker): String? {
    val romano = marker.label.removePrefix("[").removeSuffix("]").removePrefix("Illustration").trim()
    val numero = romanoInArabo(romano) ?: return null
    return "https://www.projectaon.org/en/xhtml/lw/$bookId/ill$numero.png"
}

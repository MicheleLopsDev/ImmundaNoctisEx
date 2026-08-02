package io.github.luposolitario.immundanoctisex.core.engine.inference

// La lingua in cui il modello riscrive o traduce la scena (02/08/2026,
// Michele: "aggiungere il selettore delle lingue come una proprietà
// degli llm, usa le lingue presenti in europa"). Prima erano cinque,
// dichiarate in :app; ora sono l'elenco europeo completo e vivono in
// :core:engine, perché la stessa scelta serve al client e all'editor —
// se le due liste divergessero, l'anteprima mostrerebbe una lingua che
// il giocatore non può selezionare.
//
// Tre campi, tre usi diversi:
//  - `displayName` è il nome NELLA LINGUA STESSA (un elenco di lingue
//    tradotto in italiano è inutile a chi cerca la propria);
//  - `promptValue` è in inglese perché finisce nel prompt inglese
//    ("Rewrite... in {user_language}") — è la lingua del NOME, non una
//    traduzione da fare;
//  - `tag` è il codice BCP-47, che serve al TTS del client per far
//    parlare la voce giusta (`Locale.forLanguageTag`), tenuto qui come
//    stringa perché `java.util.Locale` non esiste nel codice comune.
//
// AVVERTENZA onesta: l'elenco dice cosa si può CHIEDERE, non cosa il
// modello sa fare bene. Gemma 4 se la cava con le lingue maggiori;
// sulle meno diffuse (maltese, irlandese, estone, lussemburghese) la
// resa cala parecchio, ed è normale che sia così — nessun errore.
enum class LinguaOutput(
    val displayName: String,
    val promptValue: String,
    val tag: String,
    val ufficialeUe: Boolean = true,
) {
    // Le 24 lingue ufficiali dell'Unione Europea, in ordine alfabetico
    // di nome proprio.
    BULGARO("Български", "Bulgarian", "bg"),
    CECO("Čeština", "Czech", "cs"),
    CROATO("Hrvatski", "Croatian", "hr"),
    DANESE("Dansk", "Danish", "da"),
    ESTONE("Eesti", "Estonian", "et"),
    FINLANDESE("Suomi", "Finnish", "fi"),
    FRANCESE("Français", "French", "fr"),
    GRECO("Ελληνικά", "Greek", "el"),
    INGLESE("English", "English", "en"),
    IRLANDESE("Gaeilge", "Irish", "ga"),
    ITALIANO("Italiano", "Italian", "it"),
    LETTONE("Latviešu", "Latvian", "lv"),
    LITUANO("Lietuvių", "Lithuanian", "lt"),
    MALTESE("Malti", "Maltese", "mt"),
    OLANDESE("Nederlands", "Dutch", "nl"),
    POLACCO("Polski", "Polish", "pl"),
    PORTOGHESE("Português", "Portuguese", "pt"),
    RUMENO("Română", "Romanian", "ro"),
    SLOVACCO("Slovenčina", "Slovak", "sk"),
    SLOVENO("Slovenščina", "Slovenian", "sl"),
    SPAGNOLO("Español", "Spanish", "es"),
    SVEDESE("Svenska", "Swedish", "sv"),
    TEDESCO("Deutsch", "German", "de"),
    UNGHERESE("Magyar", "Hungarian", "hu"),

    // Altre lingue europee fuori dall'UE, incluse perché "presenti in
    // Europa" è il criterio dato — non l'appartenenza all'Unione.
    ALBANESE("Shqip", "Albanian", "sq", ufficialeUe = false),
    BOSNIACO("Bosanski", "Bosnian", "bs", ufficialeUe = false),
    ISLANDESE("Íslenska", "Icelandic", "is", ufficialeUe = false),
    MACEDONE("Македонски", "Macedonian", "mk", ufficialeUe = false),
    NORVEGESE("Norsk", "Norwegian", "nb", ufficialeUe = false),
    SERBO("Српски", "Serbian", "sr", ufficialeUe = false),
    UCRAINO("Українська", "Ukrainian", "uk", ufficialeUe = false),
    ;

    companion object {
        // L'italiano resta il default: è la lingua di Michele e di tutti
        // i libri scritti finora.
        val DEFAULT = ITALIANO

        // Ordinate come vanno mostrate in un menu: prima le ufficiali
        // UE, poi le altre, ciascun gruppo in ordine alfabetico del nome
        // proprio — un ordine STABILE, non l'ordine di dichiarazione che
        // cambierebbe a ogni aggiunta.
        val perMenu: List<LinguaOutput> by lazy {
            entries.sortedWith(compareBy({ !it.ufficialeUe }, { it.displayName }))
        }

        fun daNome(nome: String?): LinguaOutput =
            entries.firstOrNull { it.name == nome } ?: DEFAULT
    }
}

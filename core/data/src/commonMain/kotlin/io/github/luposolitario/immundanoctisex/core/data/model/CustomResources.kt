package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Risorse url: fornite da chi scrive il libro (§15.7, EDITOR.md) — un
// promemoria di comodità per l'AUTORE nell'editor, non un meccanismo
// letto dal motore di gioco: una scena continua a salvare direttamente
// "url:https://..." come sempre, questo registro esiste solo per non
// dover riscrivere lo stesso link più volte durante la scrittura.
// Diverso dal catalogo statico (StaticResourceCatalog, :tool): quello
// descrive cosa porta con sé l'app, sempre uguale per ogni libro;
// questo è dati DI QUESTO libro, stesso posto di toneHints/
// disciplineChoices.
@Serializable
data class CustomResourceEntry(val id: String, val url: String)

@Serializable
data class CustomResources(
    val images: List<CustomResourceEntry> = emptyList(),
    val sounds: List<CustomResourceEntry> = emptyList(),
)

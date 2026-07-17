package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Voce del catalogo discipline nel manifest: id canonico + testo per la
// creazione del personaggio. id è validato contro Discipline.
@Serializable
data class DisciplineDescriptor(
    val id: String,
    val name: String,
    val description: String,
)

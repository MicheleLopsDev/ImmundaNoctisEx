package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Le 10 discipline Kai canoniche (SHADOWSTEP escluso, decisione sessione 14/07).
@Serializable
enum class Discipline {
    WEAPONSKILL,
    CAMOUFLAGE,
    HUNTING,
    SIXTH_SENSE,
    TRACKING,
    HEALING,
    MINDSHIELD,
    MINDBLAST,
    ANIMAL_KINSHIP,
    MIND_OVER_MATTER,
}

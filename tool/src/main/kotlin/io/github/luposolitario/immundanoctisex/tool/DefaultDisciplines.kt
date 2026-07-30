package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineDescriptor

// Stesso testo di content/scenes.sample.json — le 10 discipline canoniche
// non cambiano da libro a libro, solo la prosa della singola avventura.
// Condiviso tra ConvertMain (ETL) e la creazione di un libro nuovo
// nell'editor grafico (doc/EDITOR.md §9.1) — prima duplicato in
// ConvertMain.kt, estratto qui il 30/07/2026 quando è servito anche là.
fun defaultDisciplineDescriptors(): List<DisciplineDescriptor> = listOf(
    DisciplineDescriptor("WEAPONSKILL", "Weaponskill", "Mastery with a chosen weapon type, adding a bonus to Combat Skill when wielding it."),
    DisciplineDescriptor("CAMOUFLAGE", "Camouflage", "The art of blending into terrain and shadow to move unseen."),
    DisciplineDescriptor("HUNTING", "Hunting", "Skill at foraging and catching food in the wild, removing the need for meals."),
    DisciplineDescriptor("SIXTH_SENSE", "Sixth Sense", "A heightened intuition that warns of danger and hidden threats."),
    DisciplineDescriptor("TRACKING", "Tracking", "The ability to follow trails and read signs left by passage."),
    DisciplineDescriptor("HEALING", "Healing", "The gift of mending wounds and restoring endurance without herbs."),
    DisciplineDescriptor("MINDSHIELD", "Mindshield", "A mental ward against psychic intrusion and fear."),
    DisciplineDescriptor("MINDBLAST", "Mindblast", "The power to strike an enemy's mind directly, aiding in combat."),
    DisciplineDescriptor("ANIMAL_KINSHIP", "Animal Kinship", "An affinity with animals, calming beasts and reading their moods."),
    DisciplineDescriptor("MIND_OVER_MATTER", "Mind Over Matter", "Telekinetic control, allowing objects to be moved by will alone."),
)

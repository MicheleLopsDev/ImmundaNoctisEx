package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// I 9 tipi d'arma canonici di Lupo Solitario (libro 1, sezione Equipment)
// più UNARMED: non è un'arma vera ma la specializzazione WEAPONSKILL a
// mani nude (REGOLE.md §4.2) — mai usarlo come tipo di un GameItem.
// ID canonici nei dati; i nomi mostrati vivranno in strings.xml.
@Serializable
enum class WeaponType {
    DAGGER,
    SPEAR,
    MACE,
    SHORT_SWORD,
    WARHAMMER,
    SWORD,
    AXE,
    QUARTERSTAFF,
    BROADSWORD,
    UNARMED,
}

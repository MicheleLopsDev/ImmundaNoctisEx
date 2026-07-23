package io.github.luposolitario.immundanoctisex.ui.creation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.HeroIcon
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType

// Catalogo per la creazione: le 10 discipline canoniche con nome/descrizione
// da strings.xml e icona (mappa di v1 CORRETTA: chiavi sugli ID canonici
// UPPER_SNAKE, non sui nomi display — bug censito in ANALISI-UI-V1.md).
data class DisciplineUi(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
)

val KAI_DISCIPLINES_UI = listOf(
    DisciplineUi("WEAPONSKILL", R.string.discipline_weaponskill, R.string.discipline_weaponskill_desc, Icons.Default.Shield),
    DisciplineUi("CAMOUFLAGE", R.string.discipline_camouflage, R.string.discipline_camouflage_desc, Icons.Default.VisibilityOff),
    DisciplineUi("HUNTING", R.string.discipline_hunting, R.string.discipline_hunting_desc, Icons.Default.Pets),
    DisciplineUi("SIXTH_SENSE", R.string.discipline_sixth_sense, R.string.discipline_sixth_sense_desc, Icons.Default.Hearing),
    DisciplineUi("TRACKING", R.string.discipline_tracking, R.string.discipline_tracking_desc, Icons.Default.LocationSearching),
    DisciplineUi("HEALING", R.string.discipline_healing, R.string.discipline_healing_desc, Icons.Default.Healing),
    DisciplineUi("MINDSHIELD", R.string.discipline_mindshield, R.string.discipline_mindshield_desc, Icons.Default.Security),
    DisciplineUi("MINDBLAST", R.string.discipline_mindblast, R.string.discipline_mindblast_desc, Icons.Default.Psychology),
    DisciplineUi("ANIMAL_KINSHIP", R.string.discipline_animal_kinship, R.string.discipline_animal_kinship_desc, Icons.Default.Group),
    DisciplineUi("MIND_OVER_MATTER", R.string.discipline_mind_over_matter, R.string.discipline_mind_over_matter_desc, Icons.Default.Star),
)

fun disciplineIcon(id: String): ImageVector =
    KAI_DISCIPLINES_UI.firstOrNull { it.id == id }?.icon ?: Icons.Default.HelpOutline

// Nome e descrizione localizzati. Null per un id fuori catalogo: chi
// chiama degrada sull'ID canonico invece di mostrare una stringa vuota
// (l'ID nei dati, il nome solo in strings.xml — vincolo di progetto).
@StringRes
fun disciplineName(id: String): Int? = KAI_DISCIPLINES_UI.firstOrNull { it.id == id }?.nameRes

@StringRes
fun disciplineDescription(id: String): Int? =
    KAI_DISCIPLINES_UI.firstOrNull { it.id == id }?.descriptionRes

// Icone armi (22/07/2026, Michele: nuovo foglio a 9 pezzi nello stesso
// stile china/silhouette, dal piano di reskin — sfondo rimosso qui con
// flood-fill a range fisso, vedi DIARIO.md). Sostituisce le sei icone di
// v1 E riempie le tre che mancavano (dagger/short_sword/warhammer, prima
// sul segnaposto ic_unknown_item). Tutte e nove ora coerenti tra loro.
fun weaponTypeIcon(type: WeaponType?): Int = when (type) {
    WeaponType.DAGGER -> R.drawable.ic_dagger
    WeaponType.SPEAR -> R.drawable.ic_spear
    WeaponType.MACE -> R.drawable.ic_mace
    WeaponType.SHORT_SWORD -> R.drawable.ic_short_sword
    WeaponType.WARHAMMER -> R.drawable.ic_warhammer
    WeaponType.SWORD -> R.drawable.ic_sword
    WeaponType.AXE -> R.drawable.ic_axe
    WeaponType.QUARTERSTAFF -> R.drawable.ic_staff
    WeaponType.BROADSWORD -> R.drawable.ic_broadsword
    WeaponType.UNARMED -> R.drawable.ic_fists
    else -> R.drawable.ic_unknown_item
}

@StringRes
fun weaponTypeName(type: WeaponType): Int = when (type) {
    WeaponType.DAGGER -> R.string.weapon_dagger
    WeaponType.SPEAR -> R.string.weapon_spear
    WeaponType.MACE -> R.string.weapon_mace
    WeaponType.SHORT_SWORD -> R.string.weapon_short_sword
    WeaponType.WARHAMMER -> R.string.weapon_warhammer
    WeaponType.SWORD -> R.string.weapon_sword
    WeaponType.AXE -> R.string.weapon_axe
    WeaponType.QUARTERSTAFF -> R.string.weapon_quarterstaff
    WeaponType.BROADSWORD -> R.string.weapon_broadsword
    WeaponType.UNARMED -> R.string.weapon_unarmed
}

// Icona dell'eroe (24/07/2026, richiesta Michele: "facciamogli scegliere
// l'icona... devono essere tutti animali", lista mandata al grafico).
// Consegnate il 24/07 in un unico foglio "icone per personaggi.png"
// (griglia 5×3 con etichetta sotto ogni icona), ritagliate e sfondo
// rimosso qui (stesso flood-fill a range fisso già collaudato). Il
// foglio aveva anche un lupo nello stesso stile "ombreggiato" degli
// altri 14: su richiesta di Michele ("uniforma il tutto") ha
// SOSTITUITO il vecchio `lupo_solitario.png` (due soli toni, stile
// diverso) — tutte e 15 le icone sono ora nella stessa famiglia
// visiva. Stesso file, quindi anche `TenSidedDie` (faccia zero del
// dado) eredita il lupo nuovo senza bisogno di toccarlo.
fun heroIconRes(icon: HeroIcon): Int = when (icon) {
    HeroIcon.WOLF -> R.drawable.lupo_solitario
    HeroIcon.FALCON -> R.drawable.hero_falcon
    HeroIcon.EAGLE -> R.drawable.hero_eagle
    HeroIcon.BEAR -> R.drawable.hero_bear
    HeroIcon.FOX -> R.drawable.hero_fox
    HeroIcon.RAVEN -> R.drawable.hero_raven
    HeroIcon.OWL -> R.drawable.hero_owl
    HeroIcon.LION -> R.drawable.hero_lion
    HeroIcon.TIGER -> R.drawable.hero_tiger
    HeroIcon.PANTHER -> R.drawable.hero_panther
    HeroIcon.LYNX -> R.drawable.hero_lynx
    HeroIcon.BOAR -> R.drawable.hero_boar
    HeroIcon.STAG -> R.drawable.hero_stag
    HeroIcon.SNAKE -> R.drawable.hero_snake
    HeroIcon.DRAGON -> R.drawable.hero_dragon
}

@StringRes
fun heroIconName(icon: HeroIcon): Int = when (icon) {
    HeroIcon.WOLF -> R.string.hero_icon_wolf
    HeroIcon.FALCON -> R.string.hero_icon_falcon
    HeroIcon.EAGLE -> R.string.hero_icon_eagle
    HeroIcon.BEAR -> R.string.hero_icon_bear
    HeroIcon.FOX -> R.string.hero_icon_fox
    HeroIcon.RAVEN -> R.string.hero_icon_raven
    HeroIcon.OWL -> R.string.hero_icon_owl
    HeroIcon.LION -> R.string.hero_icon_lion
    HeroIcon.TIGER -> R.string.hero_icon_tiger
    HeroIcon.PANTHER -> R.string.hero_icon_panther
    HeroIcon.LYNX -> R.string.hero_icon_lynx
    HeroIcon.BOAR -> R.string.hero_icon_boar
    HeroIcon.STAG -> R.string.hero_icon_stag
    HeroIcon.SNAKE -> R.string.hero_icon_snake
    HeroIcon.DRAGON -> R.string.hero_icon_dragon
}

// Oggetto speciale iniziale a scelta (come v1/canone libro 1): Mappa,
// Elmo (+2 RES) o Gilet di maglia di ferro (+4 RES) — i bonus sono
// l'effetto dichiarativo ENDURANCE:n calcolato dall'engine finché
// l'oggetto è posseduto, mai sommato a mano (differenza da v1).
data class SpecialItemUi(
    val item: GameItem,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val iconRes: Int,
)

val INITIAL_SPECIAL_ITEMS = listOf(
    SpecialItemUi(
        item = GameItem(name = "Map", type = ItemType.SPECIAL_ITEM),
        nameRes = R.string.item_map,
        descriptionRes = R.string.item_map_desc,
        iconRes = R.drawable.ic_map_icon,
    ),
    SpecialItemUi(
        item = GameItem(name = "Helmet", type = ItemType.SPECIAL_ITEM, effect = "ENDURANCE:2"),
        nameRes = R.string.item_helmet,
        descriptionRes = R.string.item_helmet_desc,
        iconRes = R.drawable.ic_helmet,
    ),
    SpecialItemUi(
        item = GameItem(name = "Chainmail Waistcoat", type = ItemType.SPECIAL_ITEM, effect = "ENDURANCE:4"),
        nameRes = R.string.item_chainmail,
        descriptionRes = R.string.item_chainmail_desc,
        iconRes = R.drawable.ic_armor,
    ),
)

// Oggetti comuni dati a TUTTI alla partenza (come v1): una Pozione
// Curativa (HEAL:4, canone Laumspur — non usabile in combattimento) e
// due Pasti. Le Corone arrivano dal tiro delle stat.
val INITIAL_COMMON_ITEMS = listOf(
    GameItem(name = "Laumspur Potion", type = ItemType.BACKPACK_ITEM, effect = "HEAL:4"),
    GameItem(name = "Meal", type = ItemType.BACKPACK_ITEM, quantity = 2),
)

// Equipaggiamento iniziale: TUTTI i 9 tipi d'arma canonici (richiesta
// Michele dopo il primo test sul device), nomi canonici inglesi come i
// dati dei pacchetti; i nomi mostrati vengono da strings.xml via
// weaponTypeName. L'alternativa "nessuna arma" (arti marziali) vive in
// CreationState.fightsUnarmed.
val INITIAL_WEAPONS = listOf(
    GameItem(name = "Dagger", type = ItemType.WEAPON, weaponType = WeaponType.DAGGER),
    GameItem(name = "Spear", type = ItemType.WEAPON, weaponType = WeaponType.SPEAR),
    GameItem(name = "Mace", type = ItemType.WEAPON, weaponType = WeaponType.MACE),
    GameItem(name = "Short Sword", type = ItemType.WEAPON, weaponType = WeaponType.SHORT_SWORD),
    GameItem(name = "Warhammer", type = ItemType.WEAPON, weaponType = WeaponType.WARHAMMER),
    GameItem(name = "Sword", type = ItemType.WEAPON, weaponType = WeaponType.SWORD),
    GameItem(name = "Axe", type = ItemType.WEAPON, weaponType = WeaponType.AXE),
    GameItem(name = "Quarterstaff", type = ItemType.WEAPON, weaponType = WeaponType.QUARTERSTAFF),
    GameItem(name = "Broadsword", type = ItemType.WEAPON, weaponType = WeaponType.BROADSWORD),
)

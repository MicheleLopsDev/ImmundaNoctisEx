package io.github.luposolitario.immundanoctisex.ui.creation

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.data.model.HeroIcon
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.dice.RandomDiceRoller
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Creazione personaggio (UI.md §schermata 3), v0.1 funzionale: lupo/lupa,
// tiro stat, 5 discipline, specializzazione WEAPONSKILL a SCELTA (+ bottone
// caso), arma iniziale. Il Dado del Destino teatrale arriva in Fase 7:
// qui il tiro è un bottone.
@Composable
fun CharacterCreationScreen(
    state: CreationState,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.creation_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.creation_subtitle),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        GenderCard(state)
        HeroIconCard(state)
        StatsCard(state)
        DisciplinesCard(state)
        WeaponSkillCard(state)
        WeaponCard(state)
        SpecialItemCard(state)

        Button(
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = state.canProceed,
        ) {
            Text(stringResource(R.string.creation_start))
        }
    }
}

// Lupo o lupa coi RITRATTI (24/07/2026: sostituiti quelli di v1 —
// `class_warrior_male/female`, fotorealistici e fuori stile rispetto al
// resto del reskin a china — coi busti ritagliati da `hero.png`,
// stesso stile a china di tutte le altre icone): tocco sul ritratto,
// cerchio evidenziato sul selezionato.
@Composable
private fun GenderCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.creation_gender), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                PortraitOption(
                    imageRes = R.drawable.hero_portrait_male,
                    label = stringResource(R.string.creation_gender_male),
                    selected = state.gender == Gender.MALE,
                    onClick = { state.gender = Gender.MALE },
                )
                PortraitOption(
                    imageRes = R.drawable.hero_portrait_female,
                    label = stringResource(R.string.creation_gender_female),
                    selected = state.gender == Gender.FEMALE,
                    onClick = { state.gender = Gender.FEMALE },
                )
            }
            Spacer(Modifier.height(12.dp))
            // Nome personalizzabile (24/07/2026, richiesta Michele):
            // opzionale, il placeholder mostra già cosa succede se si
            // lascia vuoto. Non più "Lupo"/"Lupa" (24/07/2026, stesso
            // giorno, ora che l'icona è personalizzabile su 15 animali:
            // "Lupo" come nome non avrebbe più senso scegliendo un
            // drago) — "Eroe Solitario"/"Eroina Solitaria", generico
            // quanto basta per qualunque animale si scelga.
            OutlinedTextField(
                value = state.heroName,
                onValueChange = { state.heroName = it },
                label = { Text(stringResource(R.string.creation_name)) },
                placeholder = {
                    Text(
                        if (state.gender == Gender.MALE) {
                            stringResource(R.string.creation_name_default_male)
                        } else {
                            stringResource(R.string.creation_name_default_female)
                        },
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// Ritratto circolare selezionabile: bordo dorato sul selezionato (la
// convenzione oro di v1), grigio sugli altri.
@Composable
private fun PortraitOption(
    imageRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(if (selected) 4.dp else 1.dp, borderColor, CircleShape)
                .clickable(onClick = onClick),
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// Icona dell'eroe (24/07/2026, richiesta Michele: "facciamogli scegliere
// l'icona... devono essere tutti animali"), stessa cella/griglia già
// usata per le armi — solo WOLF ha un'illustrazione vera oggi
// (heroIconRes degrada sul lupo per tutte le altre, vedi CreationCatalog).
@Composable
private fun HeroIconCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.creation_icon), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                // Ancora troppo piccole a 72dp (24/07/2026, stesso
                // giorno, Michele: "quasi non distingui gli animali
                // diversi") — un altro salto, non un ritocco: 100dp,
                // celle minime da 120 a 140dp per farcele stare senza
                // schiacciarle.
                columns = GridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(560.dp),
            ) {
                items(HeroIcon.entries) { icon ->
                    WeaponCell(
                        iconRes = heroIconRes(icon),
                        nameRes = heroIconName(icon),
                        selected = state.heroIcon == icon,
                        onClick = { state.heroIcon = icon },
                        iconSize = 100.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.creation_stats_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (state.statsRolled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text(stringResource(R.string.creation_combat_skill, state.combatSkill), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.creation_endurance, state.endurance), fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.creation_gold, state.gold))
                Spacer(Modifier.height(8.dp))
            }
            // Statistiche già tirate all'apertura (CreationState.init,
            // 24/07/2026: "per velocizzare la creazione") — il bottone
            // resta SEMPRE attivo, mai un tiro solo: "si deve divertire
            // ma col giusto livello di aleatorietà" (richiesta esplicita
            // di Michele). L'etichetta cambia in "Ritira" perché non è
            // più il primo tiro.
            Button(onClick = { state.rollStats() }) {
                Text(
                    stringResource(
                        if (state.statsRolled) R.string.creation_reroll_stats else R.string.creation_roll_stats,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DisciplinesCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.creation_disciplines_title, state.selectedDisciplines.size),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(420.dp),
            ) {
                items(KAI_DISCIPLINES_UI) { discipline ->
                    val isSelected = state.selectedDisciplines.contains(discipline.id)
                    val enabled = isSelected || state.selectedDisciplines.size < 5
                    DisciplineCell(
                        discipline = discipline,
                        isSelected = isSelected,
                        enabled = enabled,
                        onClick = { state.toggleDiscipline(discipline.id) },
                    )
                }
            }
        }
    }
}

// Specializzazione WEAPONSKILL come MENU A TENDINA (richiesta Michele:
// accorcia la pagina) + bottone "scegli a caso". SEMPRE visibile:
// disabilitata con spiegazione finché la disciplina Scherma non è scelta
// (prima compariva solo con Scherma selezionata e sembrava sparita).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeaponSkillCard(state: CreationState) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = state.needsWeaponSkillChoice

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.creation_weaponskill_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    if (enabled) R.string.creation_weaponskill_pick else R.string.creation_weaponskill_locked,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded && enabled,
                onExpandedChange = { if (enabled) expanded = it },
            ) {
                OutlinedTextField(
                    value = state.weaponSkillType?.let { stringResource(weaponTypeName(it)) } ?: "—",
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded && enabled,
                    onDismissRequest = { expanded = false },
                ) {
                    WeaponType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(weaponTypeName(type))) },
                            onClick = {
                                state.weaponSkillType = type
                                expanded = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { state.rollRandomWeaponSkill() }, enabled = enabled) {
                Text(stringResource(R.string.creation_weaponskill_random))
            }
        }
    }
}

@Composable
private fun WeaponCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.creation_equipment_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.creation_pick_weapon), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            // Griglia a DUE colonne fisse, non "adattiva" (24/07/2026,
            // terzo giro: con minSize=190dp lo schermo ci faceva stare
            // una sola colonna, celle enormi con l'icona persa in mezzo
            // al vuoto — "puoi ingrandirle anche del doppio rispetto
            // allo spazio" era un sintomo della griglia sbagliata, non
            // (solo) dell'icona piccola). Due colonne = celle più strette
            // e più quadrate, l'icona le riempie per davvero.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(950.dp),
            ) {
                items(INITIAL_WEAPONS) { weapon ->
                    WeaponCell(
                        iconRes = weaponTypeIcon(weapon.weaponType),
                        nameRes = weaponTypeName(requireNotNull(weapon.weaponType)),
                        selected = state.selectedWeapon?.name == weapon.name,
                        onClick = { state.selectWeapon(weapon) },
                        // 120dp, proporzionata alla cella (metà schermo,
                        // non più tutta la larghezza) invece che a un
                        // valore fisso scelto alla cieca.
                        iconSize = 120.dp,
                        // Sfondo verde sull'arma della specializzazione
                        // WEAPONSKILL, se scelta (24/07/2026, richiesta
                        // Michele) — indipendente dal bordo oro
                        // dell'arma IMPUGNATA: due fatti diversi, si
                        // possono vedere insieme.
                        isSpecialization = weapon.weaponType == state.weaponSkillType,
                    )
                }
                // Arti marziali: si parte senza armi (con WEAPONSKILL+UNARMED
                // vale il +2 a mani nude).
                item {
                    WeaponCell(
                        iconRes = weaponTypeIcon(WeaponType.UNARMED),
                        nameRes = R.string.weapon_unarmed,
                        selected = state.fightsUnarmed,
                        onClick = { state.selectUnarmed() },
                        iconSize = 120.dp,
                        isSpecialization = state.weaponSkillType == WeaponType.UNARMED,
                    )
                }
            }
        }
    }
}

// UN oggetto speciale a scelta (come v1/canone libro 1) + promemoria dei
// comuni automatici (Pozione e due Pasti).
@Composable
private fun SpecialItemCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.creation_pick_special), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                INITIAL_SPECIAL_ITEMS.forEach { special ->
                    Column(Modifier.weight(1f)) {
                        WeaponCell(
                            iconRes = special.iconRes,
                            nameRes = special.nameRes,
                            selected = state.selectedSpecialItem == special,
                            onClick = { state.selectedSpecialItem = special },
                        )
                    }
                }
            }
            state.selectedSpecialItem?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(it.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.creation_common_items),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

// Cella arma: icona + nome, bordo oro sulla selezionata (stessa
// convenzione dei ritratti e dell'arma impugnata nella Scheda).
// iconSize parametrico: 48dp -> 64dp -> 96dp (24/07/2026, secondo giro:
// "aumenta ancora... non le distingui, almeno il 50%" — 96dp è
// esattamente +50% rispetto a 64dp). HeroIconCard resta a 100dp per gli
// animali, invariato.
@Composable
private fun WeaponCell(
    iconRes: Int,
    nameRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 96.dp,
    // Sfondo verde per l'arma della specializzazione WEAPONSKILL, se
    // scelta (24/07/2026, richiesta Michele) — indipendente dal bordo
    // oro di "impugnata ora": due fatti diversi (specializzazione vs
    // selezione corrente), si vedono insieme se coincidono.
    isSpecialization: Boolean = false,
) {
    val borderColor = if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(if (selected) 3.dp else 1.dp, borderColor),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = when {
                isSpecialization -> Color(0xFF2E7D32).copy(alpha = 0.35f)
                selected -> Color(0xFFFFD700).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = stringResource(nameRes),
                modifier = Modifier.size(iconSize),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(nameRes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DisciplineCell(
    discipline: DisciplineUi,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        enabled = enabled,
        onClick = onClick,
        label = {
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        imageVector = discipline.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(discipline.nameRes), fontWeight = FontWeight.Bold)
                }
                Text(
                    stringResource(discipline.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Preview(showBackground = true, name = "Creazione (scuro)", heightDp = 1400)
@Composable
private fun CharacterCreationPreview() {
    val dice: DiceRoller = RandomDiceRoller()
    ImmundaNoctisTheme(darkTheme = true) {
        val state = remember {
            CreationState(dice).apply {
                rollStats()
                toggleDiscipline("WEAPONSKILL")
                toggleDiscipline("HEALING")
            }
        }
        CharacterCreationScreen(state = state, onCreate = {})
    }
}

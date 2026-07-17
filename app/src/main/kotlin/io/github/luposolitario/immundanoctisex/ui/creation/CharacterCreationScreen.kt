package io.github.luposolitario.immundanoctisex.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
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
        StatsCard(state)
        DisciplinesCard(state)
        if (state.needsWeaponSkillChoice) WeaponSkillCard(state)
        WeaponCard(state)

        Button(
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = state.canProceed,
        ) {
            Text(stringResource(R.string.creation_start))
        }
    }
}

@Composable
private fun GenderCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.creation_gender), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = state.gender == Gender.MALE,
                    onClick = { state.gender = Gender.MALE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.creation_gender_male)) }
                SegmentedButton(
                    selected = state.gender == Gender.FEMALE,
                    onClick = { state.gender = Gender.FEMALE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.creation_gender_female)) }
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
            Button(onClick = { state.rollStats() }, enabled = !state.statsRolled) {
                Text(stringResource(R.string.creation_roll_stats))
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

@Composable
private fun WeaponSkillCard(state: CreationState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.creation_weaponskill_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.creation_weaponskill_pick), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    WeaponType.entries.forEach { type ->
                        FilterChip(
                            selected = state.weaponSkillType == type,
                            onClick = { state.weaponSkillType = type },
                            label = { Text(stringResource(weaponTypeName(type))) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { state.rollRandomWeaponSkill() }) {
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                INITIAL_WEAPONS.forEach { weapon ->
                    FilterChip(
                        selected = state.selectedWeapon?.name == weapon.name,
                        onClick = { state.selectedWeapon = weapon },
                        label = {
                            Text(stringResource(weaponTypeName(requireNotNull(weapon.weaponType))))
                        },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = disciplineIcon("WEAPONSKILL"),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
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

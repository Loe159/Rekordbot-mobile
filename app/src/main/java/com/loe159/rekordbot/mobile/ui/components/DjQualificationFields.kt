package com.loe159.rekordbot.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loe159.rekordbot.mobile.domain.model.DjQualificationOptions
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.ui.theme.MoodAmbient
import com.loe159.rekordbot.mobile.ui.theme.MoodCalm
import com.loe159.rekordbot.mobile.ui.theme.MoodDark
import com.loe159.rekordbot.mobile.ui.theme.MoodEnergetic
import com.loe159.rekordbot.mobile.ui.theme.MoodJoyful
import com.loe159.rekordbot.mobile.ui.theme.MoodMysterious
import com.loe159.rekordbot.mobile.ui.theme.MoodSad
import com.loe159.rekordbot.mobile.ui.theme.MoodSexy
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary

@Composable
fun DjQualificationFields(
    draft: TrackDraft,
    onDraftChange: (TrackDraft) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showContainer: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(if (showContainer) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Qualification DJ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Tout est facultatif. Appuie à nouveau sur un tag pour le retirer.",
                style = MaterialTheme.typography.bodySmall,
                color = RekordbotMutedText,
            )
            QualificationLabel("Énergie")
            ScrollableChips {
                DjQualificationOptions.energies.forEach { energy ->
                    FilterChip(
                        selected = draft.energy == energy,
                        onClick = {
                            onDraftChange(
                                draft.copy(energy = energy.takeUnless { draft.energy == energy }),
                            )
                        },
                        label = { Text("$energy ★") },
                        enabled = enabled,
                    )
                }
            }
            QualificationLabel("Mood")
            ScrollableChips {
                DjQualificationOptions.moods.forEach { mood ->
                    ColoredFilterChip(
                        label = mood,
                        selected = mood in draft.moods,
                        color = moodColor(mood),
                        enabled = enabled,
                        onClick = {
                            onDraftChange(draft.copy(moods = draft.moods.toggle(mood)))
                        },
                    )
                }
            }
            QualificationLabel("Situation")
            ScrollableChips {
                DjQualificationOptions.situations.forEach { situation ->
                    ColoredFilterChip(
                        label = situation,
                        selected = situation in draft.situations,
                        color = RekordbotPrimary,
                        enabled = enabled,
                        onClick = {
                            onDraftChange(
                                draft.copy(situations = draft.situations.toggle(situation)),
                            )
                        },
                    )
                }
            }
            QualificationLabel("DJs inspirants")
            ScrollableChips {
                DjQualificationOptions.inspirationalDjs.forEach { dj ->
                    ColoredFilterChip(
                        label = dj,
                        selected = dj in draft.inspirationalDjs,
                        color = RekordbotPrimary,
                        enabled = enabled,
                        onClick = {
                            onDraftChange(
                                draft.copy(
                                    inspirationalDjs = draft.inspirationalDjs.toggle(dj),
                                ),
                            )
                        },
                    )
                }
            }
            OutlinedTextField(
                value = draft.comment.orEmpty(),
                onValueChange = { onDraftChange(draft.copy(comment = it.ifBlank { null })) },
                label = { Text("Commentaire") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
            )
        }
    }

    if (showContainer) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, RekordbotBorder),
        ) { content() }
    } else {
        Column(modifier = modifier) { content() }
    }
}

@Composable
private fun QualificationLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ScrollableChips(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ColoredFilterChip(
    label: String,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White,
        ),
    )
}

private fun List<String>.toggle(value: String): List<String> =
    if (value in this) filterNot { it == value } else this + value

private fun moodColor(mood: String): Color = when (mood) {
    "Sexy" -> MoodSexy
    "Énergique" -> MoodEnergetic
    "Sombre" -> MoodDark
    "Joyeux" -> MoodJoyful
    "Ambiant" -> MoodAmbient
    "Calme" -> MoodCalm
    "Mystérieux" -> MoodMysterious
    "Triste" -> MoodSad
    else -> RekordbotPrimary
}

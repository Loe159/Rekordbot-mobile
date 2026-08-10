package com.loe159.rekordbot.mobile.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.ui.components.RekordbotPrimaryButton
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotError
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary
import com.loe159.rekordbot.mobile.ui.theme.RekordbotSuccess
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme

@Composable
fun SettingsRoute(
    configurationRepository: AirtableConfigurationRepository,
    airtableGateway: AirtableGateway,
    onBack: () -> Unit,
    onConfigurationSaved: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(configurationRepository, airtableGateway),
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.savedVersion) {
        if (state.savedVersion > 0) onConfigurationSaved()
    }

    SettingsScreen(
        state = state,
        onBack = onBack,
        onFieldChange = viewModel::updateField,
        onSave = viewModel::save,
        onTestConnection = viewModel::testConnection,
        onCreateDemoRecord = viewModel::createDemoRecord,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onFieldChange: (SettingsField, String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onCreateDemoRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDemoConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsHeader(onBack = onBack)
                AccessSection(state.configuration, onFieldChange)
                FieldMappingsSection(state.configuration, onFieldChange)
                DefaultsSection(state.configuration, onFieldChange)

                state.message?.let {
                    SettingsMessage(message = it, isError = state.isError)
                }

                RekordbotPrimaryButton(
                    label = if (state.isBusy) "Vérification…" else "Tester la connexion",
                    onClick = onTestConnection,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onSave,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("Enregistrer sans tester")
                }
                OutlinedButton(
                    onClick = { showDemoConfirmation = true },
                    enabled = state.isConnectionValidated && !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("Créer un enregistrement de démonstration")
                }
                Text(
                    text = "Le test de création reste désactivé tant que la connexion n’est pas validée.",
                    style = MaterialTheme.typography.labelMedium,
                    color = RekordbotMutedText,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showDemoConfirmation) {
        AlertDialog(
            onDismissRequest = { showDemoConfirmation = false },
            title = { Text("Créer un enregistrement ?") },
            text = {
                Text(
                    "Un vrai enregistrement nommé « Test Rekordbot Mobile » sera ajouté à la table Airtable.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDemoConfirmation = false
                        onCreateDemoRecord()
                    },
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirmation = false }) {
                    Text("Annuler")
                }
            },
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Retour")
        }
        Column {
            Text(
                text = "RÉGLAGES",
                style = MaterialTheme.typography.labelLarge,
                color = RekordbotPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Connexion Airtable",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun AccessSection(
    configuration: AirtableConfiguration,
    onFieldChange: (SettingsField, String) -> Unit,
) {
    SettingsSection(
        title = "Accès",
        description = "PAT requis : schema.bases:read et data.records:write, limité à ta base.",
    ) {
        SettingsTextField(
            label = "Personal Access Token",
            value = configuration.personalAccessToken,
            onValueChange = { onFieldChange(SettingsField.TOKEN, it) },
            isSecret = true,
        )
        SettingsTextField(
            label = "Base ID",
            value = configuration.baseId,
            onValueChange = { onFieldChange(SettingsField.BASE_ID, it) },
            supportingText = "Exemple : appXXXXXXXXXXXXXX",
        )
        SettingsTextField(
            label = "Table (nom ou ID)",
            value = configuration.table,
            onValueChange = { onFieldChange(SettingsField.TABLE, it) },
            supportingText = "Exemple : Morceaux ou tblXXXXXXXXXXXXXX",
        )
    }
}

@Composable
private fun FieldMappingsSection(
    configuration: AirtableConfiguration,
    onFieldChange: (SettingsField, String) -> Unit,
) {
    val fields = configuration.fields
    SettingsSection(
        title = "Correspondance des champs",
        description = "Les cinq premiers sont obligatoires. Laisse vide un champ optionnel absent de ta table.",
    ) {
        SettingsTextField("Titre", fields.title, { onFieldChange(SettingsField.TITLE, it) })
        SettingsTextField("Artiste", fields.artist, { onFieldChange(SettingsField.ARTIST, it) })
        SettingsTextField(
            "Lien Spotify",
            fields.spotifyUrl,
            { onFieldChange(SettingsField.SPOTIFY_URL, it) },
        )
        SettingsTextField(
            "Spotify Track ID",
            fields.spotifyTrackId,
            { onFieldChange(SettingsField.SPOTIFY_TRACK_ID, it) },
        )
        SettingsTextField("Statut", fields.status, { onFieldChange(SettingsField.STATUS, it) })
        SettingsTextField(
            "Genre brut (optionnel)",
            fields.rawGenre,
            { onFieldChange(SettingsField.RAW_GENRE, it) },
        )
        SettingsTextField(
            "Énergie (optionnel)",
            fields.energy,
            { onFieldChange(SettingsField.ENERGY, it) },
        )
        SettingsTextField("Mood (optionnel)", fields.mood, { onFieldChange(SettingsField.MOOD, it) })
        SettingsTextField(
            "Situation (optionnel)",
            fields.situation,
            { onFieldChange(SettingsField.SITUATION, it) },
        )
        SettingsTextField(
            "DJs inspirants (optionnel)",
            fields.inspirationalDjs,
            { onFieldChange(SettingsField.INSPIRATIONAL_DJS, it) },
        )
        SettingsTextField(
            "Commentaire (optionnel)",
            fields.comment,
            { onFieldChange(SettingsField.COMMENT, it) },
        )
        SettingsTextField(
            "Source (optionnel)",
            fields.source,
            { onFieldChange(SettingsField.SOURCE, it) },
        )
    }
}

@Composable
private fun DefaultsSection(
    configuration: AirtableConfiguration,
    onFieldChange: (SettingsField, String) -> Unit,
) {
    SettingsSection(
        title = "Valeurs par défaut",
        description = "Ces valeurs doivent exister si les champs Airtable sont des listes fermées.",
    ) {
        SettingsTextField(
            label = "Statut",
            value = configuration.defaultStatus,
            onValueChange = { onFieldChange(SettingsField.DEFAULT_STATUS, it) },
        )
        SettingsTextField(
            label = "Source",
            value = configuration.defaultSource,
            onValueChange = { onFieldChange(SettingsField.DEFAULT_SOURCE, it) },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RekordbotBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = RekordbotMutedText,
            )
            content()
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    isSecret: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = supportingText?.let { text -> { Text(text) } },
        visualTransformation = if (isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun SettingsMessage(
    message: String,
    isError: Boolean,
) {
    val color = if (isError) RekordbotError else RekordbotSuccess
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun SettingsScreenPreview() {
    RekordbotTheme {
        SettingsScreen(
            state = SettingsUiState(isLoading = false),
            onBack = {},
            onFieldChange = { _, _ -> },
            onSave = {},
            onTestConnection = {},
            onCreateDemoRecord = {},
        )
    }
}

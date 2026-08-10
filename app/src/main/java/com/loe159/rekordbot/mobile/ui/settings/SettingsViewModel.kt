package com.loe159.rekordbot.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableConfigurationValidator
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val configurationRepository: AirtableConfigurationRepository,
    private val airtableGateway: AirtableGateway,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                configurationRepository.load() to configurationRepository.isConnectionValidated()
            }
                .onSuccess { (configuration, isConnectionValidated) ->
                    mutableState.update {
                        it.copy(
                            configuration = configuration,
                            isLoading = false,
                            isConnectionValidated = isConnectionValidated,
                        )
                    }
                }
                .onFailure {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            message = "Impossible de lire la configuration enregistrée.",
                            isError = true,
                        )
                    }
                }
        }
    }

    fun updateField(field: SettingsField, value: String) {
        mutableState.update { current ->
            current.copy(
                configuration = current.configuration.withField(field, value),
                isConnectionValidated = false,
                message = null,
                isError = false,
            )
        }
    }

    fun save() {
        val configuration = state.value.configuration
        val errors = AirtableConfigurationValidator.localErrors(configuration)
        if (errors.isNotEmpty()) {
            showErrors(errors)
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, message = null) }
            runCatching { configurationRepository.save(configuration) }
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isBusy = false,
                            isConnectionValidated = false,
                            message = "Configuration enregistrée sur ce téléphone.",
                            isError = false,
                            savedVersion = it.savedVersion + 1,
                        )
                    }
                }
                .onFailure { error -> showFailure(error, "Impossible d’enregistrer la configuration.") }
        }
    }

    fun testConnection() {
        val configuration = state.value.configuration
        val errors = AirtableConfigurationValidator.localErrors(configuration)
        if (errors.isNotEmpty()) {
            showErrors(errors)
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, message = null) }
            airtableGateway.fetchTableSchema(configuration)
                .onSuccess { schema ->
                    val schemaErrors = AirtableConfigurationValidator.schemaErrors(configuration, schema)
                    if (schemaErrors.isNotEmpty()) {
                        showErrors(schemaErrors)
                    } else {
                        runCatching { configurationRepository.save(configuration) }
                            .onSuccess {
                                configurationRepository.markConnectionValidated()
                                mutableState.update {
                                    it.copy(
                                        isBusy = false,
                                        isConnectionValidated = true,
                                        message = "Connexion validée : ${schema.name}, ${schema.fields.size} champs détectés.",
                                        isError = false,
                                        savedVersion = it.savedVersion + 1,
                                    )
                                }
                            }
                            .onFailure { error ->
                                showFailure(error, "Connexion valide, mais enregistrement local impossible.")
                            }
                    }
                }
                .onFailure { error -> showFailure(error, "Connexion Airtable impossible.") }
        }
    }

    fun createDemoRecord() {
        val current = state.value
        if (!current.isConnectionValidated || current.isBusy) return

        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, message = null) }
            airtableGateway.createDemoRecord(current.configuration)
                .onSuccess { recordId ->
                    mutableState.update {
                        it.copy(
                            isBusy = false,
                            message = "Enregistrement de démonstration créé : $recordId",
                            isError = false,
                        )
                    }
                }
                .onFailure { error -> showFailure(error, "Création du test impossible.") }
        }
    }

    private fun showErrors(errors: List<String>) {
        mutableState.update {
            it.copy(
                isBusy = false,
                isConnectionValidated = false,
                message = errors.joinToString("\n"),
                isError = true,
            )
        }
    }

    private fun showFailure(error: Throwable, fallbackMessage: String) {
        mutableState.update {
            it.copy(
                isBusy = false,
                isConnectionValidated = false,
                message = error.message?.takeIf(String::isNotBlank) ?: fallbackMessage,
                isError = true,
            )
        }
    }

    private fun AirtableConfiguration.withField(
        field: SettingsField,
        value: String,
    ): AirtableConfiguration = when (field) {
        SettingsField.TOKEN -> copy(personalAccessToken = value)
        SettingsField.BASE_ID -> copy(baseId = value)
        SettingsField.TABLE -> copy(table = value)
        SettingsField.TITLE -> copy(fields = fields.copy(title = value))
        SettingsField.ARTIST -> copy(fields = fields.copy(artist = value))
        SettingsField.SPOTIFY_URL -> copy(fields = fields.copy(spotifyUrl = value))
        SettingsField.SPOTIFY_TRACK_ID -> copy(fields = fields.copy(spotifyTrackId = value))
        SettingsField.STATUS -> copy(fields = fields.copy(status = value))
        SettingsField.RAW_GENRE -> copy(fields = fields.copy(rawGenre = value))
        SettingsField.ENERGY -> copy(fields = fields.copy(energy = value))
        SettingsField.MOOD -> copy(fields = fields.copy(mood = value))
        SettingsField.SITUATION -> copy(fields = fields.copy(situation = value))
        SettingsField.INSPIRATIONAL_DJS -> copy(fields = fields.copy(inspirationalDjs = value))
        SettingsField.COMMENT -> copy(fields = fields.copy(comment = value))
        SettingsField.SOURCE -> copy(fields = fields.copy(source = value))
        SettingsField.DEFAULT_STATUS -> copy(defaultStatus = value)
        SettingsField.DEFAULT_SOURCE -> copy(defaultSource = value)
    }

    companion object {
        fun factory(
            configurationRepository: AirtableConfigurationRepository,
            airtableGateway: AirtableGateway,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(configurationRepository, airtableGateway) as T
        }
    }
}

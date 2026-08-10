package com.loe159.rekordbot.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableConfigurationValidator
import com.loe159.rekordbot.mobile.domain.model.DuplicateStrategy
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.SoundchartsConfigurationRepository
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfigurationValidator
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val configurationRepository: AirtableConfigurationRepository,
    private val airtableGateway: AirtableGateway,
    private val soundchartsConfigurationRepository: SoundchartsConfigurationRepository,
    private val soundchartsGateway: SoundchartsGateway,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                Triple(
                    configurationRepository.load(),
                    configurationRepository.isConnectionValidated(),
                    soundchartsConfigurationRepository.load(),
                )
            }
                .onSuccess { (configuration, isConnectionValidated, soundchartsConfiguration) ->
                    mutableState.update {
                        it.copy(
                            configuration = configuration,
                            soundchartsConfiguration = soundchartsConfiguration,
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

    fun setSoundchartsEnabled(enabled: Boolean) {
        mutableState.update {
            it.copy(
                soundchartsConfiguration = it.soundchartsConfiguration.copy(enabled = enabled),
                soundchartsMessage = null,
                isSoundchartsError = false,
            )
        }
    }

    fun updateSoundchartsAppId(value: String) {
        mutableState.update {
            it.copy(
                soundchartsConfiguration = it.soundchartsConfiguration.copy(appId = value),
                soundchartsMessage = null,
                isSoundchartsError = false,
            )
        }
    }

    fun updateSoundchartsApiKey(value: String) {
        mutableState.update {
            it.copy(
                soundchartsConfiguration = it.soundchartsConfiguration.copy(apiKey = value),
                soundchartsMessage = null,
                isSoundchartsError = false,
            )
        }
    }

    fun saveSoundcharts() {
        val configuration = state.value.soundchartsConfiguration
        val errors = SoundchartsConfigurationValidator.localErrors(configuration)
        if (errors.isNotEmpty()) {
            showSoundchartsErrors(errors)
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSoundchartsBusy = true, soundchartsMessage = null) }
            runCatching { soundchartsConfigurationRepository.save(configuration) }
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isSoundchartsBusy = false,
                            soundchartsMessage = if (configuration.enabled) {
                                "Configuration Soundcharts enregistrée."
                            } else {
                                "Enrichissement Soundcharts désactivé."
                            },
                            isSoundchartsError = false,
                            savedVersion = it.savedVersion + 1,
                        )
                    }
                }
                .onFailure { showSoundchartsFailure("Enregistrement Soundcharts impossible.") }
        }
    }

    fun testSoundchartsConnection() {
        val configuration = state.value.soundchartsConfiguration
        val errors = SoundchartsConfigurationValidator.localErrors(
            configuration.copy(enabled = true),
        )
        if (errors.isNotEmpty()) {
            showSoundchartsErrors(errors)
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSoundchartsBusy = true, soundchartsMessage = null) }
            soundchartsGateway.fetchTrackMetadata(SOUNDCHARTS_TEST_SPOTIFY_ID, configuration)
                .onSuccess {
                    runCatching { soundchartsConfigurationRepository.save(configuration) }
                        .onSuccess {
                            mutableState.update {
                                it.copy(
                                    isSoundchartsBusy = false,
                                    soundchartsMessage = "Connexion Soundcharts validée.",
                                    isSoundchartsError = false,
                                    savedVersion = it.savedVersion + 1,
                                )
                            }
                        }
                        .onFailure { showSoundchartsFailure("Connexion valide, mais enregistrement impossible.") }
                }
                .onFailure { error ->
                    showSoundchartsFailure(
                        error.message ?: "Connexion Soundcharts impossible.",
                    )
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

    fun updateDuplicateStrategy(strategy: DuplicateStrategy) {
        mutableState.update { current ->
            current.copy(
                configuration = current.configuration.copy(duplicateStrategy = strategy),
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

    private fun showSoundchartsErrors(errors: List<String>) {
        mutableState.update {
            it.copy(
                isSoundchartsBusy = false,
                soundchartsMessage = errors.joinToString("\n"),
                isSoundchartsError = true,
            )
        }
    }

    private fun showSoundchartsFailure(message: String) {
        mutableState.update {
            it.copy(
                isSoundchartsBusy = false,
                soundchartsMessage = message,
                isSoundchartsError = true,
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
        SettingsField.ISRC -> copy(fields = fields.copy(isrc = value))
        SettingsField.STATUS -> copy(fields = fields.copy(status = value))
        SettingsField.REKORDBOT_STATE -> copy(fields = fields.copy(rekordbotState = value))
        SettingsField.REKORDBOT_ERROR -> copy(fields = fields.copy(rekordbotError = value))
        SettingsField.LAST_SYNC -> copy(fields = fields.copy(lastSync = value))
        SettingsField.MATCHING_METHOD -> copy(fields = fields.copy(matchingMethod = value))
        SettingsField.RAW_GENRE -> copy(fields = fields.copy(rawGenre = value))
        SettingsField.ENERGY -> copy(fields = fields.copy(energy = value))
        SettingsField.MOOD -> copy(fields = fields.copy(mood = value))
        SettingsField.SITUATION -> copy(fields = fields.copy(situation = value))
        SettingsField.INSPIRATIONAL_DJS -> copy(fields = fields.copy(inspirationalDjs = value))
        SettingsField.COMMENT -> copy(fields = fields.copy(comment = value))
        SettingsField.SOURCE -> copy(fields = fields.copy(source = value))
        SettingsField.DEFAULT_STATUS -> copy(defaultStatus = value)
        SettingsField.DEFAULT_SOURCE -> copy(defaultSource = value)
        SettingsField.DEFAULT_REKORDBOT_STATE -> copy(defaultRekordbotState = value)
    }

    companion object {
        fun factory(
            configurationRepository: AirtableConfigurationRepository,
            airtableGateway: AirtableGateway,
            soundchartsConfigurationRepository: SoundchartsConfigurationRepository,
            soundchartsGateway: SoundchartsGateway,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(
                    configurationRepository,
                    airtableGateway,
                    soundchartsConfigurationRepository,
                    soundchartsGateway,
                ) as T
        }

        private const val SOUNDCHARTS_TEST_SPOTIFY_ID = "4uLU6hMCjMI75M1A2tKUQC"
    }
}

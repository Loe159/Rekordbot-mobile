package com.loe159.rekordbot.mobile.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableOAuthClient
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableResourceGateway
import com.loe159.rekordbot.mobile.domain.airtable.AirtableAuthorizationCallback
import com.loe159.rekordbot.mobile.domain.airtable.AirtableOAuthConfiguration
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTableSummary
import com.loe159.rekordbot.mobile.domain.model.AirtableAuthenticationMode
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableConfigurationValidator
import com.loe159.rekordbot.mobile.domain.model.DuplicateStrategy
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.AirtableSessionRepository
import com.loe159.rekordbot.mobile.domain.repository.SoundchartsConfigurationRepository
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfigurationValidator
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsGateway
import com.loe159.rekordbot.mobile.ui.toUserFacingMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val configurationRepository: AirtableConfigurationRepository,
    private val airtableGateway: AirtableGateway,
    private val airtableResourceGateway: AirtableResourceGateway,
    private val airtableSessionRepository: AirtableSessionRepository,
    private val airtableOAuthConfiguration: AirtableOAuthConfiguration,
    private val soundchartsConfigurationRepository: SoundchartsConfigurationRepository,
    private val soundchartsGateway: SoundchartsGateway,
    private val airtableOAuthClientFactory: (AirtableOAuthConfiguration) -> AirtableOAuthClient = {
        AirtableOAuthClient(it)
    },
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
                            isOAuthAvailable = airtableOAuthConfiguration.clientId.isNotBlank() &&
                                AirtableAuthorizationCallback.isSecureRedirectUri(
                                    airtableOAuthConfiguration.redirectUri,
                                ),
                            isOAuthConnected = airtableSessionRepository.loadTokens() != null,
                        )
                    }
                    if (
                        configuration.authenticationMode == AirtableAuthenticationMode.OAUTH &&
                        airtableSessionRepository.loadTokens() != null
                    ) {
                        loadAirtableResources(configuration)
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

    fun beginAirtableConnection(openAuthorizationUrl: (String) -> Unit) {
        if (
            airtableOAuthConfiguration.clientId.isBlank() ||
            !AirtableAuthorizationCallback.isSecureRedirectUri(
                airtableOAuthConfiguration.redirectUri,
            )
        ) {
            showErrors(listOf("La connexion Airtable n’est pas configurée dans cette build."))
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isOAuthBusy = true, message = null, isError = false) }
            runCatching {
                val session = airtableOAuthClientFactory(airtableOAuthConfiguration)
                    .createAuthorizationSession()
                airtableSessionRepository.savePendingAuthorization(session)
                session.authorizationUrl
            }.onSuccess { url ->
                mutableState.update { it.copy(isOAuthBusy = false) }
                openAuthorizationUrl(url)
            }.onFailure { error ->
                showFailure(error, "Impossible de démarrer la connexion Airtable.")
            }
        }
    }

    fun handleAirtableAuthorizationCallback(callbackUri: String) {
        viewModelScope.launch {
            mutableState.update { it.copy(isOAuthBusy = true, message = null, isError = false) }
            val result = runCatching {
                val callback = Uri.parse(callbackUri)
                callback.getQueryParameter("error")?.let {
                    throw IllegalStateException("Autorisation Airtable refusée.")
                }
                val code = callback.getQueryParameter("code").orEmpty()
                val returnedState = callback.getQueryParameter("state")
                val pending = airtableSessionRepository.loadPendingAuthorization()
                    ?: throw IllegalStateException("Connexion Airtable expirée. Recommence.")
                airtableOAuthClientFactory(airtableOAuthConfiguration)
                    .exchangeAuthorizationCode(code, returnedState, pending)
                    .getOrThrow()
            }
            airtableSessionRepository.clearPendingAuthorization()
            result.onSuccess { tokens ->
                airtableSessionRepository.saveTokens(tokens)
                val configuration = state.value.configuration.copy(
                    authenticationMode = AirtableAuthenticationMode.OAUTH,
                    personalAccessToken = "",
                )
                configurationRepository.save(configuration)
                mutableState.update {
                    it.copy(
                        configuration = configuration,
                        isOAuthConnected = true,
                        isOAuthBusy = false,
                        isConnectionValidated = false,
                        message = "Airtable connecté. Choisis la base et la table.",
                        isError = false,
                        savedVersion = it.savedVersion + 1,
                    )
                }
                loadAirtableResources(configuration)
            }.onFailure { error ->
                showFailure(error, "Connexion Airtable impossible.")
            }
        }
    }

    fun disconnectAirtable() {
        viewModelScope.launch {
            mutableState.update { it.copy(isOAuthBusy = true, message = null) }
            runCatching {
                airtableSessionRepository.clearTokens()
                val configuration = state.value.configuration.copy(
                    authenticationMode = AirtableAuthenticationMode.OAUTH,
                )
                configurationRepository.save(configuration)
                configuration
            }.onSuccess { configuration ->
                mutableState.update {
                    it.copy(
                        configuration = configuration,
                        isOAuthConnected = false,
                        isOAuthBusy = false,
                        isConnectionValidated = false,
                        availableBases = emptyList(),
                        availableTables = emptyList(),
                        message = "Compte Airtable déconnecté de l’application.",
                        isError = false,
                        savedVersion = it.savedVersion + 1,
                    )
                }
            }.onFailure { error ->
                showFailure(error, "Déconnexion Airtable impossible.")
            }
        }
    }

    fun usePersonalAccessTokenMode() {
        mutableState.update {
            it.copy(
                configuration = it.configuration.copy(
                    authenticationMode = AirtableAuthenticationMode.PERSONAL_ACCESS_TOKEN,
                ),
                isConnectionValidated = false,
                message = null,
                isError = false,
            )
        }
    }

    fun useOAuthMode() {
        mutableState.update {
            it.copy(
                configuration = it.configuration.copy(
                    authenticationMode = AirtableAuthenticationMode.OAUTH,
                ),
                isConnectionValidated = false,
                message = null,
                isError = false,
            )
        }
    }

    fun selectAirtableBase(baseId: String) {
        val configuration = state.value.configuration.copy(baseId = baseId, table = "")
        mutableState.update {
            it.copy(
                configuration = configuration,
                availableTables = emptyList(),
                isConnectionValidated = false,
                message = null,
            )
        }
        viewModelScope.launch { loadAirtableTables(configuration, baseId) }
    }

    fun selectAirtableTable(tableId: String) {
        mutableState.update {
            it.copy(
                configuration = it.configuration.copy(table = tableId),
                isConnectionValidated = false,
                message = null,
            )
        }
    }

    fun refreshAirtableResources() {
        viewModelScope.launch { loadAirtableResources(state.value.configuration) }
    }

    private suspend fun loadAirtableResources(configuration: AirtableConfiguration) {
        mutableState.update { it.copy(isOAuthBusy = true) }
        airtableResourceGateway.listBases(configuration)
            .onSuccess { bases ->
                val selectedBaseId = configuration.baseId.takeIf { configured ->
                    bases.any { it.id == configured }
                } ?: bases.singleOrNull()?.id.orEmpty()
                val updatedConfiguration = configuration.copy(baseId = selectedBaseId)
                mutableState.update {
                    it.copy(
                        configuration = updatedConfiguration,
                        availableBases = bases,
                        isOAuthBusy = selectedBaseId.isNotBlank(),
                        message = if (bases.isEmpty()) {
                            "Aucune base n’a été autorisée dans Airtable."
                        } else {
                            it.message
                        },
                    )
                }
                if (selectedBaseId.isNotBlank()) {
                    loadAirtableTables(updatedConfiguration, selectedBaseId)
                }
            }
            .onFailure { error ->
                showFailure(error, "Impossible de lister les bases Airtable.")
            }
    }

    private suspend fun loadAirtableTables(
        configuration: AirtableConfiguration,
        baseId: String,
    ) {
        mutableState.update { it.copy(isOAuthBusy = true) }
        airtableResourceGateway.listTables(configuration, baseId)
            .onSuccess { tables -> applyAirtableTables(configuration, tables) }
            .onFailure { error ->
                showFailure(error, "Impossible de lister les tables Airtable.")
            }
    }

    private fun applyAirtableTables(
        configuration: AirtableConfiguration,
        tables: List<AirtableTableSummary>,
    ) {
        val selectedTableId = configuration.table.takeIf { configured ->
            tables.any { it.id == configured }
        } ?: tables.firstOrNull { it.name.equals("Sons", ignoreCase = true) }?.id
            ?: tables.singleOrNull()?.id.orEmpty()
        mutableState.update {
            it.copy(
                configuration = configuration.copy(table = selectedTableId),
                availableTables = tables,
                isOAuthBusy = false,
            )
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
                        error.toUserFacingMessage(
                            "Connexion Soundcharts impossible. Vérifie les identifiants et le réseau.",
                        ),
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
                isOAuthBusy = false,
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
                isOAuthBusy = false,
                isConnectionValidated = false,
                message = error.toUserFacingMessage(fallbackMessage),
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
            airtableResourceGateway: AirtableResourceGateway,
            airtableSessionRepository: AirtableSessionRepository,
            airtableOAuthConfiguration: AirtableOAuthConfiguration,
            soundchartsConfigurationRepository: SoundchartsConfigurationRepository,
            soundchartsGateway: SoundchartsGateway,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(
                    configurationRepository,
                    airtableGateway,
                    airtableResourceGateway,
                    airtableSessionRepository,
                    airtableOAuthConfiguration,
                    soundchartsConfigurationRepository,
                    soundchartsGateway,
                ) as T
        }

        private const val SOUNDCHARTS_TEST_SPOTIFY_ID = "4uLU6hMCjMI75M1A2tKUQC"
    }
}

package com.loe159.rekordbot.mobile.ui.shazam

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyApiException
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyOAuthClient
import com.loe159.rekordbot.mobile.domain.repository.ShazamInboxRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifySessionRepository
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistSynchronizer
import com.loe159.rekordbot.mobile.domain.shazam.ShazamSpotifyPlaybackController
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfigurationValidator
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackAction
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackAuthorizationRequiredException
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShazamViewModel(
    private val configurationRepository: SpotifyConfigurationRepository,
    private val sessionRepository: SpotifySessionRepository,
    private val inboxRepository: ShazamInboxRepository,
    private val synchronizer: ShazamPlaylistSynchronizer,
    private val playbackController: ShazamSpotifyPlaybackController,
    private val bundledClientId: String = "",
    private val oauthClientFactory: (SpotifyConfiguration) -> SpotifyOAuthClient = {
        SpotifyOAuthClient(it)
    },
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ShazamUiState(
            isLoading = true,
            isClientIdManagedByApp = bundledClientId.isNotBlank(),
        ),
    )
    val state: StateFlow<ShazamUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                inboxRepository.observePending(),
                inboxRepository.observeIgnored(),
                inboxRepository.observeCounts(),
            ) { pending, ignored, counts -> Triple(pending, ignored, counts) }
                .collect { (pending, ignored, counts) ->
                    mutableState.update {
                        it.copy(
                            pendingTracks = pending,
                            ignoredTracks = ignored,
                            counts = counts,
                        )
                    }
                }
        }
        viewModelScope.launch {
            runCatching {
                configurationRepository.load() to sessionRepository.loadTokens()
            }.onSuccess { (configuration, tokens) ->
                mutableState.update {
                    it.copy(
                        clientId = configuration.clientId,
                        playlistName = configuration.playlistName,
                        isConnected = tokens != null,
                        hasPlaybackPermission = SpotifyScopes.PLAYBACK_CONTROL in
                            tokens?.scopes.orEmpty(),
                        isLoading = false,
                    )
                }
            }.onFailure {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Impossible de lire la configuration Spotify.",
                    )
                }
            }
        }
    }

    fun updateClientId(value: String) = updateEditableState { copy(clientId = value) }

    fun updatePlaylistName(value: String) = updateEditableState { copy(playlistName = value) }

    fun selectTab(tab: ShazamInboxTab) {
        mutableState.update { it.copy(selectedTab = tab) }
    }

    fun saveConfiguration() {
        val configuration = currentConfiguration()
        val errors = SpotifyConfigurationValidator.localErrors(configuration)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(errorMessage = errors.joinToString("\n"), message = null) }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(isSavingConfiguration = true, errorMessage = null, message = null)
            }
            runCatching { configurationRepository.save(configuration) }
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isSavingConfiguration = false,
                            message = "Configuration Spotify enregistrée.",
                        )
                    }
                }
                .onFailure {
                    mutableState.update {
                        it.copy(
                            isSavingConfiguration = false,
                            errorMessage = "Impossible d’enregistrer la configuration Spotify.",
                        )
                    }
                }
        }
    }

    fun beginConnection(openAuthorizationUrl: (String) -> Unit) {
        val configuration = currentConfiguration()
        val errors = SpotifyConfigurationValidator.localErrors(configuration)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(errorMessage = errors.joinToString("\n"), message = null) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isConnecting = true, errorMessage = null, message = null) }
            runCatching {
                configurationRepository.save(configuration)
                val session = oauthClientFactory(configuration).createAuthorizationSession()
                sessionRepository.savePendingAuthorization(session)
                session.authorizationUrl
            }.onSuccess { authorizationUrl ->
                mutableState.update { it.copy(isConnecting = false) }
                openAuthorizationUrl(authorizationUrl)
            }.onFailure {
                mutableState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = "Impossible de démarrer la connexion Spotify.",
                    )
                }
            }
        }
    }

    fun handleAuthorizationCallback(callbackUri: String) {
        viewModelScope.launch {
            mutableState.update { it.copy(isConnecting = true, errorMessage = null, message = null) }
            val result = runCatching {
                val callback = Uri.parse(callbackUri)
                callback.getQueryParameter("error")?.let {
                    throw IllegalStateException("Autorisation Spotify refusée.")
                }
                val code = callback.getQueryParameter("code").orEmpty()
                val returnedState = callback.getQueryParameter("state")
                val configuration = configurationRepository.load()
                val pendingSession = sessionRepository.loadPendingAuthorization()
                    ?: throw IllegalStateException("Connexion Spotify expirée. Recommence.")
                oauthClientFactory(configuration)
                    .exchangeAuthorizationCode(code, returnedState, pendingSession)
                    .getOrThrow()
            }
            sessionRepository.clearPendingAuthorization()
            result.onSuccess { tokens ->
                sessionRepository.saveTokens(tokens)
                mutableState.update {
                    it.copy(
                        isConnected = true,
                        hasPlaybackPermission = SpotifyScopes.PLAYBACK_CONTROL in tokens.scopes,
                        isConnecting = false,
                        message = "Spotify connecté. Synchronisation de Shazam…",
                    )
                }
                synchronize()
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = error.errorMessageForSpotifyLogin(),
                    )
                }
            }
        }
    }

    fun synchronize() {
        if (state.value.isSyncing) return
        viewModelScope.launch {
            mutableState.update { it.copy(isSyncing = true, errorMessage = null, message = null) }
            synchronizer.synchronize()
                .onSuccess { summary ->
                    mutableState.update {
                        it.copy(
                            isSyncing = false,
                            isConnected = true,
                            connectedPlaylistName = summary.playlistName,
                            message = "${summary.addedCount} nouveau(x) morceau(x), " +
                                "${summary.refreshedCount} déjà connu(s).",
                        )
                    }
                }
                .onFailure { error ->
                    val authorizationExpired = error is SpotifyApiException &&
                        (error.statusCode == 400 || error.statusCode == 401)
                    if (authorizationExpired) sessionRepository.clearTokens()
                    mutableState.update {
                        it.copy(
                            isSyncing = false,
                            isConnected = if (authorizationExpired) false else it.isConnected,
                            errorMessage = error.message
                                ?.takeIf(String::isNotBlank)
                                ?: "Synchronisation Shazam impossible.",
                        )
                    }
                }
        }
    }

    fun ignore(track: ShazamInboxTrack) = updateDecision {
        inboxRepository.markIgnored(track.spotifyTrackId)
    }

    fun restore(track: ShazamInboxTrack) = updateDecision {
        inboxRepository.reopen(track.spotifyTrackId)
    }

    fun togglePreview(track: ShazamInboxTrack) {
        if (state.value.playbackBusyTrackId != null) return
        viewModelScope.launch {
            val wasPlaying = state.value.playingTrackId == track.spotifyTrackId
            mutableState.update {
                it.copy(
                    playbackBusyTrackId = track.spotifyTrackId,
                    errorMessage = null,
                    message = null,
                )
            }
            playbackController.toggle(track.spotifyTrackId, wasPlaying)
                .onSuccess { action ->
                    mutableState.update {
                        it.copy(
                            playingTrackId = when (action) {
                                SpotifyPlaybackAction.PLAYING -> track.spotifyTrackId
                                SpotifyPlaybackAction.PAUSED -> null
                            },
                            playbackBusyTrackId = null,
                        )
                    }
                }
                .onFailure { error ->
                    val authorizationRequired =
                        error is SpotifyPlaybackAuthorizationRequiredException
                    val authorizationExpired = error is SpotifyApiException &&
                        error.statusCode == 401
                    if (authorizationExpired) sessionRepository.clearTokens()
                    mutableState.update {
                        it.copy(
                            isConnected = if (authorizationExpired) false else it.isConnected,
                            hasPlaybackPermission = if (
                                authorizationRequired || authorizationExpired
                            ) {
                                false
                            } else {
                                it.hasPlaybackPermission
                            },
                            playbackBusyTrackId = null,
                            errorMessage = error.message
                                ?.takeIf(String::isNotBlank)
                                ?: "Pré-écoute Spotify impossible.",
                        )
                    }
                }
        }
    }

    private fun updateDecision(action: suspend () -> Boolean) {
        viewModelScope.launch {
            runCatching { action() }.onFailure {
                mutableState.update { state ->
                    state.copy(errorMessage = "Impossible de conserver cette décision.")
                }
            }
        }
    }

    private fun updateEditableState(transform: ShazamUiState.() -> ShazamUiState) {
        mutableState.update {
            if (it.isBusy) it else transform(it).copy(message = null, errorMessage = null)
        }
    }

    private fun currentConfiguration(): SpotifyConfiguration = SpotifyConfiguration(
        clientId = state.value.clientId.trim(),
        playlistName = state.value.playlistName.trim(),
    )

    companion object {
        fun factory(
            configurationRepository: SpotifyConfigurationRepository,
            sessionRepository: SpotifySessionRepository,
            inboxRepository: ShazamInboxRepository,
            synchronizer: ShazamPlaylistSynchronizer,
            playbackController: ShazamSpotifyPlaybackController,
            bundledClientId: String = "",
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ShazamViewModel(
                configurationRepository = configurationRepository,
                sessionRepository = sessionRepository,
                inboxRepository = inboxRepository,
                synchronizer = synchronizer,
                playbackController = playbackController,
                bundledClientId = bundledClientId,
            ) as T
        }
    }
}

private fun Throwable.errorMessageForSpotifyLogin(): String = message
    ?.takeIf { it.startsWith("Autorisation Spotify") || it.startsWith("Connexion Spotify") }
    ?: "Connexion Spotify impossible. Vérifie le Client ID et la Redirect URI."

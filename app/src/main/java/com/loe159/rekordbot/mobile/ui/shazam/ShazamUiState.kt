package com.loe159.rekordbot.mobile.ui.shazam

import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxCounts
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration

enum class ShazamInboxTab {
    PENDING,
    IGNORED,
}

data class ShazamUiState(
    val clientId: String = "",
    val isClientIdManagedByApp: Boolean = false,
    val playlistName: String = SpotifyConfiguration.DEFAULT_SHAZAM_PLAYLIST_NAME,
    val isConnected: Boolean = false,
    val hasPlaybackPermission: Boolean = false,
    val accountName: String? = null,
    val connectedPlaylistName: String? = null,
    val counts: ShazamInboxCounts = ShazamInboxCounts(
        pending = 0,
        saved = 0,
        ignored = 0,
        alreadyPresent = 0,
    ),
    val pendingTracks: List<ShazamInboxTrack> = emptyList(),
    val ignoredTracks: List<ShazamInboxTrack> = emptyList(),
    val selectedTab: ShazamInboxTab = ShazamInboxTab.PENDING,
    val isLoading: Boolean = false,
    val isSavingConfiguration: Boolean = false,
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val playingTrackId: String? = null,
    val playbackBusyTrackId: String? = null,
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val visibleTracks: List<ShazamInboxTrack>
        get() = when (selectedTab) {
            ShazamInboxTab.PENDING -> pendingTracks
            ShazamInboxTab.IGNORED -> ignoredTracks
        }

    val isBusy: Boolean
        get() = isLoading || isSavingConfiguration || isConnecting || isSyncing

    val hasUsableConfiguration: Boolean
        get() = clientId.isNotBlank() && playlistName.isNotBlank()
}

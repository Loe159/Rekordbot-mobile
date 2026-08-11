package com.loe159.rekordbot.mobile.ui.shazam

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loe159.rekordbot.mobile.domain.repository.ShazamInboxRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifySessionRepository
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistSynchronizer
import com.loe159.rekordbot.mobile.domain.shazam.ShazamSpotifyPlaybackController

@Composable
fun ShazamRoute(
    configurationRepository: SpotifyConfigurationRepository,
    sessionRepository: SpotifySessionRepository,
    inboxRepository: ShazamInboxRepository,
    synchronizer: ShazamPlaylistSynchronizer,
    playbackController: ShazamSpotifyPlaybackController,
    authorizationCallback: String?,
    onAuthorizationCallbackConsumed: () -> Unit,
    onConnectionAvailable: () -> Unit,
    onPrepare: (ShazamInboxTrack) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ShazamViewModel = viewModel(
        factory = ShazamViewModel.factory(
            configurationRepository = configurationRepository,
            sessionRepository = sessionRepository,
            inboxRepository = inboxRepository,
            synchronizer = synchronizer,
            playbackController = playbackController,
        ),
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) onConnectionAvailable()
    }

    LaunchedEffect(authorizationCallback) {
        authorizationCallback?.let {
            viewModel.handleAuthorizationCallback(it)
            onAuthorizationCallbackConsumed()
        }
    }

    ShazamScreen(
        state = state,
        onBack = onBack,
        onClientIdChange = viewModel::updateClientId,
        onPlaylistNameChange = viewModel::updatePlaylistName,
        onSaveConfiguration = viewModel::saveConfiguration,
        onConnect = {
            viewModel.beginConnection { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        },
        onSynchronize = viewModel::synchronize,
        onEnablePlayback = {
            viewModel.beginConnection { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        },
        onTabSelected = viewModel::selectTab,
        onPreview = viewModel::togglePreview,
        onOpenSpotify = { track ->
            val spotifyAppIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("spotify:track:${track.spotifyTrackId}"),
            ).setPackage(SPOTIFY_PACKAGE_NAME)
            try {
                context.startActivity(spotifyAppIntent)
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.spotifyUrl)))
            }
        },
        onPrepare = onPrepare,
        onIgnore = viewModel::ignore,
        onRestore = viewModel::restore,
    )
}

private const val SPOTIFY_PACKAGE_NAME = "com.spotify.music"

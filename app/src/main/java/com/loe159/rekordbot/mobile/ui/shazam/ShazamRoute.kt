package com.loe159.rekordbot.mobile.ui.shazam

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

@Composable
fun ShazamRoute(
    configurationRepository: SpotifyConfigurationRepository,
    sessionRepository: SpotifySessionRepository,
    inboxRepository: ShazamInboxRepository,
    synchronizer: ShazamPlaylistSynchronizer,
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
        onTabSelected = viewModel::selectTab,
        onPrepare = onPrepare,
        onIgnore = viewModel::ignore,
        onRestore = viewModel::restore,
    )
}

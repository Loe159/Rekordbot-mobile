package com.loe159.rekordbot.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesAirtableConfigurationRepository
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesSoundchartsConfigurationRepository
import com.loe159.rekordbot.mobile.data.local.AndroidKeystoreSpotifySessionRepository
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesSpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.data.local.queue.RekordbotDatabase
import com.loe159.rekordbot.mobile.data.local.queue.RoomPendingTrackRepository
import com.loe159.rekordbot.mobile.data.local.shazam.RoomShazamInboxRepository
import com.loe159.rekordbot.mobile.data.work.WorkManagerQueueScheduler
import com.loe159.rekordbot.mobile.data.work.WorkManagerShazamSyncScheduler
import com.loe159.rekordbot.mobile.data.remote.airtable.DirectAirtableGateway
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyWebMetadataGateway
import com.loe159.rekordbot.mobile.data.remote.spotify.DirectSpotifyPlaylistGateway
import com.loe159.rekordbot.mobile.data.remote.spotify.DirectSpotifyPlaybackGateway
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyOAuthClient
import com.loe159.rekordbot.mobile.data.remote.soundcharts.DirectSoundchartsGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxCounts
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistSynchronizer
import com.loe159.rekordbot.mobile.domain.shazam.ShazamSpotifyPlaybackController
import com.loe159.rekordbot.mobile.domain.shazam.SpotifyTokenRefresher
import com.loe159.rekordbot.mobile.ui.home.HomeRoute
import com.loe159.rekordbot.mobile.ui.queue.QueueRoute
import com.loe159.rekordbot.mobile.ui.share.SharePreviewRoute
import com.loe159.rekordbot.mobile.ui.share.SharePreviewCompletion
import com.loe159.rekordbot.mobile.ui.shazam.ShazamRoute
import com.loe159.rekordbot.mobile.ui.settings.SettingsRoute
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme
import kotlinx.coroutines.launch

@Composable
fun RekordbotApp(
    incomingShare: SpotifyShareParseResult? = null,
    onShareClosed: () -> Unit = {},
    spotifyAuthorizationCallback: String? = null,
    onSpotifyAuthorizationCallbackConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configurationRepository = remember {
        SharedPreferencesAirtableConfigurationRepository(context.applicationContext)
    }
    val airtableGateway = remember { DirectAirtableGateway() }
    val soundchartsConfigurationRepository = remember {
        SharedPreferencesSoundchartsConfigurationRepository(context.applicationContext)
    }
    val soundchartsGateway = remember { DirectSoundchartsGateway() }
    val spotifyMetadataGateway = remember { SpotifyWebMetadataGateway() }
    val pendingTrackRepository = remember {
        RoomPendingTrackRepository(
            RekordbotDatabase.getInstance(context.applicationContext).queuedTrackDao(),
        )
    }
    val spotifyConfigurationRepository = remember {
        SharedPreferencesSpotifyConfigurationRepository(context.applicationContext)
    }
    val spotifySessionRepository = remember {
        AndroidKeystoreSpotifySessionRepository(context.applicationContext)
    }
    val spotifyPlaylistGateway = remember { DirectSpotifyPlaylistGateway() }
    val spotifyPlaybackGateway = remember { DirectSpotifyPlaybackGateway() }
    val shazamInboxRepository = remember {
        RoomShazamInboxRepository(
            RekordbotDatabase.getInstance(context.applicationContext).shazamInboxDao(),
        )
    }
    val shazamSynchronizer = remember {
        ShazamPlaylistSynchronizer(
            configurationRepository = spotifyConfigurationRepository,
            sessionRepository = spotifySessionRepository,
            playlistGateway = spotifyPlaylistGateway,
            inboxRepository = shazamInboxRepository,
            tokenRefresherFactory = { configuration ->
                val oauthClient = SpotifyOAuthClient(configuration)
                SpotifyTokenRefresher { currentTokens ->
                    oauthClient.refreshTokens(currentTokens)
                }
            },
        )
    }
    val shazamSyncScheduler = remember {
        WorkManagerShazamSyncScheduler(context.applicationContext)
    }
    val shazamPlaybackController = remember {
        ShazamSpotifyPlaybackController(
            configurationRepository = spotifyConfigurationRepository,
            sessionRepository = spotifySessionRepository,
            playbackGateway = spotifyPlaybackGateway,
            tokenRefresherFactory = { configuration ->
                val oauthClient = SpotifyOAuthClient(configuration)
                SpotifyTokenRefresher { currentTokens ->
                    oauthClient.refreshTokens(currentTokens)
                }
            },
        )
    }
    val queueWorkScheduler = remember { WorkManagerQueueScheduler(context.applicationContext) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showShazam by rememberSaveable { mutableStateOf(false) }
    var selectedShazamTrack by remember { mutableStateOf<ShazamInboxTrack?>(null) }
    var configurationVersion by rememberSaveable { mutableIntStateOf(0) }
    val isAirtableConfigured by produceState(
        initialValue = false,
        configurationVersion,
    ) {
        value = configurationRepository.isConnectionValidated()
    }
    val pendingTrackCount by pendingTrackRepository.observeOpenCount().collectAsState(initial = 0)
    val shazamCounts by shazamInboxRepository.observeCounts().collectAsState(
        initial = ShazamInboxCounts(0, 0, 0, 0),
    )

    LaunchedEffect(Unit) {
        queueWorkScheduler.schedule()
        if (spotifySessionRepository.loadTokens() != null) {
            shazamSyncScheduler.schedulePeriodic()
            shazamSyncScheduler.scheduleNow()
        }
    }

    LaunchedEffect(spotifyAuthorizationCallback) {
        if (spotifyAuthorizationCallback != null) showShazam = true
    }

    BackHandler(
        enabled = incomingShare != null || selectedShazamTrack != null ||
            showSettings || showQueue || showShazam,
    ) {
        when {
            incomingShare != null -> onShareClosed()
            selectedShazamTrack != null -> selectedShazamTrack = null
            showSettings -> showSettings = false
            showQueue -> showQueue = false
            showShazam -> showShazam = false
        }
    }

    RekordbotTheme {
        if (incomingShare != null) {
            SharePreviewRoute(
                parseResult = incomingShare,
                metadataGateway = spotifyMetadataGateway,
                configurationRepository = configurationRepository,
                airtableGateway = airtableGateway,
                pendingTrackRepository = pendingTrackRepository,
                queueWorkScheduler = queueWorkScheduler,
                soundchartsConfigurationRepository = soundchartsConfigurationRepository,
                soundchartsGateway = soundchartsGateway,
                onBack = onShareClosed,
            )
        } else if (selectedShazamTrack != null) {
            val track = requireNotNull(selectedShazamTrack)
            val parseResult = remember(track.spotifyTrackId) {
                SpotifyShareParseResult(
                    draft = TrackDraft(
                        spotifyTrackId = track.spotifyTrackId,
                        title = track.title,
                        artist = track.artist,
                        spotifyUrl = track.spotifyUrl,
                        source = "Shazam",
                        isrc = track.isrc,
                    ),
                )
            }
            SharePreviewRoute(
                parseResult = parseResult,
                metadataGateway = spotifyMetadataGateway,
                configurationRepository = configurationRepository,
                airtableGateway = airtableGateway,
                pendingTrackRepository = pendingTrackRepository,
                queueWorkScheduler = queueWorkScheduler,
                soundchartsConfigurationRepository = soundchartsConfigurationRepository,
                soundchartsGateway = soundchartsGateway,
                onBack = { selectedShazamTrack = null },
                onCompleted = { completion ->
                    coroutineScope.launch {
                        when (completion) {
                            SharePreviewCompletion.SAVED ->
                                shazamInboxRepository.markSaved(track.spotifyTrackId)
                            SharePreviewCompletion.ALREADY_PRESENT ->
                                shazamInboxRepository.markAlreadyPresent(track.spotifyTrackId)
                        }
                    }
                },
            )
        } else if (showSettings) {
            SettingsRoute(
                configurationRepository = configurationRepository,
                airtableGateway = airtableGateway,
                soundchartsConfigurationRepository = soundchartsConfigurationRepository,
                soundchartsGateway = soundchartsGateway,
                onBack = { showSettings = false },
                onConfigurationSaved = { configurationVersion++ },
            )
        } else if (showQueue) {
            QueueRoute(
                repository = pendingTrackRepository,
                workScheduler = queueWorkScheduler,
                onBack = { showQueue = false },
            )
        } else if (showShazam) {
            ShazamRoute(
                configurationRepository = spotifyConfigurationRepository,
                sessionRepository = spotifySessionRepository,
                inboxRepository = shazamInboxRepository,
                synchronizer = shazamSynchronizer,
                playbackController = shazamPlaybackController,
                authorizationCallback = spotifyAuthorizationCallback,
                onAuthorizationCallbackConsumed = onSpotifyAuthorizationCallbackConsumed,
                onConnectionAvailable = shazamSyncScheduler::schedulePeriodic,
                onPrepare = { selectedShazamTrack = it },
                onBack = { showShazam = false },
            )
        } else {
            HomeRoute(
                isAirtableConfigured = isAirtableConfigured,
                pendingTracks = pendingTrackCount,
                pendingShazams = shazamCounts.pending,
                onConfigureAirtable = { showSettings = true },
                onOpenQueue = { showQueue = true },
                onOpenShazam = { showShazam = true },
            )
        }
    }
}

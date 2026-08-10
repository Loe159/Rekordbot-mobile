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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesAirtableConfigurationRepository
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesSoundchartsConfigurationRepository
import com.loe159.rekordbot.mobile.data.local.queue.RekordbotDatabase
import com.loe159.rekordbot.mobile.data.local.queue.RoomPendingTrackRepository
import com.loe159.rekordbot.mobile.data.work.WorkManagerQueueScheduler
import com.loe159.rekordbot.mobile.data.remote.airtable.DirectAirtableGateway
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyWebMetadataGateway
import com.loe159.rekordbot.mobile.data.remote.soundcharts.DirectSoundchartsGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult
import com.loe159.rekordbot.mobile.ui.home.HomeRoute
import com.loe159.rekordbot.mobile.ui.queue.QueueRoute
import com.loe159.rekordbot.mobile.ui.share.SharePreviewRoute
import com.loe159.rekordbot.mobile.ui.settings.SettingsRoute
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme

@Composable
fun RekordbotApp(
    incomingShare: SpotifyShareParseResult? = null,
    onShareClosed: () -> Unit = {},
) {
    val context = LocalContext.current
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
    val queueWorkScheduler = remember { WorkManagerQueueScheduler(context.applicationContext) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var configurationVersion by rememberSaveable { mutableIntStateOf(0) }
    val isAirtableConfigured by produceState(
        initialValue = false,
        configurationVersion,
    ) {
        value = configurationRepository.isConnectionValidated()
    }
    val pendingTrackCount by pendingTrackRepository.observeOpenCount().collectAsState(initial = 0)

    LaunchedEffect(Unit) {
        queueWorkScheduler.schedule()
    }

    BackHandler(enabled = incomingShare != null || showSettings || showQueue) {
        when {
            incomingShare != null -> onShareClosed()
            showSettings -> showSettings = false
            showQueue -> showQueue = false
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
        } else {
            HomeRoute(
                isAirtableConfigured = isAirtableConfigured,
                pendingTracks = pendingTrackCount,
                onConfigureAirtable = { showSettings = true },
                onOpenQueue = { showQueue = true },
            )
        }
    }
}

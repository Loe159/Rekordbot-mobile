package com.loe159.rekordbot.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesAirtableConfigurationRepository
import com.loe159.rekordbot.mobile.data.remote.airtable.DirectAirtableGateway
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyWebMetadataGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult
import com.loe159.rekordbot.mobile.ui.home.HomeRoute
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
    val spotifyMetadataGateway = remember { SpotifyWebMetadataGateway() }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var configurationVersion by rememberSaveable { mutableIntStateOf(0) }
    val isAirtableConfigured by produceState(
        initialValue = false,
        configurationVersion,
    ) {
        value = configurationRepository.isConnectionValidated()
    }

    RekordbotTheme {
        if (incomingShare != null) {
            SharePreviewRoute(
                parseResult = incomingShare,
                metadataGateway = spotifyMetadataGateway,
                onBack = onShareClosed,
            )
        } else if (showSettings) {
            SettingsRoute(
                configurationRepository = configurationRepository,
                airtableGateway = airtableGateway,
                onBack = { showSettings = false },
                onConfigurationSaved = { configurationVersion++ },
            )
        } else {
            HomeRoute(
                isAirtableConfigured = isAirtableConfigured,
                onConfigureAirtable = { showSettings = true },
            )
        }
    }
}

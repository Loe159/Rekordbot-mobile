package com.loe159.rekordbot.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.loe159.rekordbot.mobile.domain.airtable.AirtableAuthorizationCallback
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParser
import com.loe159.rekordbot.mobile.ui.RekordbotApp

class MainActivity : ComponentActivity() {
    private var incomingShare by mutableStateOf<SpotifyShareParseResult?>(null)
    private var spotifyAuthorizationCallback by mutableStateOf<String?>(null)
    private var airtableAuthorizationCallback by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        incomingShare = intent.parseSpotifyShare()
        spotifyAuthorizationCallback = intent.parseSpotifyAuthorizationCallback()
        airtableAuthorizationCallback = intent.parseAirtableAuthorizationCallback()
        enableEdgeToEdge()
        setContent {
            RekordbotApp(
                incomingShare = incomingShare,
                onShareClosed = ::clearIncomingShare,
                spotifyAuthorizationCallback = spotifyAuthorizationCallback,
                onSpotifyAuthorizationCallbackConsumed = ::clearSpotifyAuthorizationCallback,
                airtableAuthorizationCallback = airtableAuthorizationCallback,
                onAirtableAuthorizationCallbackConsumed = ::clearAirtableAuthorizationCallback,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShare = intent.parseSpotifyShare()
        spotifyAuthorizationCallback = intent.parseSpotifyAuthorizationCallback()
        airtableAuthorizationCallback = intent.parseAirtableAuthorizationCallback()
    }

    private fun Intent.parseSpotifyShare(): SpotifyShareParseResult? {
        if (action != Intent.ACTION_SEND || type != "text/plain") return null

        return SpotifyShareParser.parse(
            sharedText = getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            sharedSubject = getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString(),
        )
    }

    private fun Intent.parseSpotifyAuthorizationCallback(): String? {
        if (action != Intent.ACTION_VIEW) return null
        val callback = data ?: return null
        if (callback.scheme != "rekordbot-mobile-login" || callback.host != "callback") return null
        return callback.toString()
    }

    private fun Intent.parseAirtableAuthorizationCallback(): String? {
        if (action != Intent.ACTION_VIEW) return null
        val callback = data ?: return null
        if (
            !AirtableAuthorizationCallback.isExpected(
                callbackUri = callback.toString(),
                expectedRedirectUri = BuildConfig.AIRTABLE_REDIRECT_URI,
            )
        ) {
            return null
        }
        return callback.toString()
    }

    private fun clearIncomingShare() {
        incomingShare = null
        setIntent(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            },
        )
    }

    private fun clearSpotifyAuthorizationCallback() {
        spotifyAuthorizationCallback = null
        setIntent(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            },
        )
    }

    private fun clearAirtableAuthorizationCallback() {
        airtableAuthorizationCallback = null
        setIntent(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            },
        )
    }
}

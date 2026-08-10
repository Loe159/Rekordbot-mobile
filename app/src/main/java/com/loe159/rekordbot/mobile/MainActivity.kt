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
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParser
import com.loe159.rekordbot.mobile.ui.RekordbotApp

class MainActivity : ComponentActivity() {
    private var incomingShare by mutableStateOf<SpotifyShareParseResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        incomingShare = intent.parseSpotifyShare()
        enableEdgeToEdge()
        setContent {
            RekordbotApp(
                incomingShare = incomingShare,
                onShareClosed = ::clearIncomingShare,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShare = intent.parseSpotifyShare()
    }

    private fun Intent.parseSpotifyShare(): SpotifyShareParseResult? {
        if (action != Intent.ACTION_SEND || type != "text/plain") return null

        return SpotifyShareParser.parse(
            sharedText = getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            sharedSubject = getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString(),
        )
    }

    private fun clearIncomingShare() {
        incomingShare = null
        setIntent(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            },
        )
    }
}

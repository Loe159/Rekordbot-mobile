package com.loe159.rekordbot.mobile.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.loe159.rekordbot.mobile.BuildConfig
import com.loe159.rekordbot.mobile.data.local.AndroidKeystoreSpotifySessionRepository
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesSpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.data.local.queue.RekordbotDatabase
import com.loe159.rekordbot.mobile.data.local.shazam.RoomShazamInboxRepository
import com.loe159.rekordbot.mobile.data.remote.spotify.DirectSpotifyPlaylistGateway
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyApiException
import com.loe159.rekordbot.mobile.data.remote.spotify.SpotifyOAuthClient
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistSyncSummary
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistSynchronizer
import com.loe159.rekordbot.mobile.domain.shazam.ShazamSyncPrerequisiteException
import com.loe159.rekordbot.mobile.domain.shazam.SpotifyTokenRefresher
import java.io.IOException

class ShazamSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val database = RekordbotDatabase.getInstance(applicationContext)
        val sessionRepository = AndroidKeystoreSpotifySessionRepository(applicationContext)
        val synchronizer = ShazamPlaylistSynchronizer(
            configurationRepository = SharedPreferencesSpotifyConfigurationRepository(
                applicationContext,
                bundledClientId = BuildConfig.SPOTIFY_CLIENT_ID,
            ),
            sessionRepository = sessionRepository,
            playlistGateway = DirectSpotifyPlaylistGateway(),
            inboxRepository = RoomShazamInboxRepository(database.shazamInboxDao()),
            tokenRefresherFactory = { configuration ->
                val oauthClient = SpotifyOAuthClient(configuration)
                SpotifyTokenRefresher { currentTokens ->
                    oauthClient.refreshTokens(currentTokens)
                }
            },
        )

        val syncResult = synchronizer.synchronize()
        syncResult.getOrNull()?.let { summary ->
            return Result.success(summary.toWorkData())
        }
        val error = syncResult.exceptionOrNull()
        if (
            error is SpotifyApiException &&
            (error.statusCode == 400 || error.statusCode == 401)
        ) {
            sessionRepository.clearTokens()
        }
        return error.toWorkerResult()
    }

    private fun Throwable?.toWorkerResult(): Result = when (this) {
        is ShazamSyncPrerequisiteException,
        is IllegalArgumentException -> Result.failure()

        is SpotifyApiException -> if (
            statusCode == null || statusCode == 429 || (statusCode ?: 0) in 500..599
        ) {
            Result.retry()
        } else {
            Result.failure()
        }

        is IOException -> Result.retry()
        else -> Result.retry()
    }

    private fun ShazamPlaylistSyncSummary.toWorkData(): Data = Data.Builder()
        .putString(KEY_PLAYLIST_ID, playlistId)
        .putString(KEY_PLAYLIST_NAME, playlistName)
        .putInt(KEY_RECEIVED_COUNT, receivedTrackCount)
        .putInt(KEY_DISTINCT_COUNT, distinctTrackCount)
        .putInt(KEY_ADDED_COUNT, addedCount)
        .putInt(KEY_REFRESHED_COUNT, refreshedCount)
        .putBoolean(KEY_TOKENS_REFRESHED, tokensRefreshed)
        .build()

    companion object {
        const val KEY_PLAYLIST_ID = "playlist_id"
        const val KEY_PLAYLIST_NAME = "playlist_name"
        const val KEY_RECEIVED_COUNT = "received_count"
        const val KEY_DISTINCT_COUNT = "distinct_count"
        const val KEY_ADDED_COUNT = "added_count"
        const val KEY_REFRESHED_COUNT = "refreshed_count"
        const val KEY_TOKENS_REFRESHED = "tokens_refreshed"
    }
}

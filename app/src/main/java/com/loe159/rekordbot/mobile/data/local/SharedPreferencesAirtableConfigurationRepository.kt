package com.loe159.rekordbot.mobile.data.local

import android.content.Context
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableFieldMappings
import com.loe159.rekordbot.mobile.domain.model.DuplicateStrategy
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesAirtableConfigurationRepository(
    context: Context,
    private val tokenVault: AndroidKeystoreTokenVault = AndroidKeystoreTokenVault(context),
) : AirtableConfigurationRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun load(): AirtableConfiguration = withContext(Dispatchers.IO) {
        val defaults = AirtableFieldMappings()
        AirtableConfiguration(
            personalAccessToken = tokenVault.read(),
            baseId = preferences.getString(KEY_BASE_ID, "").orEmpty(),
            table = preferences.getString(KEY_TABLE, "").orEmpty(),
            fields = AirtableFieldMappings(
                title = preferences.getString(KEY_FIELD_TITLE, defaults.title).orEmpty(),
                artist = preferences.getString(KEY_FIELD_ARTIST, defaults.artist).orEmpty(),
                spotifyUrl = preferences.getString(KEY_FIELD_SPOTIFY_URL, defaults.spotifyUrl).orEmpty(),
                spotifyTrackId = preferences.getString(
                    KEY_FIELD_SPOTIFY_TRACK_ID,
                    defaults.spotifyTrackId,
                ).orEmpty(),
                status = preferences.getString(KEY_FIELD_STATUS, defaults.status).orEmpty(),
                rawGenre = preferences.getString(KEY_FIELD_RAW_GENRE, defaults.rawGenre).orEmpty(),
                energy = preferences.getString(KEY_FIELD_ENERGY, defaults.energy).orEmpty(),
                mood = preferences.getString(KEY_FIELD_MOOD, defaults.mood).orEmpty(),
                situation = preferences.getString(KEY_FIELD_SITUATION, defaults.situation).orEmpty(),
                inspirationalDjs = preferences.getString(
                    KEY_FIELD_INSPIRATIONAL_DJS,
                    defaults.inspirationalDjs,
                ).orEmpty(),
                comment = preferences.getString(KEY_FIELD_COMMENT, defaults.comment).orEmpty(),
                source = preferences.getString(KEY_FIELD_SOURCE, defaults.source).orEmpty(),
            ),
            defaultStatus = preferences.getString(KEY_DEFAULT_STATUS, "À qualifier").orEmpty(),
            defaultSource = preferences.getString(KEY_DEFAULT_SOURCE, "Spotify").orEmpty(),
            duplicateStrategy = preferences.getString(
                KEY_DUPLICATE_STRATEGY,
                DuplicateStrategy.BLOCK.name,
            )
                ?.let { stored ->
                    DuplicateStrategy.entries.firstOrNull { it.name == stored }
                }
                ?: DuplicateStrategy.BLOCK,
        )
    }

    override suspend fun save(configuration: AirtableConfiguration) = withContext(Dispatchers.IO) {
        tokenVault.write(configuration.personalAccessToken.trim())
        preferences.edit()
            .putString(KEY_BASE_ID, configuration.baseId.trim())
            .putString(KEY_TABLE, configuration.table.trim())
            .putString(KEY_FIELD_TITLE, configuration.fields.title.trim())
            .putString(KEY_FIELD_ARTIST, configuration.fields.artist.trim())
            .putString(KEY_FIELD_SPOTIFY_URL, configuration.fields.spotifyUrl.trim())
            .putString(KEY_FIELD_SPOTIFY_TRACK_ID, configuration.fields.spotifyTrackId.trim())
            .putString(KEY_FIELD_STATUS, configuration.fields.status.trim())
            .putString(KEY_FIELD_RAW_GENRE, configuration.fields.rawGenre.trim())
            .putString(KEY_FIELD_ENERGY, configuration.fields.energy.trim())
            .putString(KEY_FIELD_MOOD, configuration.fields.mood.trim())
            .putString(KEY_FIELD_SITUATION, configuration.fields.situation.trim())
            .putString(KEY_FIELD_INSPIRATIONAL_DJS, configuration.fields.inspirationalDjs.trim())
            .putString(KEY_FIELD_COMMENT, configuration.fields.comment.trim())
            .putString(KEY_FIELD_SOURCE, configuration.fields.source.trim())
            .putString(KEY_DEFAULT_STATUS, configuration.defaultStatus.trim())
            .putString(KEY_DEFAULT_SOURCE, configuration.defaultSource.trim())
            .putString(KEY_DUPLICATE_STRATEGY, configuration.duplicateStrategy.name)
            .putBoolean(KEY_CONNECTION_VALIDATED, false)
            .commit()
        Unit
    }

    override suspend fun isConnectionValidated(): Boolean = withContext(Dispatchers.IO) {
        preferences.getBoolean(KEY_CONNECTION_VALIDATED, false) && tokenVault.read().isNotBlank()
    }

    override suspend fun markConnectionValidated() = withContext(Dispatchers.IO) {
        preferences.edit().putBoolean(KEY_CONNECTION_VALIDATED, true).commit()
        Unit
    }

    private companion object {
        const val PREFERENCES_NAME = "rekordbot_airtable_configuration"
        const val KEY_BASE_ID = "base_id"
        const val KEY_TABLE = "table"
        const val KEY_FIELD_TITLE = "field_title"
        const val KEY_FIELD_ARTIST = "field_artist"
        const val KEY_FIELD_SPOTIFY_URL = "field_spotify_url"
        const val KEY_FIELD_SPOTIFY_TRACK_ID = "field_spotify_track_id"
        const val KEY_FIELD_STATUS = "field_status"
        const val KEY_FIELD_RAW_GENRE = "field_raw_genre"
        const val KEY_FIELD_ENERGY = "field_energy"
        const val KEY_FIELD_MOOD = "field_mood"
        const val KEY_FIELD_SITUATION = "field_situation"
        const val KEY_FIELD_INSPIRATIONAL_DJS = "field_inspirational_djs"
        const val KEY_FIELD_COMMENT = "field_comment"
        const val KEY_FIELD_SOURCE = "field_source"
        const val KEY_DEFAULT_STATUS = "default_status"
        const val KEY_DEFAULT_SOURCE = "default_source"
        const val KEY_DUPLICATE_STRATEGY = "duplicate_strategy"
        const val KEY_CONNECTION_VALIDATED = "connection_validated"
    }
}

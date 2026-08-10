package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.TrackDraft

object AirtableRecordMapper {
    fun fields(
        configuration: AirtableConfiguration,
        track: TrackDraft,
    ): Map<String, String> = buildMap {
        put(configuration.fields.title, track.title.trim())
        put(configuration.fields.artist, track.artist.trim())
        put(configuration.fields.spotifyUrl, track.spotifyUrl.trim())
        put(configuration.fields.spotifyTrackId, track.spotifyTrackId.trim())
        put(configuration.fields.status, configuration.defaultStatus.trim())
        putIfConfigured(configuration.fields.source, configuration.defaultSource)
        putIfConfigured(configuration.fields.rawGenre, track.rawGenre)
        putIfConfigured(configuration.fields.comment, track.comment)
    }

    private fun MutableMap<String, String>.putIfConfigured(field: String, value: String?) {
        if (field.isNotBlank() && !value.isNullOrBlank()) put(field, value.trim())
    }
}

object AirtableFormula {
    fun textEquals(field: String, value: String): String =
        "{${field.replace("}", "\\}")}}='${value.escapeFormulaText()}'"

    private fun String.escapeFormulaText(): String = replace("\\", "\\\\").replace("'", "\\'")
}

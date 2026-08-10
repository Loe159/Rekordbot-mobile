package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.TrackDraft

object AirtableRecordMapper {
    fun fields(
        configuration: AirtableConfiguration,
        track: TrackDraft,
    ): Map<String, Any> = buildMap {
        put(configuration.fields.title, track.title.trim())
        put(configuration.fields.artist, track.artist.trim())
        put(configuration.fields.spotifyUrl, track.spotifyUrl.trim())
        put(configuration.fields.spotifyTrackId, track.spotifyTrackId.trim())
        put(configuration.fields.status, configuration.defaultStatus.trim())
        putIfConfigured(configuration.fields.isrc, track.isrc)
        putIfConfigured(configuration.fields.rekordbotState, configuration.defaultRekordbotState)
        putIfConfigured(configuration.fields.source, configuration.defaultSource)
        putIfConfigured(configuration.fields.rawGenre, track.rawGenre)
        putNumberIfConfigured(configuration.fields.energy, track.energy)
        putListIfConfigured(configuration.fields.mood, track.moods)
        putListIfConfigured(configuration.fields.situation, track.situations)
        putListIfConfigured(configuration.fields.inspirationalDjs, track.inspirationalDjs)
        putIfConfigured(configuration.fields.comment, track.comment)
    }

    private fun MutableMap<String, Any>.putIfConfigured(field: String, value: String?) {
        if (field.isNotBlank() && !value.isNullOrBlank()) put(field, value.trim())
    }

    private fun MutableMap<String, Any>.putNumberIfConfigured(field: String, value: Int?) {
        if (field.isNotBlank() && value != null) put(field, value)
    }

    private fun MutableMap<String, Any>.putListIfConfigured(
        field: String,
        values: List<String>,
    ) {
        val normalized = values.map(String::trim).filter(String::isNotBlank).distinct()
        if (field.isNotBlank() && normalized.isNotEmpty()) put(field, normalized)
    }
}

object AirtableFormula {
    fun textEquals(field: String, value: String): String =
        "{${field.replace("}", "\\}")}}='${value.escapeFormulaText()}'"

    private fun String.escapeFormulaText(): String = replace("\\", "\\\\").replace("'", "\\'")
}

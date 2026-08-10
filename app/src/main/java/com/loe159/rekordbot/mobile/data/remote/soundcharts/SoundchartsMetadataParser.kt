package com.loe159.rekordbot.mobile.data.remote.soundcharts

import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsMetadata
import org.json.JSONArray
import org.json.JSONObject

object SoundchartsMetadataParser {
    fun parse(responseBody: String): SoundchartsMetadata {
        val song = JSONObject(responseBody).optJSONObject("object")
            ?: throw SoundchartsApiException("Réponse Soundcharts incomplète.")
        val genres = buildList {
            val values = song.optJSONArray("genres") ?: JSONArray()
            for (index in 0 until values.length()) {
                when (val genre = values.opt(index)) {
                    is JSONObject -> {
                        addGenreValues(genre.opt("root"))
                        addGenreValues(genre.opt("sub"))
                    }
                    else -> addGenreValues(genre)
                }
            }
        }.distinctBy { it.lowercase() }
        val isrc = when (val value = song.opt("isrc")) {
            is JSONObject -> value.optString("value").trim()
            is String -> value.trim()
            else -> ""
        }.takeIf(String::isNotBlank)
        return SoundchartsMetadata(genres = genres, isrc = isrc)
    }

    private fun MutableList<String>.addGenreValues(value: Any?) {
        when (value) {
            is String -> value.trim().takeIf(String::isNotBlank)?.let(::add)
            is JSONArray -> for (index in 0 until value.length()) addGenreValues(value.opt(index))
            is JSONObject -> {
                val label = sequenceOf("name", "value", "label")
                    .map { key -> value.optString(key) }
                    .firstOrNull(String::isNotBlank)
                if (label != null) add(label.trim())
            }
        }
    }
}

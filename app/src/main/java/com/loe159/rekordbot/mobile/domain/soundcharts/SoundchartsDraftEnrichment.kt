package com.loe159.rekordbot.mobile.domain.soundcharts

import com.loe159.rekordbot.mobile.domain.model.TrackDraft

data class SoundchartsDraftEnrichment(
    val draft: TrackDraft,
    val genreSuggestion: String? = null,
)

/** Merges against the latest UI draft so edits made while the request runs always win. */
object SoundchartsDraftEnricher {
    fun merge(
        current: TrackDraft,
        metadata: SoundchartsMetadata,
    ): SoundchartsDraftEnrichment {
        val fetchedGenre = metadata.rawGenre?.trim()?.takeIf(String::isNotBlank)
        val currentGenre = current.rawGenre?.takeIf(String::isNotBlank)
        val suggestion = fetchedGenre?.takeIf {
            currentGenre != null && !it.equals(currentGenre, ignoreCase = true)
        }
        return SoundchartsDraftEnrichment(
            draft = current.copy(
                rawGenre = currentGenre ?: fetchedGenre,
                isrc = current.isrc?.takeIf(String::isNotBlank)
                    ?: metadata.isrc?.trim()?.takeIf(String::isNotBlank),
            ),
            genreSuggestion = suggestion,
        )
    }
}

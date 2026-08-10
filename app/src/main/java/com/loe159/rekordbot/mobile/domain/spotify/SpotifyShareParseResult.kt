package com.loe159.rekordbot.mobile.domain.spotify

import com.loe159.rekordbot.mobile.domain.model.TrackDraft

data class SpotifyShareParseResult(
    val draft: TrackDraft,
    val issue: SpotifyShareIssue? = null,
) {
    fun withMetadata(metadata: SpotifyTrackMetadata): SpotifyShareParseResult {
        val enrichedDraft = draft.copy(
            title = draft.title.ifBlank { metadata.title },
            artist = draft.artist.ifBlank { metadata.artist },
        )
        return copy(
            draft = enrichedDraft,
            issue = enrichedDraft.metadataIssue(),
        )
    }
}

enum class SpotifyShareIssue {
    EMPTY_SHARE,
    NOT_A_SPOTIFY_TRACK,
    MISSING_TITLE_AND_ARTIST,
    MISSING_TITLE,
    MISSING_ARTIST,
}

internal fun TrackDraft.metadataIssue(): SpotifyShareIssue? = when {
    title.isBlank() && artist.isBlank() -> SpotifyShareIssue.MISSING_TITLE_AND_ARTIST
    title.isBlank() -> SpotifyShareIssue.MISSING_TITLE
    artist.isBlank() -> SpotifyShareIssue.MISSING_ARTIST
    else -> null
}

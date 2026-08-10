package com.loe159.rekordbot.mobile.domain.spotify

import com.loe159.rekordbot.mobile.domain.model.TrackDraft

data class SpotifyShareParseResult(
    val draft: TrackDraft,
    val issue: SpotifyShareIssue? = null,
)

enum class SpotifyShareIssue {
    EMPTY_SHARE,
    NOT_A_SPOTIFY_TRACK,
    MISSING_TITLE_AND_ARTIST,
    MISSING_TITLE,
    MISSING_ARTIST,
}

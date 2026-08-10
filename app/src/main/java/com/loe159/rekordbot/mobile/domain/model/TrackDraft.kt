package com.loe159.rekordbot.mobile.domain.model

data class TrackDraft(
    val spotifyTrackId: String,
    val title: String,
    val artist: String,
    val spotifyUrl: String,
    val rawGenre: String? = null,
    val comment: String? = null,
) {
    val isReadyForAirtable: Boolean
        get() = spotifyTrackId.isNotBlank() &&
            title.isNotBlank() &&
            artist.isNotBlank() &&
            spotifyUrl.isNotBlank()
}


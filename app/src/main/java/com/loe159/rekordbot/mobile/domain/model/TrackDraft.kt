package com.loe159.rekordbot.mobile.domain.model

data class TrackDraft(
    val spotifyTrackId: String,
    val title: String,
    val artist: String,
    val spotifyUrl: String,
    val rawGenre: String? = null,
    val energy: Int? = null,
    val moods: List<String> = emptyList(),
    val situations: List<String> = emptyList(),
    val inspirationalDjs: List<String> = emptyList(),
    val comment: String? = null,
) {
    val isReadyForAirtable: Boolean
        get() = spotifyTrackId.isNotBlank() &&
            title.isNotBlank() &&
            artist.isNotBlank() &&
            spotifyUrl.isNotBlank()
}

/** Values shared by the capture and queue editors. Keep Airtable spelling exact here. */
object DjQualificationOptions {
    val energies: IntRange = 1..5

    val moods: List<String> = listOf(
        "Sexy",
        "Énergique",
        "Sombre",
        "Joyeux",
        "Ambiant",
        "Calme",
        "Mystérieux",
        "Triste",
    )

    val situations: List<String> = listOf(
        "Warm-up",
        "Montée",
        "Peak-time",
        "Transition",
        "Closing",
        "Sunset",
        "Apéro",
        "Club",
        "Festival",
        "After",
        "B2B",
    )

    val inspirationalDjs: List<String> = listOf(
        "HUGEL",
        "ANOTR",
        "Adam Ten",
        "James Hype",
        "Fred again.. — Rooftop London",
        "Fred again.. — USB",
        "Fred again.. — Fuji Rock",
    )
}

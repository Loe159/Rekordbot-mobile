package com.loe159.rekordbot.mobile.domain.model

data class AirtableConfiguration(
    val personalAccessToken: String = "",
    val baseId: String = "",
    val table: String = "",
    val fields: AirtableFieldMappings = AirtableFieldMappings(),
    val defaultStatus: String = "À qualifier",
    val defaultSource: String = "Spotify",
)

data class AirtableFieldMappings(
    val title: String = "Titre",
    val artist: String = "Artiste",
    val spotifyUrl: String = "Lien Spotify",
    val spotifyTrackId: String = "Spotify Track ID",
    val status: String = "Statut",
    val rawGenre: String = "Genre brut",
    val energy: String = "Énergie",
    val mood: String = "Mood",
    val situation: String = "Situation",
    val inspirationalDjs: String = "DJs inspirants",
    val comment: String = "Commentaire",
    val source: String = "Source",
) {
    fun configuredNames(): List<String> = listOf(
        title,
        artist,
        spotifyUrl,
        spotifyTrackId,
        status,
        rawGenre,
        energy,
        mood,
        situation,
        inspirationalDjs,
        comment,
        source,
    ).filter(String::isNotBlank)
}

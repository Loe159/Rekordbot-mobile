package com.loe159.rekordbot.mobile.domain.model

data class AirtableConfiguration(
    val personalAccessToken: String = "",
    val baseId: String = "",
    val table: String = "",
    val fields: AirtableFieldMappings = AirtableFieldMappings(),
    val defaultStatus: String = "À qualifier",
    val defaultSource: String = "Spotify",
    val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.BLOCK,
    val defaultRekordbotState: String = "À traiter",
)

enum class DuplicateStrategy {
    BLOCK,
    ALLOW,
}

data class AirtableFieldMappings(
    val title: String = "Titre",
    val artist: String = "Artiste",
    val spotifyUrl: String = "Lien Spotify",
    val spotifyTrackId: String = "Spotify Track ID",
    val isrc: String = "ISRC",
    val status: String = "Statut",
    val rekordbotState: String = "État RekordBot",
    val rekordbotError: String = "Erreur RekordBot",
    val lastSync: String = "Dernière synchro",
    val matchingMethod: String = "Méthode de matching",
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
        isrc,
        status,
        rekordbotState,
        rekordbotError,
        lastSync,
        matchingMethod,
        rawGenre,
        energy,
        mood,
        situation,
        inspirationalDjs,
        comment,
        source,
    ).filter(String::isNotBlank)
}

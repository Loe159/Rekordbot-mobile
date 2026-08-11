package com.loe159.rekordbot.mobile.domain.model

object AirtableConfigurationValidator {
    fun localErrors(configuration: AirtableConfiguration): List<String> = buildList {
        if (
            configuration.authenticationMode == AirtableAuthenticationMode.PERSONAL_ACCESS_TOKEN &&
            configuration.personalAccessToken.isBlank()
        ) {
            add("Le token Airtable est obligatoire en mode avancé.")
        }
        if (configuration.baseId.isBlank()) add("Le Base ID est obligatoire.")
        if (configuration.table.isBlank()) add("La table est obligatoire.")
        if (configuration.fields.title.isBlank()) add("Le champ Titre est obligatoire.")
        if (configuration.fields.artist.isBlank()) add("Le champ Artiste est obligatoire.")
        if (configuration.fields.spotifyUrl.isBlank()) add("Le champ Lien Spotify est obligatoire.")
        if (configuration.fields.spotifyTrackId.isBlank()) add("Le champ Spotify Track ID est obligatoire.")
        if (configuration.fields.status.isBlank()) add("Le champ Statut est obligatoire.")
        if (configuration.fields.rekordbotState.isBlank()) add("Le champ État RekordBot est obligatoire.")
        if (configuration.defaultStatus.isBlank()) add("Le statut par défaut est obligatoire.")
        if (configuration.defaultRekordbotState.isBlank()) add("État RekordBot initial obligatoire.")
    }

    fun schemaErrors(
        configuration: AirtableConfiguration,
        schema: AirtableTableSchema,
    ): List<String> {
        val actualNames = schema.fields.map { it.name.trim().lowercase() }.toSet()
        return configuration.fields.configuredNames()
            .distinctBy { it.trim().lowercase() }
            .filterNot { it.trim().lowercase() in actualNames }
            .map { "Champ « $it » introuvable dans la table ${schema.name}." }
    }
}

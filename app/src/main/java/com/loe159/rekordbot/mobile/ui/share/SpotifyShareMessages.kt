package com.loe159.rekordbot.mobile.ui.share

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareIssue
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult

internal fun SpotifyShareParseResult.userMessage(): String? = when (issue) {
    SpotifyShareIssue.EMPTY_SHARE -> "Le partage ne contient aucun texte. Colle un lien Spotify pour continuer."
    SpotifyShareIssue.NOT_A_SPOTIFY_TRACK -> "Aucun morceau Spotify reconnu. Vérifie le lien ou l’URI partagé."
    SpotifyShareIssue.MISSING_TITLE_AND_ARTIST ->
        "Spotify n’a transmis que le lien. Complète le titre et l’artiste avant l’étape d’envoi."
    SpotifyShareIssue.MISSING_TITLE -> "Le titre n’a pas été détecté automatiquement."
    SpotifyShareIssue.MISSING_ARTIST -> "L’artiste n’a pas été détecté automatiquement."
    null -> null
}

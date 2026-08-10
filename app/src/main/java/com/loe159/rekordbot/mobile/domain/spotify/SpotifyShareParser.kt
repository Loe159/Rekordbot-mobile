package com.loe159.rekordbot.mobile.domain.spotify

import com.loe159.rekordbot.mobile.domain.model.TrackDraft

object SpotifyShareParser {
    private const val TRACK_ID_LENGTH = 22
    private val webTrackRegex = Regex(
        "(?:https?://)?open\\.spotify\\.com/(?:intl-[a-zA-Z]{2}/)?track/" +
            "([A-Za-z0-9]{$TRACK_ID_LENGTH})(?=[/?#\\s]|$)",
        RegexOption.IGNORE_CASE,
    )
    private val uriTrackRegex = Regex(
        "spotify:track:([A-Za-z0-9]{$TRACK_ID_LENGTH})(?=[?\\s]|$)",
        RegexOption.IGNORE_CASE,
    )
    private val bySeparatorRegex = Regex("^(.+?)\\s+(?:by|par|de)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val visualSeparatorRegex = Regex("\\s+(?:[•·|]|—|–|-)\\s+")
    private val spotifyWrapperPrefix = Regex("^(?:écouter|listen to)\\s+", RegexOption.IGNORE_CASE)
    private val spotifyWrapperSuffix = Regex("\\s+(?:sur|on)\\s+Spotify$", RegexOption.IGNORE_CASE)

    fun parse(
        sharedText: String?,
        sharedSubject: String? = null,
    ): SpotifyShareParseResult {
        val text = sharedText.orEmpty().trim()
        val subject = sharedSubject.orEmpty().trim()
        if (text.isBlank() && subject.isBlank()) {
            return emptyResult(SpotifyShareIssue.EMPTY_SHARE)
        }

        val trackId = findTrackId(text) ?: findTrackId(subject)
        if (trackId == null) {
            return emptyResult(SpotifyShareIssue.NOT_A_SPOTIFY_TRACK)
        }

        val metadata = extractMetadata(subject, text)
        val draft = TrackDraft(
            spotifyTrackId = trackId,
            title = metadata.first,
            artist = metadata.second,
            spotifyUrl = "https://open.spotify.com/track/$trackId",
        )
        return SpotifyShareParseResult(draft = draft, issue = draft.metadataIssue())
    }

    private fun findTrackId(value: String): String? =
        webTrackRegex.find(value)?.groupValues?.get(1)
            ?: uriTrackRegex.find(value)?.groupValues?.get(1)

    private fun extractMetadata(subject: String, text: String): Pair<String, String> {
        val candidates = buildList {
            subject.cleanMetadataCandidate().takeIf { it.isNotBlank() && !it.equals("Spotify", true) }
                ?.let(::add)
            text
                .replace(webTrackRegex, "")
                .replace(uriTrackRegex, "")
                .cleanMetadataCandidate()
                .takeIf(String::isNotBlank)
                ?.let(::add)
        }

        candidates.forEach { candidate ->
            parseMetadataCandidate(candidate)?.let { return it }
        }
        return "" to ""
    }

    private fun String.cleanMetadataCandidate(): String =
        trim()
            .replace(spotifyWrapperPrefix, "")
            .replace(spotifyWrapperSuffix, "")
            .trim(' ', '\n', '\r', '-', '–', '—', '•', '·', '|')

    private fun parseMetadataCandidate(candidate: String): Pair<String, String>? {
        val lines = candidate.lines().map(String::trim).filter(String::isNotBlank)
        if (lines.size >= 2) return lines[0] to lines[1]

        val value = lines.singleOrNull() ?: return null
        bySeparatorRegex.matchEntire(value)?.let { match ->
            return match.groupValues[1].trim() to match.groupValues[2].trim()
        }

        val split = visualSeparatorRegex.split(value, limit = 2)
        return split.takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() }
    }

    private fun TrackDraft.metadataIssue(): SpotifyShareIssue? = when {
        title.isBlank() && artist.isBlank() -> SpotifyShareIssue.MISSING_TITLE_AND_ARTIST
        title.isBlank() -> SpotifyShareIssue.MISSING_TITLE
        artist.isBlank() -> SpotifyShareIssue.MISSING_ARTIST
        else -> null
    }

    private fun emptyResult(issue: SpotifyShareIssue) = SpotifyShareParseResult(
        draft = TrackDraft(
            spotifyTrackId = "",
            title = "",
            artist = "",
            spotifyUrl = "",
        ),
        issue = issue,
    )
}

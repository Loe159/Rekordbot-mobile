package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTrackMetadata

internal object SpotifyPageMetadataParser {
    private val titleTagRegex = Regex(
        "<title[^>]*>(.*?)</title>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val metaTagRegex = Regex("<meta\\s+[^>]*>", RegexOption.IGNORE_CASE)
    private val attributeRegex = Regex(
        "([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val pageTitleRegex = Regex(
        "^(.+?)\\s+-\\s+(?:song and lyrics by|titre et paroles par)\\s+(.+?)(?:\\s+\\|\\s+Spotify)?$",
        RegexOption.IGNORE_CASE,
    )
    private val descriptionArtistRegex = Regex(
        "(?:Song|Titre)\\s*[·•]\\s*(.+?)\\s*[·•]\\s*\\d{4}",
        RegexOption.IGNORE_CASE,
    )
    private val numericEntityRegex = Regex("&#(x?[0-9a-fA-F]+);")

    fun parse(html: String): SpotifyTrackMetadata? {
        val meta = metaTagRegex.findAll(html).mapNotNull(::parseMetaTag).toMap()
        val documentTitle = titleTagRegex.find(html)?.groupValues?.get(1)?.decodeHtml().orEmpty()
        val titleMatch = pageTitleRegex.matchEntire(documentTitle.trim())
        val title = meta["og:title"].orEmpty().decodeHtml().ifBlank {
            titleMatch?.groupValues?.get(1).orEmpty().trim()
        }
        val artist = descriptionArtistRegex
            .find(meta["og:description"].orEmpty().decodeHtml())
            ?.groupValues
            ?.get(1)
            ?.trim()
            .orEmpty()
            .ifBlank { titleMatch?.groupValues?.get(2).orEmpty().trim() }

        return SpotifyTrackMetadata(title = title, artist = artist)
            .takeIf { it.title.isNotBlank() && it.artist.isNotBlank() }
    }

    private fun parseMetaTag(match: MatchResult): Pair<String, String>? {
        val attributes = attributeRegex.findAll(match.value).associate {
            it.groupValues[1].lowercase() to it.groupValues[3]
        }
        val key = attributes["property"] ?: attributes["name"] ?: return null
        val content = attributes["content"] ?: return null
        return key.lowercase() to content
    }

    private fun String.decodeHtml(): String {
        val decodedNumericEntities = numericEntityRegex.replace(this) { match ->
            val rawValue = match.groupValues[1]
            val radix = if (rawValue.startsWith("x", ignoreCase = true)) 16 else 10
            rawValue.removePrefix("x").removePrefix("X").toIntOrNull(radix)
                ?.let(Character::toChars)
                ?.concatToString()
                ?: match.value
        }
        return decodedNumericEntities
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace("&apos;", "'", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
    }
}

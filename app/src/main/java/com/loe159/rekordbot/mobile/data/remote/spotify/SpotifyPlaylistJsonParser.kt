package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylist
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistTrack
import org.json.JSONArray
import org.json.JSONObject

internal data class SpotifyParsedPage<T>(
    val items: List<T>,
    val nextUrl: String?,
)

internal object SpotifyPlaylistJsonParser {
    fun parsePage(body: String): SpotifyParsedPage<SpotifyPlaylist> {
        val json = JSONObject(body)
        val items = json.optJSONArray("items") ?: JSONArray()
        return SpotifyParsedPage(
            items = buildList {
                for (index in 0 until items.length()) {
                    val playlist = items.optJSONObject(index) ?: continue
                    val id = playlist.optString("id").trim()
                    val name = playlist.optString("name").trim()
                    if (id.isBlank() || name.isBlank()) continue
                    add(
                        SpotifyPlaylist(
                            id = id,
                            name = name,
                            spotifyUrl = playlist.optJSONObject("external_urls")
                                ?.optString("spotify")
                                ?.trim()
                                ?.takeIf(String::isNotBlank),
                        ),
                    )
                }
            },
            nextUrl = json.nullableString("next"),
        )
    }
}

internal object SpotifyPlaylistItemsJsonParser {
    fun parsePage(body: String): SpotifyParsedPage<SpotifyPlaylistTrack> {
        val json = JSONObject(body)
        val items = json.optJSONArray("items") ?: JSONArray()
        return SpotifyParsedPage(
            items = buildList {
                for (index in 0 until items.length()) {
                    val playlistItem = items.optJSONObject(index) ?: continue
                    val track = playlistItem.optJSONObject("item")
                        ?: playlistItem.optJSONObject("track")
                        ?: continue
                    val type = track.optString("type", "track")
                    if (type != "track") continue
                    val id = track.optString("id").trim()
                    val title = track.optString("name").trim()
                    if (id.isBlank() || title.isBlank()) continue
                    val artists = track.optJSONArray("artists").toStrings("name")
                    if (artists.isEmpty()) continue
                    val album = track.optJSONObject("album")
                    add(
                        SpotifyPlaylistTrack(
                            spotifyTrackId = id,
                            title = title,
                            artists = artists,
                            album = album?.optString("name")?.trim()?.takeIf(String::isNotBlank),
                            artworkUrl = album?.optJSONArray("images")
                                .firstObjectString("url"),
                            spotifyUrl = track.optJSONObject("external_urls")
                                ?.optString("spotify")
                                ?.trim()
                                ?.takeIf(String::isNotBlank),
                            isrc = track.optJSONObject("external_ids")
                                ?.optString("isrc")
                                ?.trim()
                                ?.takeIf(String::isNotBlank),
                            addedAt = playlistItem.nullableString("added_at"),
                        ),
                    )
                }
            },
            nextUrl = json.nullableString("next"),
        )
    }
}

private fun JSONObject.nullableString(key: String): String? =
    optString(key).trim().takeIf(String::isNotBlank)?.takeUnless { it == "null" }

private fun JSONArray?.toStrings(key: String): List<String> = buildList {
    val array = this@toStrings ?: return@buildList
    for (index in 0 until array.length()) {
        array.optJSONObject(index)?.optString(key)?.trim()?.takeIf(String::isNotBlank)?.let(::add)
    }
}

private fun JSONArray?.firstObjectString(key: String): String? {
    val array = this ?: return null
    for (index in 0 until array.length()) {
        val value = array.optJSONObject(index)?.optString(key)?.trim()
        if (!value.isNullOrBlank()) return value
    }
    return null
}

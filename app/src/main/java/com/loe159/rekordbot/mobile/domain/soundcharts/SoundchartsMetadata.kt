package com.loe159.rekordbot.mobile.domain.soundcharts

data class SoundchartsMetadata(
    val genres: List<String>,
    val isrc: String? = null,
) {
    val rawGenre: String?
        get() = genres.takeIf(List<String>::isNotEmpty)?.joinToString(", ")
}

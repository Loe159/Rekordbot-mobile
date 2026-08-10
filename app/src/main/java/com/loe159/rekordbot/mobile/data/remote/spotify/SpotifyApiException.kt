package com.loe159.rekordbot.mobile.data.remote.spotify

class SpotifyApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

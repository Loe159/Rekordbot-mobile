package com.loe159.rekordbot.mobile.data.remote.soundcharts

class SoundchartsApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

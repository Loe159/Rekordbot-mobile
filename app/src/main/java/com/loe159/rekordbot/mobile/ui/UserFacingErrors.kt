package com.loe159.rekordbot.mobile.ui

import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableApiException
import com.loe159.rekordbot.mobile.data.remote.soundcharts.SoundchartsApiException

internal fun Throwable.toUserFacingMessage(fallback: String): String = when (this) {
    is AirtableApiException,
    is SoundchartsApiException -> message?.takeIf(String::isNotBlank) ?: fallback
    else -> fallback
}

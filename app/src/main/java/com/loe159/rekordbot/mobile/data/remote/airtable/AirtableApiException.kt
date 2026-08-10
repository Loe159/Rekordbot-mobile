package com.loe159.rekordbot.mobile.data.remote.airtable

class AirtableApiException(
    override val message: String,
    val statusCode: Int? = null,
) : Exception(message)

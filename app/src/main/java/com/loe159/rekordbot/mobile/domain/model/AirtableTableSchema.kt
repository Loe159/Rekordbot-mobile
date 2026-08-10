package com.loe159.rekordbot.mobile.domain.model

data class AirtableTableSchema(
    val id: String,
    val name: String,
    val fields: List<AirtableFieldSchema>,
)

data class AirtableFieldSchema(
    val id: String,
    val name: String,
    val type: String,
)

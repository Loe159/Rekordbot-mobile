package com.loe159.rekordbot.mobile.ui.home

data class HomeUiState(
    val isAirtableConfigured: Boolean = false,
    val pendingTracks: Int = 0,
    val pendingShazams: Int = 0,
)

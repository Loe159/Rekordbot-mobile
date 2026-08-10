package com.loe159.rekordbot.mobile.ui.settings

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration

data class SettingsUiState(
    val configuration: AirtableConfiguration = AirtableConfiguration(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val isConnectionValidated: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val savedVersion: Int = 0,
)

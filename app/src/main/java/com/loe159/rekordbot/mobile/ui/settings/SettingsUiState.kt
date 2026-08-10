package com.loe159.rekordbot.mobile.ui.settings

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration

data class SettingsUiState(
    val configuration: AirtableConfiguration = AirtableConfiguration(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val isConnectionValidated: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val savedVersion: Int = 0,
    val soundchartsConfiguration: SoundchartsConfiguration = SoundchartsConfiguration(),
    val isSoundchartsBusy: Boolean = false,
    val soundchartsMessage: String? = null,
    val isSoundchartsError: Boolean = false,
)

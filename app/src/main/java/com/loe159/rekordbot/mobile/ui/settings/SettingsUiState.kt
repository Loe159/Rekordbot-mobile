package com.loe159.rekordbot.mobile.ui.settings

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.airtable.AirtableBaseSummary
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTableSummary
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration

data class SettingsUiState(
    val configuration: AirtableConfiguration = AirtableConfiguration(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val isConnectionValidated: Boolean = false,
    val isOAuthAvailable: Boolean = false,
    val isOAuthConnected: Boolean = false,
    val isOAuthBusy: Boolean = false,
    val availableBases: List<AirtableBaseSummary> = emptyList(),
    val availableTables: List<AirtableTableSummary> = emptyList(),
    val message: String? = null,
    val isError: Boolean = false,
    val savedVersion: Int = 0,
    val soundchartsConfiguration: SoundchartsConfiguration = SoundchartsConfiguration(),
    val isSoundchartsBusy: Boolean = false,
    val soundchartsMessage: String? = null,
    val isSoundchartsError: Boolean = false,
)

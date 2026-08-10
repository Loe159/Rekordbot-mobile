package com.loe159.rekordbot.mobile.domain.soundcharts

data class SoundchartsConfiguration(
    val enabled: Boolean = false,
    val appId: String = "",
    val apiKey: String = "",
) {
    val isComplete: Boolean
        get() = appId.isNotBlank() && apiKey.isNotBlank()
}

object SoundchartsConfigurationValidator {
    fun localErrors(configuration: SoundchartsConfiguration): List<String> = buildList {
        if (!configuration.enabled) return@buildList
        if (configuration.appId.isBlank()) add("L’App ID Soundcharts est obligatoire.")
        if (configuration.apiKey.isBlank()) add("La clé API Soundcharts est obligatoire.")
    }
}

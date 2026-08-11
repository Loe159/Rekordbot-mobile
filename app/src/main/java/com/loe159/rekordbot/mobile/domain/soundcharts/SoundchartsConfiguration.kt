package com.loe159.rekordbot.mobile.domain.soundcharts

data class SoundchartsConfiguration(
    val enabled: Boolean = false,
    val appId: String = "",
    val apiKey: String = "",
    val accessMode: SoundchartsAccessMode = SoundchartsAccessMode.LEGACY_CREDENTIALS,
) {
    val isComplete: Boolean
        get() = accessMode == SoundchartsAccessMode.MANAGED_SERVICE ||
            (appId.isNotBlank() && apiKey.isNotBlank())
}

enum class SoundchartsAccessMode {
    MANAGED_SERVICE,
    LEGACY_CREDENTIALS,
}

object SoundchartsConfigurationValidator {
    fun localErrors(configuration: SoundchartsConfiguration): List<String> = buildList {
        if (!configuration.enabled) return@buildList
        if (configuration.accessMode == SoundchartsAccessMode.MANAGED_SERVICE) return@buildList
        if (configuration.appId.isBlank()) add("L’App ID Soundcharts est obligatoire.")
        if (configuration.apiKey.isBlank()) add("La clé API Soundcharts est obligatoire.")
    }
}

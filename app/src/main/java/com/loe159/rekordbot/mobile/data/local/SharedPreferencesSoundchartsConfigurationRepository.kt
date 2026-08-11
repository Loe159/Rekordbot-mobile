package com.loe159.rekordbot.mobile.data.local

import android.content.Context
import com.loe159.rekordbot.mobile.domain.repository.SoundchartsConfigurationRepository
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsAccessMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesSoundchartsConfigurationRepository(
    context: Context,
    private val credentialVault: AndroidKeystoreSoundchartsCredentialVault =
        AndroidKeystoreSoundchartsCredentialVault(context),
    private val managedServiceAvailable: Boolean = false,
) : SoundchartsConfigurationRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun load(): SoundchartsConfiguration = withContext(Dispatchers.IO) {
        val (appId, apiKey) = credentialVault.read()
        SoundchartsConfiguration(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            appId = appId,
            apiKey = apiKey,
            accessMode = if (managedServiceAvailable) {
                SoundchartsAccessMode.MANAGED_SERVICE
            } else {
                SoundchartsAccessMode.LEGACY_CREDENTIALS
            },
        )
    }

    override suspend fun save(configuration: SoundchartsConfiguration) =
        withContext(Dispatchers.IO) {
            credentialVault.write(configuration.appId.trim(), configuration.apiKey.trim())
            preferences.edit().putBoolean(KEY_ENABLED, configuration.enabled).commit()
            Unit
        }

    private companion object {
        const val PREFERENCES_NAME = "rekordbot_soundcharts_configuration"
        const val KEY_ENABLED = "enabled"
    }
}

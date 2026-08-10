package com.loe159.rekordbot.mobile.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** A dedicated vault: Soundcharts credentials never share Airtable storage or key material. */
class AndroidKeystoreSoundchartsCredentialVault(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): Pair<String, String> {
        val encrypted = preferences.getString(KEY_CREDENTIALS, null) ?: return "" to ""
        val iv = preferences.getString(KEY_INITIALIZATION_VECTOR, null) ?: return "" to ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
            )
            val json = JSONObject(
                cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).decodeToString(),
            )
            json.optString("appId") to json.optString("apiKey")
        }.getOrElse {
            preferences.edit().clear().apply()
            "" to ""
        }
    }

    fun write(appId: String, apiKey: String) {
        if (appId.isBlank() && apiKey.isBlank()) {
            preferences.edit().clear().apply()
            return
        }
        val plainText = JSONObject().put("appId", appId).put("apiKey", apiKey).toString()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.encodeToByteArray())
        preferences.edit()
            .putString(KEY_CREDENTIALS, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_INITIALIZATION_VECTOR, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "rekordbot_soundcharts_credentials_key"
        const val PREFERENCES_NAME = "rekordbot_secure_soundcharts"
        const val KEY_CREDENTIALS = "encrypted_credentials"
        const val KEY_INITIALIZATION_VECTOR = "credentials_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}

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

class AndroidKeystoreTokenVault(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): String {
        val encryptedToken = preferences.getString(KEY_TOKEN, null) ?: return ""
        val initializationVector = preferences.getString(KEY_INITIALIZATION_VECTOR, null) ?: return ""

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(initializationVector, Base64.NO_WRAP)),
            )
            cipher.doFinal(Base64.decode(encryptedToken, Base64.NO_WRAP)).decodeToString()
        }.getOrElse {
            preferences.edit().clear().apply()
            ""
        }
    }

    fun write(token: String) {
        if (token.isBlank()) {
            preferences.edit().clear().apply()
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encryptedToken = cipher.doFinal(token.encodeToByteArray())

        preferences.edit()
            .putString(KEY_TOKEN, Base64.encodeToString(encryptedToken, Base64.NO_WRAP))
            .putString(
                KEY_INITIALIZATION_VECTOR,
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            )
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
        const val KEY_ALIAS = "rekordbot_airtable_token_key"
        const val PREFERENCES_NAME = "rekordbot_secure_airtable"
        const val KEY_TOKEN = "encrypted_token"
        const val KEY_INITIALIZATION_VECTOR = "token_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}

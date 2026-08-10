package com.loe159.rekordbot.mobile.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.loe159.rekordbot.mobile.domain.repository.SpotifySessionRepository
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyAuthorizationSession
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Spotify tokens and the pending PKCE verifier use dedicated Spotify-only key material. */
class AndroidKeystoreSpotifySessionRepository(
    context: Context,
) : SpotifySessionRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun loadTokens(): SpotifyTokenSet? = withContext(Dispatchers.IO) {
        decrypt(KEY_TOKENS, KEY_TOKENS_IV)?.let(::tokensFromJson)
    }

    override suspend fun saveTokens(tokens: SpotifyTokenSet) = withContext(Dispatchers.IO) {
        encrypt(KEY_TOKENS, KEY_TOKENS_IV, tokens.toJson().toString())
    }

    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        preferences.edit().remove(KEY_TOKENS).remove(KEY_TOKENS_IV).commit()
        Unit
    }

    override suspend fun loadPendingAuthorization(): SpotifyAuthorizationSession? =
        withContext(Dispatchers.IO) {
            decrypt(KEY_PENDING_AUTHORIZATION, KEY_PENDING_AUTHORIZATION_IV)
                ?.let(::authorizationFromJson)
        }

    override suspend fun savePendingAuthorization(session: SpotifyAuthorizationSession) =
        withContext(Dispatchers.IO) {
            val json = JSONObject()
                .put("authorizationUrl", session.authorizationUrl)
                .put("state", session.state)
                .put("codeVerifier", session.codeVerifier.reveal())
            encrypt(
                KEY_PENDING_AUTHORIZATION,
                KEY_PENDING_AUTHORIZATION_IV,
                json.toString(),
            )
        }

    override suspend fun clearPendingAuthorization() = withContext(Dispatchers.IO) {
        preferences.edit()
            .remove(KEY_PENDING_AUTHORIZATION)
            .remove(KEY_PENDING_AUTHORIZATION_IV)
            .commit()
        Unit
    }

    private fun SpotifyTokenSet.toJson(): JSONObject = JSONObject()
        .put("accessToken", accessToken.reveal())
        .put("refreshToken", refreshToken?.reveal())
        .put("expiresAt", expiresAtEpochSeconds)
        .put("tokenType", tokenType)
        .put("scopes", JSONArray(scopes.sorted()))

    private fun tokensFromJson(value: String): SpotifyTokenSet? = runCatching {
        val json = JSONObject(value)
        val scopesJson = json.optJSONArray("scopes") ?: JSONArray()
        val scopes = buildSet {
            for (index in 0 until scopesJson.length()) {
                scopesJson.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
        SpotifyTokenSet(
            accessToken = SpotifySecret.from(json.getString("accessToken")),
            refreshToken = json.optString("refreshToken")
                .trim()
                .takeIf(String::isNotBlank)
                ?.let(SpotifySecret::from),
            expiresAtEpochSeconds = json.getLong("expiresAt"),
            tokenType = json.optString("tokenType", "Bearer"),
            scopes = scopes,
        )
    }.getOrElse {
        preferences.edit().remove(KEY_TOKENS).remove(KEY_TOKENS_IV).apply()
        null
    }

    private fun authorizationFromJson(value: String): SpotifyAuthorizationSession? = runCatching {
        val json = JSONObject(value)
        SpotifyAuthorizationSession(
            authorizationUrl = json.getString("authorizationUrl"),
            state = json.getString("state"),
            codeVerifier = SpotifySecret.from(json.getString("codeVerifier")),
        )
    }.getOrElse {
        preferences.edit()
            .remove(KEY_PENDING_AUTHORIZATION)
            .remove(KEY_PENDING_AUTHORIZATION_IV)
            .apply()
        null
    }

    private fun encrypt(valueKey: String, ivKey: String, plainText: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.encodeToByteArray())
        preferences.edit()
            .putString(valueKey, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(ivKey, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()
    }

    private fun decrypt(valueKey: String, ivKey: String): String? {
        val encrypted = preferences.getString(valueKey, null) ?: return null
        val iv = preferences.getString(ivKey, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
            )
            cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).decodeToString()
        }.getOrElse {
            preferences.edit().remove(valueKey).remove(ivKey).apply()
            null
        }
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
        const val KEY_ALIAS = "rekordbot_spotify_session_key"
        const val PREFERENCES_NAME = "rekordbot_secure_spotify"
        const val KEY_TOKENS = "encrypted_tokens"
        const val KEY_TOKENS_IV = "tokens_iv"
        const val KEY_PENDING_AUTHORIZATION = "encrypted_pending_authorization"
        const val KEY_PENDING_AUTHORIZATION_IV = "pending_authorization_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}

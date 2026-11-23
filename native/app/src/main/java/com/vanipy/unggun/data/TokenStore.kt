package com.vanipy.unggun.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** GitHub token, AES/GCM encrypted with an Android Keystore key. Never written to logs. */
object TokenStore {

    private const val KEY_ALIAS = "unggun_github_token"
    private const val PREF = "github_token_enc"

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun secretKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun store(context: Context, token: String) {
        val prefs = context.getSharedPreferences("unggun_prefs", Context.MODE_PRIVATE)
        if (token.isBlank()) {
            prefs.edit().remove(PREF).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val enc = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(iv.size + enc.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(enc, 0, packed, iv.size, enc.size)
        prefs.edit().putString(PREF, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun read(context: Context): String {
        val prefs = context.getSharedPreferences("unggun_prefs", Context.MODE_PRIVATE)
        val packed64 = prefs.getString(PREF, null) ?: return ""
        return try {
            val packed = Base64.decode(packed64, Base64.NO_WRAP)
            val iv = packed.copyOfRange(0, 12)
            val enc = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(enc), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun hasToken(context: Context): Boolean = read(context).isNotBlank()
}

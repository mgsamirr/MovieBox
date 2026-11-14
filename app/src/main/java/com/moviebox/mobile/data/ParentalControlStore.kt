package com.moviebox.mobile.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class ParentalControlStore(context: Context) {
    private val prefs = context.getSharedPreferences("moviebox_parental_controls", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = !prefs.getString("pin_hash", null).isNullOrBlank()

    fun setPin(pin: String) {
        require(pin.length == 4 && pin.all(Char::isDigit)) { "PIN must contain exactly 4 digits." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = digest(salt, pin)
        prefs.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltText = prefs.getString("pin_salt", null) ?: return false
        val hashText = prefs.getString("pin_hash", null) ?: return false
        return runCatching {
            val salt = Base64.decode(saltText, Base64.NO_WRAP)
            val expected = Base64.decode(hashText, Base64.NO_WRAP)
            MessageDigest.isEqual(expected, digest(salt, pin))
        }.getOrDefault(false)
    }

    fun clearPin() {
        prefs.edit().remove("pin_salt").remove("pin_hash").apply()
    }

    private fun digest(salt: ByteArray, pin: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray(Charsets.UTF_8))
}

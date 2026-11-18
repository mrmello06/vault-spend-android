package com.moneytracker.vault.security

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

class VaultAuthManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "vault_secure_gate"
        private const val KEY_SALT = "pin_salt_b64"
        private const val KEY_HASH = "pin_hash_b64"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        private const val KEY_SEC_Q_SALT = "sec_q_salt_b64"
        private const val KEY_SEC_A1_HASH = "sec_a1_hash_b64"
        private const val KEY_SEC_A2_HASH = "sec_a2_hash_b64"
        private const val KEY_SEC_A3_HASH = "sec_a3_hash_b64"

        val SECURITY_QUESTIONS = listOf(
            "What was the first movie you watched in a theatre?",
            "Who was your childhood best friend?",
            "What is your favourite food or dish?"
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPasscodeConfigured(): Boolean {
        return !prefs.getString(KEY_HASH, null).isNullOrBlank()
    }

    fun hasSecurityQuestionsConfigured(): Boolean {
        return !prefs.getString(KEY_SEC_A1_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_SEC_A2_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_SEC_A3_HASH, null).isNullOrBlank()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true) && canUseBiometricHardware()
    }

    fun canUseBiometricHardware(): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun setupPasscode(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashSecret(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .apply()
    }

    fun verifyPasscode(candidatePin: String): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val expectedHashB64 = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(expectedHashB64, Base64.NO_WRAP)
        val actualHash = hashSecret(candidatePin, salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    fun saveSecurityQuestions(answer1: String, answer2: String, answer3: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val h1 = hashSecret(normalizeAnswer(answer1), salt)
        val h2 = hashSecret(normalizeAnswer(answer2), salt)
        val h3 = hashSecret(normalizeAnswer(answer3), salt)

        prefs.edit()
            .putString(KEY_SEC_Q_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_SEC_A1_HASH, Base64.encodeToString(h1, Base64.NO_WRAP))
            .putString(KEY_SEC_A2_HASH, Base64.encodeToString(h2, Base64.NO_WRAP))
            .putString(KEY_SEC_A3_HASH, Base64.encodeToString(h3, Base64.NO_WRAP))
            .apply()
    }

    fun verifySecurityQuestions(answer1: String, answer2: String, answer3: String): Boolean {
        val saltB64 = prefs.getString(KEY_SEC_Q_SALT, null) ?: return false
        val e1B64 = prefs.getString(KEY_SEC_A1_HASH, null) ?: return false
        val e2B64 = prefs.getString(KEY_SEC_A2_HASH, null) ?: return false
        val e3B64 = prefs.getString(KEY_SEC_A3_HASH, null) ?: return false

        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val ok1 = MessageDigest.isEqual(
            Base64.decode(e1B64, Base64.NO_WRAP),
            hashSecret(normalizeAnswer(answer1), salt)
        )
        val ok2 = MessageDigest.isEqual(
            Base64.decode(e2B64, Base64.NO_WRAP),
            hashSecret(normalizeAnswer(answer2), salt)
        )
        val ok3 = MessageDigest.isEqual(
            Base64.decode(e3B64, Base64.NO_WRAP),
            hashSecret(normalizeAnswer(answer3), salt)
        )
        return ok1 && ok2 && ok3
    }

    fun triggerBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onErrorMessage: (String) -> Unit
    ) {
        if (!canUseBiometricHardware()) {
            onErrorMessage("Fingerprint sensor unavailable")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onErrorMessage(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vault")
            .setSubtitle("Verify your fingerprint")
            .setNegativeButtonText("Use Passcode")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    private fun normalizeAnswer(raw: String): String {
        return raw.trim().lowercase(Locale.US).replace(Regex("""\s+"""), " ")
    }

    private fun hashSecret(secret: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(secret.toByteArray(Charsets.UTF_8))
    }
}

package com.sparsh.myapplication.ui

import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Authenticate to reveal Net Income stats",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val biometricManager = BiometricManager.from(activity)
        
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            BiometricManager.Authenticators.BIOMETRIC_WEAK or 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            BiometricManager.Authenticators.BIOMETRIC_WEAK or 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS && canAuth != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            // Biometrics or credentials not configured/supported on this device - fallback automatically for usability
            Toast.makeText(activity, "Biometric & Face ID security not available on this device", Toast.LENGTH_SHORT).show()
            onSuccess()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
                    }
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            }
        )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        try {
            promptInfoBuilder.setAllowedAuthenticators(authenticators)
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            // Fallback for older devices if combined authenticators fail
            try {
                val fallbackPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                prompt.authenticate(fallbackPromptInfo)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(activity, "Unable to launch biometrics: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
    }
}

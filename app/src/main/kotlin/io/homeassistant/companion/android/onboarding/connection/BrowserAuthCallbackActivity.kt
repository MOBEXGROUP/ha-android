package io.homeassistant.companion.android.onboarding.connection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.launch.LaunchActivity
import javax.inject.Inject
import timber.log.Timber

/**
 * Receives the [AUTH_CALLBACK] redirect when the user signs in with the external browser during
 * onboarding.
 *
 * The received authorization code is handed to [BrowserAuthManager] for the connection screen to
 * consume, then the task hosting the onboarding flow is brought back to the front. The intent
 * flags clear the browser Custom Tab still sitting on top of the original task and deliver a new
 * intent to the existing [LaunchActivity] instance instead of recreating it, keeping the
 * navigation state (and with it the connection screen waiting for the code) intact.
 */
@AndroidEntryPoint
internal class BrowserAuthCallbackActivity : ComponentActivity() {

    @Inject
    internal lateinit var browserAuthManager: BrowserAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val code = intent?.data
            ?.takeIf { it.scheme == AUTH_CALLBACK_SCHEME && it.host == AUTH_CALLBACK_HOST }
            ?.getQueryParameter(AUTH_CALLBACK_CODE_PARAMETER)
        if (code.isNullOrBlank()) {
            Timber.w("Auth callback received without an authorization code")
        } else {
            browserAuthManager.onAuthCodeReceived(code)
        }

        startActivity(
            LaunchActivity.newInstance(this).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
        finish()
    }
}

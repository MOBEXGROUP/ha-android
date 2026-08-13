package io.homeassistant.companion.android.onboarding.connection

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal const val AUTH_CALLBACK_SCHEME = "homeassistant"
internal const val AUTH_CALLBACK_HOST = "auth-callback"
internal const val AUTH_CALLBACK = "$AUTH_CALLBACK_SCHEME://$AUTH_CALLBACK_HOST"
internal const val AUTH_CALLBACK_CODE_PARAMETER = "code"

/**
 * Hands over authorization codes received from the external browser to the connection screen.
 *
 * When the user signs in with the browser instead of the onboarding WebView, Home Assistant
 * redirects to [AUTH_CALLBACK], which the system delivers to [BrowserAuthCallbackActivity] as a
 * new activity instance. This manager bridges the received code back to the
 * [ConnectionViewModel] that started the sign-in and is still alive in the original task.
 */
@Singleton
internal class BrowserAuthManager @Inject constructor() {

    private val authCodes = Channel<String>(capacity = Channel.CONFLATED)

    /**
     * Authorization codes received from the external browser.
     * Each code is delivered to a single collector.
     */
    val authCodeFlow: Flow<String> = authCodes.receiveAsFlow()

    /** Publishes an authorization [code] received from the external browser. */
    fun onAuthCodeReceived(code: String) {
        authCodes.trySend(code)
    }

    /**
     * Drops a pending code that no connection screen has consumed, so a stale code from an
     * earlier abandoned sign-in is not replayed to the next connection screen.
     */
    fun clearPendingAuthCode() {
        authCodes.tryReceive()
    }
}

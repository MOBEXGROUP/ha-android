package io.homeassistant.companion.android.onboarding.connection

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BrowserAuthManagerTest {

    private val browserAuthManager = BrowserAuthManager()

    @Test
    fun `Given a received code when collecting then the code is delivered`() = runTest {
        browserAuthManager.onAuthCodeReceived("auth_code")

        assertEquals("auth_code", browserAuthManager.authCodeFlow.first())
    }

    @Test
    fun `Given two received codes when collecting then only the latest code is delivered`() = runTest {
        browserAuthManager.onAuthCodeReceived("first_code")
        browserAuthManager.onAuthCodeReceived("second_code")

        assertEquals("second_code", browserAuthManager.authCodeFlow.first())
    }

    @Test
    fun `Given a pending code when clearPendingAuthCode is called then the code is not delivered`() = runTest {
        browserAuthManager.onAuthCodeReceived("stale_code")

        browserAuthManager.clearPendingAuthCode()

        browserAuthManager.authCodeFlow.test {
            expectNoEvents()
        }
    }
}

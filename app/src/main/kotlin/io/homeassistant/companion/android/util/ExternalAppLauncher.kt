package io.homeassistant.companion.android.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.webkit.WebViewCompat
import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.util.launchAppOrStore
import timber.log.Timber

/**
 * Opens the store/app page for the device's current WebView provider so the user can update it.
 *
 * When the provider package cannot be resolved (e.g. no updatable provider), [onShowSnackbar] surfaces the
 * failure to the user. Otherwise the provider is opened via [launchAppOrStore], which itself falls back to a
 * snackbar when nothing can handle the launch.
 */
suspend fun Context.updateSystemWebView(onShowSnackbar: suspend (message: String, action: String?) -> Boolean) {
    val webViewPackage = WebViewCompat.getCurrentWebViewPackage(this)?.packageName
    if (webViewPackage == null) {
        Timber.w("No current WebView package, cannot open update page")
        onShowSnackbar(
            getString(commonR.string.fail_to_navigate_to_uri, getString(commonR.string.system_webview)),
            null,
        )
        return
    }
    launchAppOrStore(webViewPackage, onShowSnackbar)
}

/**
 * Opens [uri] in a Custom Tab of the user's default browser.
 *
 * Unlike a WebView, the Custom Tab shares its session state (e.g. cookies) with the browser,
 * letting the user reuse existing sessions such as a sign-in at an SSO provider. When no browser
 * is available to open the tab, [onShowSnackbar] surfaces the failure to the user.
 */
suspend fun Context.openCustomTab(uri: Uri, onShowSnackbar: suspend (message: String, action: String?) -> Boolean) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(this, uri)
    } catch (e: ActivityNotFoundException) {
        // The exception embeds the intent including its data URI, so it is only logged in debug
        // builds to avoid leaking the server URL in release logs.
        Timber.e(e.takeIf { BuildConfig.DEBUG }, "No browser available to open a Custom Tab")
        onShowSnackbar(getString(commonR.string.fail_to_navigate_to_uri, uri.toString()), null)
    }
}

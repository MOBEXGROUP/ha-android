package io.homeassistant.companion.android.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.util.AndroidRuntimeException
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
 * is available or allowed to open the tab, [onShowSnackbar] surfaces the failure to the user.
 */
suspend fun Context.openCustomTab(uri: Uri, onShowSnackbar: suspend (message: String, action: String?) -> Boolean) {
    // The caught exceptions embed the intent including its data URI, so they are only logged in
    // debug builds to avoid leaking the server URL in release logs. Mirrors the exceptions handled
    // by the common startActivityCatching used for regular links.
    val failure = try {
        CustomTabsIntent.Builder().build().launchUrl(this, uri)
        null
    } catch (e: ActivityNotFoundException) {
        e
    } catch (e: SecurityException) {
        // The resolved browser requires a permission we were not granted to launch it.
        e
    } catch (e: AndroidRuntimeException) {
        // e.g. launching without FLAG_ACTIVITY_NEW_TASK from a non-Activity context.
        e
    }
    if (failure != null) {
        Timber.e(failure.takeIf { BuildConfig.DEBUG }, "Unable to open a Custom Tab")
        onShowSnackbar(getString(commonR.string.fail_to_navigate_to_uri, uri.toString()), null)
    }
}

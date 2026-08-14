package io.homeassistant.companion.android.onboarding.connection

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HAPlainButton
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.frontend.filechooser.FileChooserEffect
import io.homeassistant.companion.android.frontend.filechooser.FileChooserRequest
import io.homeassistant.companion.android.loading.LoadingScreen
import io.homeassistant.companion.android.util.compose.HAPreviews
import io.homeassistant.companion.android.util.compose.webview.HAWebView
import timber.log.Timber

@VisibleForTesting
const val CONNECTION_SCREEN_TAG = "connection_screen"

@VisibleForTesting
const val CONNECTION_SCREEN_ERROR_PLACEHOLDER_TAG = "connection_screen_error"

private val ICON_SIZE = 64.dp

@Composable
internal fun ConnectionScreen(onBackClick: () -> Unit, viewModel: ConnectionViewModel, modifier: Modifier = Modifier) {
    val url by viewModel.urlFlow.collectAsState()
    val isLoading by viewModel.isLoadingFlow.collectAsState()
    val error by viewModel.errorFlow.collectAsState()
    val pendingFileChooser by viewModel.pendingFileChooser.collectAsState()
    val isError = error != null

    ConnectionScreen(
        url = url,
        isLoading = isLoading,
        isError = isError,
        getWebViewClient = viewModel::getWebViewClient,
        webChromeClient = viewModel.webChromeClient,
        pendingFileChooser = pendingFileChooser,
        onBackClick = onBackClick,
        onWebViewCreationFailed = viewModel::onWebViewCreationFailed,
        onSignInWithBrowserClick = viewModel::onSignInWithBrowserClick,
        modifier = modifier,
    )
}

@Composable
internal fun ConnectionScreen(
    url: String?,
    isLoading: Boolean,
    isError: Boolean,
    getWebViewClient: suspend () -> WebViewClient,
    webChromeClient: WebChromeClient,
    onBackClick: () -> Unit,
    onWebViewCreationFailed: (Throwable) -> Unit,
    onSignInWithBrowserClick: () -> Unit,
    modifier: Modifier = Modifier,
    pendingFileChooser: FileChooserRequest? = null,
) {
    FileChooserEffect(pendingRequest = pendingFileChooser)

    Box(modifier = modifier.testTag(CONNECTION_SCREEN_TAG)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                )
                .background(LocalHAColorScheme.current.colorSurfaceDefault),
        )
        if (!isError) {
            url?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    var webView by remember { mutableStateOf<WebView?>(null) }
                    HAWebView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        configure = {
                            this.webChromeClient = webChromeClient
                            webView = this
                        },
                        onBackPressed = onBackClick,
                        onWebViewCreationFailed = onWebViewCreationFailed,
                    )
                    webView?.let { view ->
                        LaunchedEffect(view, url) {
                            view.webViewClient = getWebViewClient()
                            view.loadUrl(url)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LocalHAColorScheme.current.colorSurfaceDefault),
                        contentAlignment = Alignment.Center,
                    ) {
                        HAPlainButton(
                            text = stringResource(commonR.string.connection_screen_sign_in_with_browser),
                            onClick = onSignInWithBrowserClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = HADimens.SPACE1),
                        )
                    }
                }
            } ?: Timber.i("ConnectionScreen: url is null")
        } else {
            ErrorPlaceholder()
        }

        if (isLoading) {
            LoadingScreen(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * This placeholder is used to hide the ugly error screen from the webview, while a toast is displayed
 * before leaving this screen.
 */
@Composable
private fun ErrorPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().testTag(CONNECTION_SCREEN_ERROR_PLACEHOLDER_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_home_assistant_branding),
            contentDescription = null,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}

@HAPreviews
@Composable
private fun ConnectionScreenPreview() {
    HAThemeForPreview {
        ConnectionScreen(
            url = "https://www.home-assistant.io",
            isLoading = false,
            isError = false,
            getWebViewClient = { WebViewClient() },
            webChromeClient = WebChromeClient(),
            onBackClick = {},
            onWebViewCreationFailed = {},
            onSignInWithBrowserClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

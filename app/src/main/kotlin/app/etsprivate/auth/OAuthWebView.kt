package app.etsprivate.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.etsprivate.R

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OAuthWebView(
    authorizeUrl: String,
    redirectPrefix: String,
    onCode: (String) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var currentHost by remember { mutableStateOf("") }
    var pageError by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val signInFailedMessage = stringResource(R.string.error_sign_in_failed)

    fun goBack() {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onCancel()
    }
    BackHandler(onBack = ::goBack)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            Text(
                currentHost,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        pageError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(backgroundColor)
                    webView = this
                    with(settings) {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true

                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                        userAgentString = userAgentString
                            .replace("; wv)", ")")
                            .replace(Regex("Version/\\d+\\.\\d+ "), "")
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    val client = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (!url.startsWith(redirectPrefix)) return false
                            handleRedirect(url, onCode, onError, signInFailedMessage)
                            return true
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                            pageError = null
                            url?.let { currentHost = Uri.parse(it).host.orEmpty() }

                            if (url != null && url.startsWith(redirectPrefix)) {
                                view?.stopLoading()
                                handleRedirect(url, onCode, onError, signInFailedMessage)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            AdfsFixes.apply(view, "EtsOAuthWebView", url)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame != true) return
                            loading = false
                            pageError = "Load error: ${error?.description} (${Uri.parse(request.url.toString()).host})"
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?,
                        ) {
                            if (request?.isForMainFrame != true) return
                            pageError = "HTTP ${errorResponse?.statusCode} from ${Uri.parse(request.url.toString()).host}"
                        }
                    }
                    webViewClient = client

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                            if (message == null) return false
                            android.util.Log.d(
                                "EtsOAuthWebView",
                                "console ${message.messageLevel()}: ${message.message()} " +
                                    "@${message.sourceId()}:${message.lineNumber()}",
                            )
                            if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                pageError = "JS error: ${message.message().take(180)}"
                            }
                            return true
                        }

                        override fun onCreateWindow(
                            view: WebView,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message,
                        ): Boolean {
                            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                            val popup = WebView(view.context)
                            popup.settings.javaScriptEnabled = true
                            popup.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    popupView: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    request?.url?.toString()?.let { view.loadUrl(it) }
                                    return true
                                }
                            }
                            transport.webView = popup
                            resultMsg.sendToTarget()
                            return true
                        }
                    }

                    loadUrl(authorizeUrl)
                }
            },
        )
    }
}

private fun handleRedirect(url: String, onCode: (String) -> Unit, onError: (String) -> Unit, defaultErrorMessage: String) {
    val query = url.substringAfter('?', "")
    val parsed = Uri.parse("https://redirect/?$query")
    val code = parsed.getQueryParameter("code")
    android.util.Log.d("EtsOAuthWebView", "redirect intercepted, code=${code != null} query=${query.take(1000)}")
    if (code != null) {
        onCode(code)
        return
    }
    val error = parsed.getQueryParameter("error_description")
        ?: parsed.getQueryParameter("error")
        ?: defaultErrorMessage
    onError(error)
}

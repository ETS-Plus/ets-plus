package app.etsprivate.ui

import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import app.etsprivate.ui.theme.Spacing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.etsprivate.R
import app.etsprivate.auth.AdfsFixes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppBrowser(url: String, onBack: () -> Unit, visible: Boolean = true) {
    val noAppMessage = stringResource(R.string.browser_no_app)
    val fileFailed = stringResource(R.string.browser_file_failed)
    val openingFile = stringResource(R.string.browser_opening_file)

    var preview by remember { mutableStateOf<Triple<String, String, ByteArray>?>(null) }

    var stream by remember { mutableStateOf<MediaStream?>(null) }
    var fetching by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var pendingFiles by remember { mutableStateOf<android.webkit.ValueCallback<Array<Uri>>?>(null) }
    val pickFiles = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        pendingFiles?.onReceiveValue(
            android.webkit.WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
        )
        pendingFiles = null
    }
    var loading by remember { mutableStateOf(true) }

    var pageFinished by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(pageFinished) {
        if (pageFinished) {
            delay(120)
            hasLoadedOnce = true
        }
    }

    LaunchedEffect(Unit) {
        delay(1500)
        hasLoadedOnce = true
    }
    var title by remember { mutableStateOf(Uri.parse(url).host.orEmpty()) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    var currentUrl by remember { mutableStateOf(url) }
    var historyKey by remember { mutableIntStateOf(0) }
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()

    fun goBack() {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onBack()
    }
    BackHandler(enabled = visible, onBack = ::goBack)

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AppTopBar(title = title, onBack = ::goBack)
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surface)) {
            AndroidView(
                modifier = Modifier.fillMaxSize().alpha(if (hasLoadedOnce) 1f else 0f),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(backgroundColor)
                        webView = this
                        with(settings) {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true

                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true

                            userAgentString = userAgentString.replace(" wv", "")

                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        if (androidx.webkit.WebViewFeature.isFeatureSupported(
                                androidx.webkit.WebViewFeature.SERVICE_WORKER_BASIC_USAGE,
                            )
                        ) {
                            androidx.webkit.ServiceWorkerControllerCompat.getInstance()
                                .setServiceWorkerClient(object : androidx.webkit.ServiceWorkerClientCompat() {
                                    override fun shouldInterceptRequest(
                                        request: android.webkit.WebResourceRequest,
                                    ): android.webkit.WebResourceResponse? = null
                                })
                        }

                        if (androidx.webkit.WebViewFeature.isFeatureSupported(
                                androidx.webkit.WebViewFeature.OFF_SCREEN_PRERASTER,
                            )
                        ) {
                            androidx.webkit.WebSettingsCompat.setOffscreenPreRaster(settings, true)
                        }
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?,
                            ): Boolean {
                                val here = view ?: return false
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                val popup = WebView(here.context)
                                var handed = false
                                fun handOver(target: String?) {
                                    if (handed || target.isNullOrBlank() || target == "about:blank") return
                                    handed = true
                                    here.loadUrl(target)
                                    popup.post { popup.destroy() }
                                }
                                popup.settings.javaScriptEnabled = true
                                popup.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        v: WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                    ): Boolean {
                                        handOver(request?.url?.toString())
                                        return true
                                    }

                                    override fun onPageStarted(v: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                        handOver(pageUrl)
                                    }
                                }
                                transport.webView = popup
                                resultMsg.sendToTarget()
                                return true
                            }

                            override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                                if (message?.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                    android.util.Log.i("Browser", "console ${message.message()?.take(200)} @${message.sourceId()?.takeLast(60)}:${message.lineNumber()}")
                                }
                                return false
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?,
                            ): Boolean {
                                val callback = filePathCallback ?: return false
                                val intent = runCatching { fileChooserParams?.createIntent() }.getOrNull() ?: run {
                                    callback.onReceiveValue(null)
                                    return true
                                }
                                if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                                    intent.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }

                                pendingFiles?.onReceiveValue(null)
                                pendingFiles = callback
                                runCatching { pickFiles.launch(intent) }.onFailure {
                                    pendingFiles = null
                                    callback.onReceiveValue(null)
                                }
                                return true
                            }

                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (newProgress % 25 == 0) {
                                    android.util.Log.i("Browser", "progress $newProgress on ${view?.url}")
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                            ): Boolean {
                                val uri = request?.url ?: return false
                                val scheme = uri.scheme?.lowercase()
                                if (scheme == "http" || scheme == "https") return false
                                val intent = if (scheme == "intent") {
                                    runCatching {
                                        android.content.Intent.parseUri(uri.toString(), android.content.Intent.URI_INTENT_SCHEME)
                                    }.getOrNull()
                                } else {
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                }
                                val opened = intent != null && runCatching {
                                    view?.context?.startActivity(
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }.isSuccess
                                if (!opened) AppFeedback.show(noAppMessage)
                                return true
                            }

                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                loading = true
                                pageUrl?.let {
                                    title = Uri.parse(it).host.orEmpty()
                                    currentUrl = it
                                }
                                historyKey++
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?,
                            ) {
                                android.util.Log.i(
                                    "Browser",
                                    "error ${error?.errorCode} ${error?.description} on ${request?.url}",
                                )
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                response: android.webkit.WebResourceResponse?,
                            ) {
                                android.util.Log.i("Browser", "http ${response?.statusCode} on ${request?.url}")
                            }

                            override fun onPageCommitVisible(view: WebView?, pageUrl: String?) {
                                hasLoadedOnce = true
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                loading = false
                                pageFinished = true
                                AdfsFixes.apply(view, "InAppBrowser", pageUrl)
                                pageUrl?.let { currentUrl = it }
                                historyKey++
                                view?.title?.takeIf { it.isNotBlank() }?.let { title = it }
                            }
                        }

                        setDownloadListener { downloadUrl, userAgent, disposition, mimeType, _ ->
                            val viewContext = this.context
                            val name = android.webkit.URLUtil.guessFileName(downloadUrl, disposition, mimeType)
                            val type = typeOf(name, mimeType)
                            if (app.etsprivate.mail.isMedia(type)) {
                                stream = MediaStream(
                                    name,
                                    type,
                                    downloadUrl,
                                    buildMap {
                                        android.webkit.CookieManager.getInstance().getCookie(downloadUrl)?.let { put("Cookie", it) }
                                        userAgent?.let { put("User-Agent", it) }
                                    },
                                )
                                return@setDownloadListener
                            }
                            scope.launch {
                                fetching = true
                                val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    runCatching { fetchFile(downloadUrl, userAgent, disposition, mimeType) }
                                        .onFailure { android.util.Log.i("Browser", "file failed $downloadUrl: $it") }
                                        .getOrNull()
                                }
                                fetching = false
                                when {
                                    file == null -> AppFeedback.show(fileFailed)
                                    app.etsprivate.mail.isPreviewable(file.second) -> preview = file
                                    else -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        runCatching { app.etsprivate.mail.openWithSystemViewer(viewContext, file.first, file.second, file.third) }
                                    }.onFailure { AppFeedback.show(fileFailed) }
                                }
                            }
                        }
                        loadUrl(url)
                    }
                },

                update = { view ->
                    view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
                },
            )

            if (!hasLoadedOnce) {
                FullScreenLoading(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
            }
        }

        BrowserBar(
            canGoBack = remember(historyKey, webView) { webView?.canGoBack() == true },
            canGoForward = remember(historyKey, webView) { webView?.canGoForward() == true },
            url = currentUrl,
            onBack = { webView?.goBack() },
            onForward = { webView?.goForward() },
        )
    }

    preview?.let { (filename, mimeType, bytes) ->
        app.etsprivate.mail.AttachmentPreviewScreen(filename = filename, mimeType = mimeType, bytes = bytes, onBack = { preview = null })
    }
    stream?.let { media ->
        MediaStreamScreen(media, onBack = { stream = null })
    }
    if (fetching) PreparingOverlay(openingFile)
    }
}

private class MediaStream(val name: String, val mimeType: String, val url: String, val headers: Map<String, String>)

private fun typeOf(name: String, served: String?): String {
    val given = served.orEmpty().substringBefore(';').trim()
    if (given.isNotBlank() && given != "application/octet-stream" && given != "application/force-download") return given
    return android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) ?: "application/octet-stream"
}

@Composable
private fun MediaStreamScreen(media: MediaStream, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val saved = stringResource(R.string.mail_attachment_saved)
    val failed = stringResource(R.string.mail_attachment_save_failed)
    var saving by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AppTopBar(title = media.name, onBack = onBack, actions = {
            IconButton(
                enabled = !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching {
                                val (name, type, bytes) = fetchFile(media.url, media.headers["User-Agent"], null, media.mimeType)
                                app.etsprivate.mail.AttachmentSaver.saveToDownloads(context, name, type, bytes)
                            }.getOrDefault(false)
                        }
                        saving = false
                        AppFeedback.show(if (ok) saved else failed)
                    }
                },
            ) {
                Icon(Icons.Outlined.Download, contentDescription = saved)
            }
        })
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            app.etsprivate.mail.MediaPlayerView(Uri.parse(media.url), media.headers, media.mimeType, media.name)
        }
    }
}

private fun fetchFile(url: String, userAgent: String?, disposition: String?, mimeType: String?): Triple<String, String, ByteArray> {
    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    try {
        connection.instanceFollowRedirects = true
        android.webkit.CookieManager.getInstance().getCookie(url)?.let { connection.setRequestProperty("Cookie", it) }
        userAgent?.let { connection.setRequestProperty("User-Agent", it) }
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        val bytes = connection.inputStream.use { it.readBytes() }
        val finalDisposition = connection.getHeaderField("Content-Disposition") ?: disposition
        val served = (connection.contentType ?: mimeType).orEmpty()
        val name = android.webkit.URLUtil.guessFileName(connection.url.toString(), finalDisposition, served.substringBefore(';').trim())
        return Triple(name, typeOf(name, served), bytes)
    } finally {
        connection.disconnect()
    }
}

@Composable
private fun BrowserBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    url: String,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    val context = LocalContext.current
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                context.getSystemService(android.content.ClipboardManager::class.java)
                    ?.setPrimaryClip(android.content.ClipData.newPlainText(url, url))
            },
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.action_copy_link))
        }
        IconButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        ) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = stringResource(R.string.action_open_in_browser))
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_previous))
        }
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = stringResource(R.string.action_next))
        }
    }
}

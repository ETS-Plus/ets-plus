package app.etsprivate.ets

import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Refresh
import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.etsprivate.R
import app.etsprivate.ui.AppTopBar
import app.etsprivate.ui.FullScreenLoading
import app.etsprivate.ui.PdfView
import app.etsprivate.ui.theme.Spacing
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL

const val STM_PROOF_URL = "https://signets-ens.etsmtl.ca/Secure/DocPreuveSTM.aspx"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StmProofScreen(onBack: () -> Unit, onOpenInBrowser: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val today = remember { java.time.LocalDate.now().toString() }

    var generation by remember { mutableStateOf(0) }
    val kept = remember(generation) { StmProofCache.read(context, today) }
    var pdf by remember(generation) { mutableStateOf(kept) }
    var failed by remember(generation) { mutableStateOf(false) }

    DisposableEffect(generation) {
        if (kept != null) return@DisposableEffect onDispose {}
        val scope = CoroutineScope(Dispatchers.Main)
        var asked = false
        val view = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url != null && url.contains("Erreur", ignoreCase = true)) {
                        failed = true
                        return
                    }
                    if (asked) return
                    asked = true

                    view?.evaluateJavascript(FORM_FIELDS) { answer ->
                        val fields = runCatching { JSONObject(JSONObject("{\"v\":$answer}").getString("v")) }.getOrNull()
                        if (fields == null) {
                            failed = true
                            return@evaluateJavascript
                        }
                        scope.launch {
                            val bytes = withContext(Dispatchers.IO) { postForm(STM_PROOF_URL, fields) }
                            if (bytes == null) failed = true else { pdf = bytes; StmProofCache.write(context, today, bytes) }
                        }
                    }
                }
            }

            setDownloadListener { url, _, _, _, _ ->
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) { fetch(url) }
                    if (bytes == null) failed = true else { pdf = bytes; StmProofCache.write(context, today, bytes) }
                }
            }
        }
        view.loadUrl(STM_PROOF_URL)
        val giveUp = scope.launch {
            kotlinx.coroutines.delay(25_000)
            if (pdf == null) failed = true
        }
        onDispose {
            giveUp.cancel()
            view.destroy()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AppTopBar(
            title = stringResource(R.string.ets_stm_proof),
            onBack = onBack,
            actions = {
                androidx.compose.material3.IconButton(onClick = {
                    StmProofCache.clear(context)
                    generation++
                }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.ets_stm_regenerate),
                    )
                }
            },
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val bytes = pdf
            when {
                bytes != null -> PdfView(bytes)
                failed -> Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.ets_stm_proof_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onOpenInBrowser,
                        modifier = Modifier.padding(top = Spacing.oneHalf),
                    ) {
                        Text(stringResource(R.string.action_open_in_browser))
                    }
                }
                else -> FullScreenLoading()
            }
        }
    }
}

private val FORM_FIELDS = """
(function () {
  var out = {};
  document.querySelectorAll('input,select,textarea').forEach(function (field) {
    if (!field.name) return;
    if (field.type === 'submit' || field.type === 'button') return;
    if ((field.type === 'checkbox' || field.type === 'radio') && !field.checked) return;
    out[field.name] = field.value;
  });
  var go = document.getElementById('ctl00_ContentPlaceHolderMain_BtnGO');
  if (go && go.name) out[go.name] = go.value;
  return JSON.stringify(out);
})();
""".trimIndent()

private fun postForm(url: String, fields: JSONObject): ByteArray? = runCatching {
    val body = fields.keys().asSequence().joinToString("&") { key ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(fields.optString(key), "UTF-8")}"
    }
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        instanceFollowRedirects = true
        connectTimeout = 20_000
        readTimeout = 20_000
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        setRequestProperty("Referer", url)
        CookieManager.getInstance().getCookie(url)?.let { setRequestProperty("Cookie", it) }
    }
    connection.outputStream.use { it.write(body.toByteArray()) }
    val type = connection.contentType.orEmpty()
    val bytes = connection.inputStream.use { it.readBytes() }
    android.util.Log.i("StmProof", "post -> ${connection.responseCode} $type ${bytes.size} bytes")

    bytes.takeIf { it.size > 4 && String(it, 0, 4) == "%PDF" }
}.getOrNull()

private fun fetch(url: String): ByteArray? = runCatching {
    val connection = URL(url).openConnection() as HttpURLConnection
    CookieManager.getInstance().getCookie(url)?.let { connection.setRequestProperty("Cookie", it) }
    connection.connectTimeout = 20_000
    connection.readTimeout = 20_000
    connection.inputStream.use { it.readBytes() }.takeIf { connection.responseCode == 200 && it.isNotEmpty() }
}.getOrNull()

private object StmProofCache {
    private fun file(context: android.content.Context) = java.io.File(context.filesDir, "stm-proof.pdf")
    private fun dayFile(context: android.content.Context) = java.io.File(context.filesDir, "stm-proof.day")

    fun read(context: android.content.Context, day: String): ByteArray? = runCatching {
        if (dayFile(context).readText() != day) return null
        file(context).readBytes().takeIf { it.size > 4 && String(it, 0, 4) == "%PDF" }
    }.getOrNull()

    fun clear(context: android.content.Context) {
        runCatching { file(context).delete(); dayFile(context).delete() }
    }

    fun write(context: android.content.Context, day: String, bytes: ByteArray) {
        runCatching {
            file(context).writeBytes(bytes)
            dayFile(context).writeText(day)
        }
    }
}

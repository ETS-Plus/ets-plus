package app.etsprivate.ets

import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import app.etsprivate.cache.DiskCache
import app.etsprivate.cache.MemoryCache
import app.etsprivate.R
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.TopLoadingBar
import app.etsprivate.ui.theme.Spacing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

data class RefundItem(val labelRes: Int, val url: String, val icon: ImageVector, val external: Boolean = false)

val REFUND_ITEMS = listOf(
    RefundItem(
        R.string.ets_refund_development_fund,
        "https://signets-ens.etsmtl.ca/Secure/ActionAnnulerFondsDeveloppement.aspx",
        Icons.Outlined.Savings,
    ),
    RefundItem(
        R.string.ets_refund_sustainable_fund,
        "https://signets-ens.etsmtl.ca/Secure/ActionAnnulerFondsDeveloppementDurable.aspx",
        Icons.Outlined.Eco,
    ),
    RefundItem(
        R.string.ets_refund_student_paper,
        "https://signets-ens.etsmtl.ca/Secure/ActionAnnulerContributionJournalEtudiant.aspx",
        Icons.Outlined.Newspaper,
    ),
    RefundItem(
        R.string.ets_refund_science_clubs,
        "https://signets-ens.etsmtl.ca/Secure/ActionAnnulerContributionClulbsScientifiques.aspx",
        Icons.Outlined.Science,
    ),

    RefundItem(
        R.string.ets_refund_aseq,
        "https://signets-ens.etsmtl.ca/Secure/ActionAnnulerASEQ.aspx",
        Icons.Outlined.HealthAndSafety,
        external = true,
    ),
)

data class RefundStatus(
    val canCancel: Boolean,
    val cancelled: Boolean,
    val amount: String?,
    val window: String?,
    val note: String?,
) {
    val nothingToDo: Boolean get() = !canCancel && !cancelled

    val settled: Boolean get() = !canCancel
}

private const val CHECKBOX_ID = "ctl00_ContentPlaceHolderMain_CheckAnn"
private const val CACHE_KEY = "ets/refunds"

private fun encodeStatuses(statuses: Map<String, RefundStatus?>): String =
    JSONObject().apply {
        statuses.forEach { (url, status) ->
            if (status != null) {
                put(
                    url,
                    JSONObject()
                        .put("canCancel", status.canCancel)
                        .put("cancelled", status.cancelled)
                        .putOpt("amount", status.amount)
                        .putOpt("window", status.window)
                        .putOpt("note", status.note),
                )
            }
        }
    }.toString()

private fun decodeStatuses(raw: String): Map<String, RefundStatus?> {
    val json = JSONObject(raw)
    return json.keys().asSequence().associateWith { url ->
        val o = json.getJSONObject(url)
        RefundStatus(
            canCancel = o.optBoolean("canCancel"),
            cancelled = o.optBoolean("cancelled"),
            amount = o.optString("amount").ifBlank { null },
            window = o.optString("window").ifBlank { null },
            note = o.optString("note").ifBlank { null },
        )
    }
}

private val PROBE = """
(function () {
  var box = document.getElementById('$CHECKBOX_ID');
  var texts = [];
  var nodes = document.querySelectorAll('td, span, div, p, li');
  for (var i = 0; i < nodes.length; i++) {
    var t = (nodes[i].textContent || '').replace(/\s+/g, ' ').trim();
    if (t && t.length < 160) texts.push(t);
  }
  function first(re) {
    for (var i = 0; i < texts.length; i++) if (re.test(texts[i])) return texts[i];
    return null;
  }
  return JSON.stringify({
    url: location.href,
    hasBox: !!box,
    checked: !!(box && box.checked),
    disabled: !!(box && box.disabled),
    amount: first(/\d[\d ,.]*\s*\${'$'}/),
    window: first(/^annulation permise/i),
    note: first(/^vous avez d|^utilisation interdite/i)
  });
})();
""".trimIndent()

private const val CANCEL_SCRIPT =
    "(function(){var b=document.getElementById('$CHECKBOX_ID');" +
        "if(!b||b.disabled||b.checked)return 'no';b.click();return 'clicked';})();"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RefundStatusProbe(reloadKey: Int, onStatus: (String, RefundStatus?) -> Unit, cancelUrl: String?, onCancelled: (String) -> Unit) {
    var pageDone by remember { mutableStateOf<CompletableDeferred<Unit>?>(null) }
    var target by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val view = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }
    }
    DisposableEffect(view) {
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView?, url: String?) {
                val wanted = target ?: return
                if (url?.startsWith(wanted) == true) pageDone?.complete(Unit)
            }
        }
        onDispose { view.destroy() }
    }

    suspend fun load(view: WebView, url: String): Boolean {
        target = url
        val done = CompletableDeferred<Unit>()
        pageDone = done
        view.loadUrl(url)
        return withTimeoutOrNull(45_000) { done.await() } != null
    }

    suspend fun script(view: WebView, js: String): String? {
        val result = CompletableDeferred<String?>()
        view.evaluateJavascript(js) { result.complete(it) }
        return withTimeoutOrNull(5_000) { result.await() }
    }

    LaunchedEffect(reloadKey) {
        REFUND_ITEMS.filterNot { it.external }.forEach { item ->
            if (!load(view, item.url)) {
                onStatus(item.url, null)
                return@forEach
            }
            val raw = script(view, PROBE)?.let {
                runCatching { org.json.JSONTokener(it).nextValue() as? String }.getOrNull()
            }
            android.util.Log.i("Refunds", "${item.url} -> $raw")
            onStatus(item.url, raw?.let { runCatching { parseStatus(it) }.getOrNull() })
        }
    }

    LaunchedEffect(cancelUrl) {
        val url = cancelUrl ?: return@LaunchedEffect
        if (!load(view, url)) return@LaunchedEffect
        val clicked = script(view, CANCEL_SCRIPT)?.contains("clicked") == true

        if (clicked) {
            val done = CompletableDeferred<Unit>()
            pageDone = done
            withTimeoutOrNull(20_000) { done.await() }
        }
        onCancelled(url)
    }
}

private fun parseStatus(json: String): RefundStatus {
    val o = JSONObject(json)
    val hasBox = o.optBoolean("hasBox")
    val checked = o.optBoolean("checked")
    val note = o.optString("note").takeIf { it.isNotBlank() && it != "null" }

    val saysCancelled = note?.let {
        it.contains("déjà annul", ignoreCase = true) || it.contains("already cancel", ignoreCase = true)
    } == true
    return RefundStatus(
        canCancel = hasBox && !checked && !o.optBoolean("disabled") && !saysCancelled,
        cancelled = (hasBox && checked) || saysCancelled,
        amount = o.optString("amount").takeIf { it.isNotBlank() && it != "null" },
        window = o.optString("window").takeIf { it.isNotBlank() && it != "null" },
        note = note,
    )
}

@Composable
fun RefundsListContent(onOpen: (RefundItem) -> Unit) {
    val context = LocalContext.current

    var statuses by remember {
        mutableStateOf(
            MemoryCache.get<Map<String, RefundStatus?>>(CACHE_KEY)
                ?: DiskCache.read(context, CACHE_KEY)?.let { runCatching { decodeStatuses(it) }.getOrNull() }
                ?: emptyMap(),
        )
    }
    var reloadKey by remember { mutableStateOf(0) }
    var confirming by remember { mutableStateOf<RefundItem?>(null) }
    var cancelUrl by remember { mutableStateOf<String?>(null) }
    val cancelledMessage = stringResource(R.string.ets_refund_done)

    RefundStatusProbe(
        reloadKey = reloadKey,
        onStatus = { url, status ->

            if (status != null) {
                statuses = statuses + (url to status)
                MemoryCache.put(CACHE_KEY, statuses)
                DiskCache.write(context, CACHE_KEY, encodeStatuses(statuses))
            }
        },
        cancelUrl = cancelUrl,
        onCancelled = {
            cancelUrl = null
            reloadKey++
            AppFeedback.show(cancelledMessage)
        },
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (statuses.isEmpty()) TopLoadingBar()
        app.etsprivate.ui.RoundedGroup(modifier = Modifier.padding(top = 8.dp)) { REFUND_ITEMS.forEach { item ->
            val status = statuses[item.url]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(app.etsprivate.ui.groupColor)
                    .clickable {
                        if (item.external || status?.canCancel != true) onOpen(item) else confirming = item
                    }
                    .padding(horizontal = 16.dp, vertical = Spacing.oneHalf),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(stringResource(item.labelRes), style = MaterialTheme.typography.bodyLarge)
                    val detail = when {
                        item.external -> stringResource(R.string.ets_refund_opens_page)
                        status == null -> null
                        status.cancelled -> stringResource(R.string.ets_refund_already_cancelled)
                        status.canCancel -> listOfNotNull(status.amount, status.window).joinToString(" · ")
                            .ifBlank { stringResource(R.string.ets_refund_can_cancel) }
                        else -> status.note ?: stringResource(R.string.ets_refund_nothing)
                    }
                    detail?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.quarter),
                        )
                    }
                }
                when {
                    status?.settled == true -> Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    item.external -> Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } }
        app.etsprivate.ui.GroupFooter(stringResource(R.string.ets_refunds_explainer))

        app.etsprivate.ui.RoundedGroup(modifier = Modifier.padding(top = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(app.etsprivate.ui.groupColor)
                    .padding(horizontal = 16.dp, vertical = Spacing.oneHalf),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(stringResource(R.string.ets_refund_coop_pass_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.ets_refund_coop_pass),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.quarter),
                    )
                }
            }
        }
    }

    confirming?.let { item ->
        ConfirmDialog(
            title = stringResource(item.labelRes),
            onDismissRequest = { confirming = null },
            confirmLabel = stringResource(R.string.ets_refund_cancel_action),
            onConfirm = {
                cancelUrl = item.url
                confirming = null
            },
            dismissLabel = stringResource(R.string.ets_refund_keep),
        ) {
            Text(stringResource(R.string.ets_refund_confirm_message))
            statuses[item.url]?.amount?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.half),
                )
            }
        }
    }
}

package app.etsprivate.mail

import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import app.etsprivate.ui.bleed
import androidx.compose.ui.draw.clip
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import app.etsprivate.ui.theme.AppShapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Slider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import app.etsprivate.R
import app.etsprivate.auth.OAuthException
import app.etsprivate.auth.MicrosoftOAuth
import app.etsprivate.auth.OAuthWebView
import app.etsprivate.auth.TokenSet
import app.etsprivate.auth.TokenStore
import app.etsprivate.cache.DiskCache
import app.etsprivate.cache.MemoryCache
import app.etsprivate.ui.ZoomableBox
import app.etsprivate.cache.loadCached
import app.etsprivate.cache.mapObjects
import app.etsprivate.cache.putOrBlank
import app.etsprivate.settings.MailSwipeAction
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.settings.Settings
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.CopyableValue
import app.etsprivate.ui.PreparingOverlay
import app.etsprivate.ui.ShareAsImage
import app.etsprivate.ui.AppTopBar
import app.etsprivate.ui.CenteredMessage
import app.etsprivate.ui.FullScreenLoading
import app.etsprivate.ui.InAppBrowser
import app.etsprivate.ui.SignInPrompt
import app.etsprivate.ui.rememberDelayedLoading
import app.etsprivate.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ACCOUNT = "mail"

internal const val FOLDER_KEY = "mail/folder"
internal const val OPEN_UID_KEY = "mail/openUid"

private const val DEFAULT_FOLDER = "INBOX"

private const val MESSAGE_PAGE_SIZE = 25

private val DEFAULT_ROLE_ORDER = listOf(
    MailFolderRole.INBOX, MailFolderRole.SENT, MailFolderRole.DRAFTS,
    MailFolderRole.TRASH, MailFolderRole.JUNK, MailFolderRole.ARCHIVE,
)

internal fun defaultFolderOrder(allFolders: List<MailFolderInfo>): List<String> =
    DEFAULT_ROLE_ORDER.mapNotNull { role -> allFolders.firstOrNull { it.role == role }?.fullName }

@Composable
internal fun folderLabel(folder: MailFolderInfo): String = when (folder.role) {
    MailFolderRole.INBOX -> stringResource(R.string.mail_folder_inbox)
    MailFolderRole.SENT -> stringResource(R.string.mail_folder_sent)
    MailFolderRole.DRAFTS -> stringResource(R.string.mail_folder_drafts)
    MailFolderRole.TRASH -> stringResource(R.string.mail_folder_trash)
    MailFolderRole.JUNK -> stringResource(R.string.mail_folder_junk)
    MailFolderRole.ARCHIVE -> stringResource(R.string.mail_folder_archive)

    MailFolderRole.OTHER -> {
        val resources = LocalContext.current.resources
        folder.fullName.split('/').joinToString(" / ") { segment ->
            KNOWN_FOLDERS[segment.trim().lowercase()]?.first?.let { resources.getString(it) } ?: segment.trim()
        }
    }
}

internal fun folderIcon(folder: MailFolderInfo) = when (folder.role) {
    MailFolderRole.OTHER ->
        KNOWN_FOLDERS[folder.fullName.substringAfterLast('/').lowercase()]?.second
            ?: KNOWN_FOLDERS[folder.fullName.substringBefore('/').lowercase()]?.second
            ?: Icons.Outlined.Folder
    else -> folderIcon(folder.role)
}

internal fun folderIcon(role: MailFolderRole) = when (role) {
    MailFolderRole.INBOX -> Icons.Outlined.Inbox
    MailFolderRole.SENT -> Icons.AutoMirrored.Outlined.Send
    MailFolderRole.DRAFTS -> Icons.Outlined.Drafts
    MailFolderRole.TRASH -> Icons.Outlined.Delete
    MailFolderRole.JUNK -> Icons.Outlined.Report
    MailFolderRole.ARCHIVE -> Icons.Outlined.Archive
    MailFolderRole.OTHER -> Icons.Outlined.Folder
}

private val KNOWN_FOLDERS: Map<String, Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>> = mapOf(
    "calendar" to (R.string.mail_folder_calendar to Icons.Outlined.CalendarMonth),
    "birthdays" to (R.string.mail_folder_birthdays to Icons.Outlined.Cake),
    "united states holidays" to (R.string.mail_folder_holidays to Icons.Outlined.CalendarMonth),
    "contacts" to (R.string.mail_folder_contacts to Icons.Outlined.Contacts),
    "conversation history" to (R.string.mail_folder_conversation_history to Icons.Outlined.Forum),
    "journal" to (R.string.mail_folder_journal to Icons.Outlined.MenuBook),
    "notes" to (R.string.mail_folder_notes to Icons.Outlined.StickyNote2),
    "outbox" to (R.string.mail_folder_outbox to Icons.Outlined.Outbox),
    "tasks" to (R.string.mail_folder_tasks to Icons.Outlined.TaskAlt),
)

internal fun encodeFolderInfos(folders: List<MailFolderInfo>): String =
    JSONArray(folders.map { JSONObject().put("fullName", it.fullName).put("role", it.role.name) }).toString()

internal fun decodeFolderInfos(json: String): List<MailFolderInfo> = JSONArray(json).mapObjects {
    MailFolderInfo(it.getString("fullName"), runCatching { MailFolderRole.valueOf(it.getString("role")) }.getOrDefault(MailFolderRole.OTHER))
}

internal fun encodeMailMessages(messages: List<MailMessage>): String = JSONArray(
    messages.map {
        JSONObject().put("uid", it.uid).put("subject", it.subject).put("from", it.from)
            .put("date", it.date).put("isRead", it.isRead).put("hasAttachment", it.hasAttachment)
            .put("fromAddress", it.fromAddress).put("isStarred", it.isStarred).put("preview", it.preview)
    },
).toString()

internal fun decodeMailMessages(json: String): List<MailMessage> = JSONArray(json).mapObjects {
    MailMessage(
        it.getLong("uid"), it.getString("subject"), it.getString("from"), it.getLong("date"),
        it.optBoolean("isRead", true), it.optBoolean("hasAttachment", false),
        it.optString("fromAddress"), it.optBoolean("isStarred", false), it.optString("preview"),
    )
}

private fun MailAddress.toJson(): JSONObject = JSONObject().put("name", name).put("address", address)
private fun addressFromJson(o: JSONObject) = MailAddress(o.getString("name"), o.getString("address"))

private fun encodeBody(body: MailBody): String {
    val o = JSONObject().put("text", body.text).put("html", body.html ?: "")
    body.from?.let { o.put("from", it.toJson()) }
    o.put("to", JSONArray(body.to.map { it.toJson() }))
    o.put("cc", JSONArray(body.cc.map { it.toJson() }))
    o.put("bcc", JSONArray(body.bcc.map { it.toJson() }))
    o.putOrBlank("messageId", body.messageId)
    o.put(
        "attachments",
        JSONArray(body.attachments.map { JSONObject().put("filename", it.filename).put("mimeType", it.mimeType).put("sizeBytes", it.sizeBytes).put("path", it.path) }),
    )
    val inlineImages = JSONObject()
    body.inlineImages.forEach { (cid, image) -> inlineImages.put(cid, JSONObject().put("mimeType", image.mimeType).put("base64", image.base64)) }
    o.put("inlineImages", inlineImages)
    return o.toString()
}

private fun decodeBody(json: String): MailBody {
    val o = JSONObject(json)
    val inlineImagesObj = o.optJSONObject("inlineImages")
    val inlineImages = inlineImagesObj?.keys()?.asSequence()?.associateWith { cid ->
        val entry = inlineImagesObj.getJSONObject(cid)
        MailInlineImage(entry.getString("mimeType"), entry.getString("base64"))
    }.orEmpty()
    return MailBody(
        text = o.getString("text"),
        html = o.getString("html").ifBlank { null },
        from = o.optJSONObject("from")?.let { addressFromJson(it) },
        to = o.optJSONArray("to")?.mapObjects { addressFromJson(it) }.orEmpty(),
        cc = o.optJSONArray("cc")?.mapObjects { addressFromJson(it) }.orEmpty(),
        bcc = o.optJSONArray("bcc")?.mapObjects { addressFromJson(it) }.orEmpty(),
        attachments = o.optJSONArray("attachments")?.mapObjects {
            MailAttachment(it.getString("filename"), it.getString("mimeType"), it.getLong("sizeBytes"), it.getString("path"))
        }.orEmpty(),
        inlineImages = inlineImages,
        messageId = o.optString("messageId").ifBlank { null },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailScreen(
    openFolder: String? = null,
    openUid: Long? = null,
    onOpenHandled: () -> Unit = {},

    shared: ComposeDraft? = null,
    onSharedHandled: () -> Unit = {},

    visible: Boolean = true,
) {
    run {
        MailScreenBody(openFolder = openFolder, openUid = openUid, onOpenHandled = onOpenHandled, shared = shared, onSharedHandled = onSharedHandled, visible = visible)
    }
}

@Composable
private fun MailScreenBody(
    openFolder: String? = null,
    openUid: Long? = null,
    onOpenHandled: () -> Unit = {},

    shared: ComposeDraft? = null,
    onSharedHandled: () -> Unit = {},

    visible: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tokens by remember { mutableStateOf(TokenStore.load(context, ACCOUNT)) }

    LaunchedEffect(TokenStore.revision) {
        val now = TokenStore.load(context, ACCOUNT)
        if (now?.accessToken != tokens?.accessToken) tokens = now
    }
    var signingIn by remember { mutableStateOf(false) }
    var codeVerifier by remember { mutableStateOf(MicrosoftOAuth.newCodeVerifier()) }
    var status by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf<String?>(null) }

    var searchFrom by remember { mutableStateOf<Pair<String, String>?>(null) }
    var searchAttachment by remember { mutableStateOf(false) }
    var searchUnread by remember { mutableStateOf(false) }
    var searchStarred by remember { mutableStateOf(false) }
    var searchDays by remember { mutableIntStateOf(0) }
    var searchRelevant by remember { mutableStateOf(false) }

    var serverResults by remember { mutableStateOf<List<MailMessage>?>(null) }
    var searchingServer by remember { mutableStateOf(false) }
    var confirmingEmpty by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery == null) {
        if (searchQuery == null) {
            searchFrom = null; searchAttachment = false; searchUnread = false; searchStarred = false
            searchDays = 0; searchRelevant = false; serverResults = null
        }
    }
    var loading by remember { mutableStateOf(false) }

    var folderOrder by remember { mutableStateOf(Settings.getMailFolderOrder(context).orEmpty()) }

    var currentFolder by remember { mutableStateOf(MemoryCache.get<String>(FOLDER_KEY) ?: DEFAULT_FOLDER) }

    var selectedMessage by remember {
        mutableStateOf(
            MemoryCache.get<Long>(OPEN_UID_KEY)?.let { uid ->
                val account = TokenStore.load(context, ACCOUNT)?.email.orEmpty()
                MemoryCache.get<List<MailMessage>>("mail/messages/$account/$currentFolder")
                    ?.firstOrNull { it.uid == uid }
            },
        )
    }

    var restoredOpenMessage by remember { mutableStateOf(false) }
    LaunchedEffect(currentFolder) { MemoryCache.put(FOLDER_KEY, currentFolder) }
    LaunchedEffect(selectedMessage?.uid, restoredOpenMessage) {
        if (!restoredOpenMessage) return@LaunchedEffect
        MemoryCache.put(OPEN_UID_KEY, selectedMessage?.uid)
    }

    var pendingOpenUid by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(openUid, openFolder) {
        val uid = openUid ?: return@LaunchedEffect
        openFolder?.let { if (it != currentFolder) currentFolder = it }
        pendingOpenUid = uid
        onOpenHandled()
    }
    var composeDraft by remember { mutableStateOf<ComposeDraft?>(null) }
    LaunchedEffect(shared) {
        val draft = shared ?: return@LaunchedEffect
        composeDraft = draft
        onSharedHandled()
    }

    val listState = rememberLazyListState()

    var highlightedUids by remember { mutableStateOf(setOf<Long>()) }

    var noOlder by remember(currentFolder) { mutableStateOf(false) }
    var unreadCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var loadingMore by remember(currentFolder) { mutableStateOf(false) }
    var selectedUids by remember { mutableStateOf(setOf<Long>()) }

    var busyUids by remember { mutableStateOf(setOf<Long>()) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val swipeEnabled = remember { Settings.getMailSwipeEnabled(context) }
    val leftSwipeAction = remember { Settings.getMailSwipeLeft(context) }
    val rightSwipeAction = remember { Settings.getMailSwipeRight(context) }

    val tokenExchangeFailedMessage = stringResource(R.string.mail_error_token_exchange)
    val sessionExpiredMessage = stringResource(R.string.error_session_expired)
    val loadMessagesFailedMessage = stringResource(R.string.mail_error_load_messages)
    val actionFailedMessage = stringResource(R.string.mail_error_action_failed)

    if (signingIn) {
        OAuthWebView(
            authorizeUrl = MicrosoftOAuth.authorizeUrl(MicrosoftOAuth.MAIL, codeVerifier),
            redirectPrefix = MicrosoftOAuth.MAIL.redirectPrefix,
            onCode = { code ->
                signingIn = false
                loading = true
                scope.launch {
                    try {
                        val result = MicrosoftOAuth.exchangeCode(MicrosoftOAuth.MAIL, code, codeVerifier)
                        TokenStore.save(context, ACCOUNT, result)
                        tokens = result
                    } catch (e: Exception) {
                        status = tokenExchangeFailedMessage
                    } finally {
                        loading = false
                        codeVerifier = MicrosoftOAuth.newCodeVerifier()
                    }
                }
            },
            onError = { message ->
                signingIn = false
                status = message
                codeVerifier = MicrosoftOAuth.newCodeVerifier()
            },
            onCancel = { signingIn = false },
        )
        return
    }

    val currentTokens = tokens
    if (currentTokens == null) {
        SignInPrompt(
            message = stringResource(R.string.not_signed_in_mail),
            buttonLabel = stringResource(R.string.sign_in),
            onSignIn = { status = null; signingIn = true },
            status = status,
            loading = loading,
        )
        return
    }
    val email = currentTokens.email ?: ""

    var allFolders by remember { mutableStateOf(MemoryCache.get<List<MailFolderInfo>>("mail/folders/$email").orEmpty()) }

    var messages by remember(currentFolder) { mutableStateOf(MemoryCache.get<List<MailMessage>>("mail/messages/$email/$currentFolder").orEmpty()) }

    LaunchedEffect(messages) {
        if (restoredOpenMessage || messages.isEmpty()) return@LaunchedEffect
        if (selectedMessage != null) {
            restoredOpenMessage = true
            return@LaunchedEffect
        }
        val uid = MemoryCache.get<Long>(OPEN_UID_KEY)
        if (uid != null) selectedMessage = messages.firstOrNull { it.uid == uid }
        restoredOpenMessage = true
    }

    val isTrashFolder = allFolders.firstOrNull { it.fullName == currentFolder }?.role == MailFolderRole.TRASH

    val isDraftsFolder = allFolders.firstOrNull { it.fullName == currentFolder }?.role == MailFolderRole.DRAFTS
    var pendingPermanentDelete by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val deletedMessage = stringResource(R.string.mail_action_deleted)
    val deletedPermanentlyMessage = stringResource(R.string.mail_action_deleted_permanently)
    val archivedMessage = stringResource(R.string.mail_action_archived)
    val movedMessage = stringResource(R.string.mail_action_moved)
    val markedReadMessage = stringResource(R.string.mail_action_marked_read)
    val markedUnreadMessage = stringResource(R.string.mail_action_marked_unread)
    val undoLabel = stringResource(R.string.action_undo)

    suspend fun handleAuthFailure() {
        val refresh = currentTokens.refreshToken
        if (refresh == null) {
            status = sessionExpiredMessage
            TokenStore.clear(context, ACCOUNT)
            tokens = null
            return
        }
        try {
            val refreshed = MicrosoftOAuth.refresh(MicrosoftOAuth.MAIL, refresh)
            val merged = refreshed.copy(email = refreshed.email ?: currentTokens.email)
            TokenStore.save(context, ACCOUNT, merged)
            tokens = merged
        } catch (refreshError: Exception) {
            android.util.Log.w("MailFail", "refresh failed", refreshError)
            if (refreshError is OAuthException && refreshError.requiresReauth) {
                status = sessionExpiredMessage
                TokenStore.clear(context, ACCOUNT)
                tokens = null
            } else {
                status = loadMessagesFailedMessage
            }
        }
    }

    LaunchedEffect(currentTokens) {
        try {
            loadCached(
                context = context,
                key = "mail/folders/$email",
                decode = ::decodeFolderInfos,
                encode = ::encodeFolderInfos,
                fetch = { withContext(Dispatchers.IO) { OutlookImap.listFolders(email, currentTokens.accessToken) } },
                render = { list ->
                    allFolders = list
                    val savedOrder = Settings.getMailFolderOrder(context)
                    val order = if (savedOrder != null) {
                        savedOrder.filter { name -> list.any { it.fullName == name } }
                    } else {
                        defaultFolderOrder(list).also { Settings.setMailFolderOrder(context, it) }
                    }
                    folderOrder = order
                    if (currentFolder !in list.map { it.fullName }) {
                        currentFolder = list.firstOrNull { it.role == MailFolderRole.INBOX }?.fullName
                            ?: order.firstOrNull() ?: list.firstOrNull()?.fullName ?: DEFAULT_FOLDER
                    }
                },
            )
        } catch (e: OutlookImap.AuthException) {
            handleAuthFailure()
        } catch (e: Exception) {
        }
    }

    suspend fun fillPreviews() {
        val folderNow = currentFolder
        val missing = messages.filter { it.preview.isEmpty() }.map { it.uid }.take(50)
        if (missing.isEmpty()) return
        val found = runCatching {
            withContext(Dispatchers.IO) { OutlookImap.previewsFor(email, currentTokens.accessToken, folderNow, missing) }
        }.getOrNull() ?: return
        if (folderNow != currentFolder || found.isEmpty()) return
        val list = messages.map { m -> found[m.uid]?.let { m.copy(preview = it.ifEmpty { " " }) } ?: m }
        messages = list
        val key = "mail/messages/$email/$folderNow"
        MemoryCache.put(key, list)
        withContext(Dispatchers.IO) { DiskCache.write(context, key, encodeMailMessages(list.take(Settings.getMailKeep(context)))) }
    }

    suspend fun refreshMessages(background: Boolean = false, attempt: Int = 0): Boolean {
        try {
            loadCached(
                context = context,
                key = "mail/messages/$email/$currentFolder",
                decode = ::decodeMailMessages,

                encode = { encodeMailMessages(it.take(Settings.getMailKeep(context))) },

                fetch = {
                    val fresh = withContext(Dispatchers.IO) {
                        OutlookImap.fetchFolder(email, currentTokens.accessToken, currentFolder, MESSAGE_PAGE_SIZE, withPreviews = false)
                    }
                    val freshUids = fresh.map { it.uid }.toSet()
                    val oldestFresh = fresh.minOfOrNull { it.date } ?: Long.MAX_VALUE
                    val previews = messages.associate { it.uid to it.preview }
                    fresh.map { if (it.preview.isEmpty()) it.copy(preview = previews[it.uid].orEmpty()) else it } +
                        messages.filter { it.uid !in freshUids && it.date < oldestFresh }
                },
                render = { messages = it },
            )
            fillPreviews()
            return true
        } catch (e: OutlookImap.AuthException) {
            handleAuthFailure()
            return false
        } catch (e: Exception) {
            android.util.Log.w("MailFail", "load failed (attempt $attempt)", e)

            if (attempt < 2) {
                kotlinx.coroutines.delay(1_500)
                return refreshMessages(background, attempt + 1)
            }
            if (background) AppFeedback.show(loadMessagesFailedMessage) else status = loadMessagesFailedMessage
            return false
        }
    }

    LaunchedEffect(currentFolder) { listState.scrollToItem(0) }

    var topUid by remember(currentFolder) { mutableStateOf(0L) }
    LaunchedEffect(messages, currentFolder) { topUid = messages.maxOfOrNull { it.uid } ?: topUid }

    suspend fun refreshUnread() {
        val names = (folderOrder + allFolders.map { it.fullName }).distinct().ifEmpty { return }
        unreadCounts = runCatching {
            withContext(Dispatchers.IO) { OutlookImap.unreadCounts(email, currentTokens.accessToken, names) }
        }.getOrDefault(unreadCounts)
    }

    LaunchedEffect(currentTokens, currentFolder) {
        loading = true
        status = null
        refreshMessages()
        loading = false
        refreshUnread()
    }
    LaunchedEffect(currentTokens, folderOrder) { refreshUnread() }

    LaunchedEffect(listState, currentFolder) {
        var failedAt: Pair<Int, Int>? = null
        snapshotFlow {
            Triple(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1, messages.size, loading || loadingMore)
        }
            .collect { (lastVisible, loaded, busy) ->
                val failedHere = failedAt == (lastVisible to loaded)
                if (!noOlder && !busy && !failedHere && loaded > 0 && lastVisible >= loaded - 5) {
                    loadingMore = true

                    val ok = try {
                        val oldestUid = messages.minOf { it.uid }
                        val older = withContext(Dispatchers.IO) {
                            OutlookImap.fetchOlder(email, currentTokens.accessToken, currentFolder, oldestUid, MESSAGE_PAGE_SIZE)
                        }
                        if (older.isEmpty()) {
                            noOlder = true
                        } else {
                            val list = (messages + older).distinctBy { it.uid }
                            messages = list
                            val key = "mail/messages/$email/$currentFolder"
                            MemoryCache.put(key, list)
                            withContext(Dispatchers.IO) { DiskCache.write(context, key, encodeMailMessages(list.take(Settings.getMailKeep(context)))) }
                        }
                        fillPreviews()
                        true
                    } catch (e: OutlookImap.AuthException) {
                        handleAuthFailure()
                        false
                    } catch (e: Exception) {
                        false
                    }

                    failedAt = if (ok) null else lastVisible to loaded
                    loadingMore = false
                }
            }
    }

    val pollInterval = remember { Settings.getMailPollInterval(context) }
    LaunchedEffect(currentTokens, currentFolder, pollInterval) {
        if (pollInterval.minutes <= 0) return@LaunchedEffect
        while (true) {
            delay(pollInterval.minutes * 60_000L)
            refreshMessages(background = true)
        }
    }

    fun persistMessages(newList: List<MailMessage>) {
        messages = newList
        val key = "mail/messages/$email/$currentFolder"

        MemoryCache.put(key, newList)
        scope.launch { withContext(Dispatchers.IO) { DiskCache.write(context, key, encodeMailMessages(newList.take(Settings.getMailKeep(context)))) } }
    }

    fun moveBackFrom(uidsInTarget: List<Long>, targetFolder: String, originalFolder: String, restore: List<MailMessage> = emptyList()) {
        if (uidsInTarget.isEmpty()) return
        val token = currentTokens.accessToken
        scope.launch {
            if (currentFolder == originalFolder && restore.isNotEmpty()) {
                val present = messages.map { it.uid }.toSet()
                val readded = restore.filterNot { it.uid in present }
                if (readded.isNotEmpty()) {
                    val onScreenNow = listState.layoutInfo.visibleItemsInfo
                    val firstVisible = onScreenNow.firstOrNull()?.index ?: 0
                    val lastVisible = onScreenNow.lastOrNull()?.index ?: 0

                    persistMessages((messages + readded).sortedByDescending { it.date })
                    highlightedUids = readded.map { it.uid }.toSet()
                    val at = messages.indexOfFirst { it.uid in highlightedUids }

                    if (at >= 0 && at !in (firstVisible + 1) until lastVisible) {
                        listState.animateScrollToItem(at)
                    }

                    launch {
                        delay(1100)
                        highlightedUids = emptySet()
                    }
                }
            }
            try {
                withContext(Dispatchers.IO) { OutlookImap.moveMessages(email, token, targetFolder, uidsInTarget, originalFolder) }
                if (currentFolder == originalFolder) {
                    refreshMessages(background = true)
                }
            } catch (e: OutlookImap.AuthException) {
                handleAuthFailure()
            } catch (e: Exception) {
                AppFeedback.show(actionFailedMessage)
            }
        }
    }

    fun toggleStar(message: MailMessage) {
        val starred = !message.isStarred
        persistMessages(messages.map { if (it.uid == message.uid) it.copy(isStarred = starred) else it })
        serverResults = serverResults?.map { if (it.uid == message.uid) it.copy(isStarred = starred) else it }
        if (selectedMessage?.uid == message.uid) selectedMessage = selectedMessage?.copy(isStarred = starred)
        val folderNow = currentFolder
        scope.launch {
            try {
                withContext(Dispatchers.IO) { OutlookImap.setStarred(email, currentTokens.accessToken, folderNow, listOf(message.uid), starred) }
            } catch (e: OutlookImap.AuthException) {
                handleAuthFailure()
            } catch (e: Exception) {
                AppFeedback.show(actionFailedMessage)
            }
        }
    }

    LaunchedEffect(searchQuery?.trim(), currentFolder, searchFrom) {
        serverResults = null
        val typed = searchQuery?.trim().orEmpty()

        val q = if (typed.length >= 2) typed else searchFrom?.second?.takeIf { it.isNotBlank() } ?: searchFrom?.first.orEmpty()
        if (searchQuery == null || q.length < 2) return@LaunchedEffect
        delay(350)
        searchingServer = true
        try {
            serverResults = withContext(Dispatchers.IO) { OutlookImap.search(email, currentTokens.accessToken, currentFolder, q, inText = false) }
            if (typed.length >= 2) {
                val inText = withContext(Dispatchers.IO) { OutlookImap.search(email, currentTokens.accessToken, currentFolder, q) }
                serverResults = (serverResults.orEmpty() + inText).distinctBy { it.uid }
            }
        } catch (e: OutlookImap.AuthException) {
            handleAuthFailure()
        } catch (e: Exception) {
        } finally {
            searchingServer = false
        }
    }

    fun searchShown(): List<MailMessage> {
        val q = searchQuery?.trim().orEmpty()
        val needle = q.lowercase()
        val base = if (needle.isEmpty()) {
            messages
        } else {
            val local = messages.filter { it.subject.lowercase().contains(needle) || it.from.lowercase().contains(needle) }
            (serverResults.orEmpty() + local).distinctBy { it.uid }
        }
        val since = if (searchDays == 0) 0L else System.currentTimeMillis() - searchDays * 86_400_000L
        val filtered = base.filter { m ->
            fromMatches(m, searchFrom) && (!searchAttachment || m.hasAttachment) &&
                (!searchUnread || !m.isRead) && (!searchStarred || m.isStarred) && m.date >= since
        }
        return if (searchRelevant && needle.isNotEmpty()) {
            filtered.sortedWith(
                compareByDescending<MailMessage> {
                    (if (it.subject.lowercase().contains(needle)) 2 else 0) + (if (it.from.lowercase().contains(needle)) 1 else 0)
                }.thenByDescending { it.date },
            )
        } else {
            filtered.sortedByDescending { it.date }
        }
    }

    fun applyReadState(uids: Set<Long>, read: Boolean, folder: String) {
        if (folder == currentFolder) {
            persistMessages(messages.map { if (it.uid in uids) it.copy(isRead = read) else it })
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { OutlookImap.setReadMessages(email, currentTokens.accessToken, folder, uids.toList(), read) }
                refreshUnread()
            } catch (e: OutlookImap.AuthException) {
                handleAuthFailure()
            } catch (e: Exception) {
                AppFeedback.show(actionFailedMessage)
            }
        }
    }

    LaunchedEffect(pendingOpenUid, messages) {
        val uid = pendingOpenUid ?: return@LaunchedEffect
        val message = messages.firstOrNull { it.uid == uid } ?: return@LaunchedEffect
        selectedMessage = message
        pendingOpenUid = null

        if (!message.isRead) applyReadState(setOf(message.uid), true, currentFolder)
    }

    fun performAction(action: MailSwipeAction, message: MailMessage) {
        if (action == MailSwipeAction.NONE) return
        if (action == MailSwipeAction.DELETE && isTrashFolder) {
            pendingPermanentDelete = setOf(message.uid)
            return
        }
        val originalFolder = currentFolder

        if (action == MailSwipeAction.TOGGLE_READ) {
            persistMessages(messages.map { if (it.uid == message.uid) it.copy(isRead = !message.isRead) else it })
        }
        val feedback = when (action) {
            MailSwipeAction.TOGGLE_READ -> if (message.isRead) markedUnreadMessage else markedReadMessage
            MailSwipeAction.ARCHIVE -> archivedMessage
            MailSwipeAction.DELETE -> deletedMessage
            MailSwipeAction.NONE -> null
        }
        when (action) {
            MailSwipeAction.TOGGLE_READ -> {
                feedback?.let { AppFeedback.show(it, undoLabel) { applyReadState(setOf(message.uid), message.isRead, originalFolder) } }
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { OutlookImap.setRead(email, currentTokens.accessToken, originalFolder, message.uid, !message.isRead) }
                    } catch (e: OutlookImap.AuthException) {
                        handleAuthFailure()
                    } catch (e: Exception) {
                        AppFeedback.show(actionFailedMessage)
                    }
                }
            }

            MailSwipeAction.ARCHIVE, MailSwipeAction.DELETE -> {
                val token = currentTokens.accessToken
                val isArchive = action == MailSwipeAction.ARCHIVE
                val role = if (isArchive) MailFolderRole.ARCHIVE else MailFolderRole.TRASH
                scope.launch {
                    busyUids = busyUids + message.uid
                    try {
                        val target = allFolders.firstOrNull { it.role == role }?.fullName
                            ?: runCatching { withContext(Dispatchers.IO) { OutlookImap.folderNameForRole(email, token, role) } }.getOrNull()
                        val moved = withContext(Dispatchers.IO) {
                            if (isArchive) {
                                OutlookImap.archiveMessage(email, token, originalFolder, message.uid)
                            } else {
                                OutlookImap.deleteMessage(email, token, originalFolder, message.uid)
                            }
                        }
                        if (currentFolder == originalFolder) {
                            persistMessages(messages.filterNot { it.uid == message.uid })
                        }
                        val landedOn = moved.values.toList()
                        feedback?.let {
                            if (target != null && landedOn.isNotEmpty()) {
                                AppFeedback.show(it, undoLabel) { moveBackFrom(landedOn, target, originalFolder, listOf(message)) }
                            } else {
                                AppFeedback.show(it)
                            }
                        }
                    } catch (e: OutlookImap.AuthException) {
                        handleAuthFailure()
                    } catch (e: Exception) {
                        AppFeedback.show(actionFailedMessage)
                    } finally {
                        busyUids = busyUids - message.uid
                    }
                }
            }
            MailSwipeAction.NONE -> Unit
        }
    }

    fun performBulkAction(uids: Set<Long>, feedback: String, targetFolder: String?, imapCall: suspend () -> Map<Long, Long>) {
        val originalFolder = currentFolder

        val removed = messages.filter { it.uid in uids }
        selectedUids = emptySet()
        scope.launch {
            busyUids = busyUids + uids
            try {
                val moved = withContext(Dispatchers.IO) { imapCall() }
                if (currentFolder == originalFolder) {
                    persistMessages(messages.filterNot { it.uid in uids })
                }
                val landedOn = moved.values.toList()
                if (targetFolder != null && landedOn.isNotEmpty()) {
                    AppFeedback.show(feedback, undoLabel) { moveBackFrom(landedOn, targetFolder, originalFolder, removed) }
                } else {
                    AppFeedback.show(feedback)
                }
            } catch (e: OutlookImap.AuthException) {
                handleAuthFailure()
            } catch (e: Exception) {
                AppFeedback.show(actionFailedMessage)
            } finally {
                busyUids = busyUids - uids
            }
        }
    }

    fun performPermanentDelete(uids: Set<Long>) {
        pendingPermanentDelete = emptySet()
        selectedUids = emptySet()
        scope.launch {
            busyUids = busyUids + uids
            try {
                withContext(Dispatchers.IO) { OutlookImap.deleteMessages(email, currentTokens.accessToken, currentFolder, uids.toList()) }
                persistMessages(messages.filterNot { it.uid in uids })
                AppFeedback.show(deletedPermanentlyMessage)
            } catch (e: OutlookImap.AuthException) {
                handleAuthFailure()
            } catch (e: Exception) {
                AppFeedback.show(actionFailedMessage)
            } finally {
                busyUids = busyUids - uids
            }
        }
    }

    fun bulkMarkRead(read: Boolean) {
        val uids = selectedUids
        val originalFolder = currentFolder
        applyReadState(uids, read, originalFolder)
        selectedUids = emptySet()
        AppFeedback.show(if (read) markedReadMessage else markedUnreadMessage, undoLabel) { applyReadState(uids, !read, originalFolder) }
    }

    LaunchedEffect(composeDraft != null) {
        android.util.Log.i("MailStart", "compose=${composeDraft != null} folder=$currentFolder open=${selectedMessage?.uid}")
    }
    composeDraft?.let { draft ->

        fun closeCompose() {
            composeDraft = null
            if (draft.editingUid != null) selectedMessage = null
        }
        ComposeMailScreen(
            fromEmail = email,
            accessToken = currentTokens.accessToken,
            draft = draft,
            onBack = ::closeCompose,
            onSent = ::closeCompose,
        )
        return
    }

    remember { MailClock.mark("MAILSCREEN CREATED") }
    var lastOpened by remember { mutableStateOf<MailMessage?>(null) }

    if (selectedMessage != null && selectedMessage != lastOpened) lastOpened = selectedMessage
    LaunchedEffect(selectedMessage?.uid) {
        android.util.Log.i("MailDbg", "--- selected=${selectedMessage?.uid} ---")
    }

    Box(modifier = Modifier.fillMaxSize()) {
    lastOpened?.let { message ->
        MessageDetailScreen(
            email = email,
            accessToken = currentTokens.accessToken,
            folder = currentFolder,
            message = message,
            moveTargets = folderOrder.filter { it != currentFolder }.map { name ->
                allFolders.firstOrNull { it.fullName == name } ?: MailFolderInfo(name, MailFolderRole.OTHER)
            },
            isTrashFolder = isTrashFolder,
            isDraftsFolder = isDraftsFolder,
            visible = visible && selectedMessage?.uid == message.uid,
            onBack = { selectedMessage = null },
            onCompose = { draft -> composeDraft = draft },
            onMoveToFolder = { target ->
                val uid = message.uid
                selectedMessage = null
                performBulkAction(setOf(uid), movedMessage, target.fullName) {
                    OutlookImap.moveMessages(email, currentTokens.accessToken, currentFolder, listOf(uid), target.fullName)
                }
            },

            onDelete = { selectedMessage = null; performAction(MailSwipeAction.DELETE, message) },
            onArchive = if (allFolders.firstOrNull { it.fullName == currentFolder }?.role == MailFolderRole.ARCHIVE) null else {
                { selectedMessage = null; performAction(MailSwipeAction.ARCHIVE, message) }
            },

            onMarkUnread = {
                selectedMessage = null
                val now = messages.firstOrNull { it.uid == message.uid } ?: message
                if (now.isRead) performAction(MailSwipeAction.TOGGLE_READ, now)
            },
            folderName = allFolders.firstOrNull { it.fullName == currentFolder }?.let { folderLabel(it) } ?: currentFolder,
            onToggleStar = { toggleStar(messages.firstOrNull { it.uid == message.uid } ?: message) },
        )
    }

    if (selectedMessage == null) {
    if (confirmingEmpty) {
        val junk = allFolders.firstOrNull { it.fullName == currentFolder }?.role == MailFolderRole.JUNK
        ConfirmDialog(
            title = stringResource(if (junk) R.string.mail_empty_junk_title else R.string.mail_empty_trash_title),
            onDismissRequest = { confirmingEmpty = false },
            confirmLabel = stringResource(R.string.mail_empty_action),
            onConfirm = {
                confirmingEmpty = false
                val folderNow = currentFolder
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { OutlookImap.emptyFolder(email, currentTokens.accessToken, folderNow) }
                        if (folderNow == currentFolder) {
                            messages = emptyList()
                            val key = "mail/messages/$email/$folderNow"
                            MemoryCache.put(key, emptyList<MailMessage>())
                            withContext(Dispatchers.IO) { DiskCache.write(context, key, encodeMailMessages(emptyList())) }
                        }

                        unreadCounts = unreadCounts + (folderNow to 0)
                    } catch (e: OutlookImap.AuthException) {
                        handleAuthFailure()
                    } catch (e: Exception) {
                        AppFeedback.show(actionFailedMessage)
                    }
                }
            },
            dismissLabel = stringResource(R.string.action_cancel),
        )
    }

    if (pendingPermanentDelete.isNotEmpty()) {
        val uids = pendingPermanentDelete
        ConfirmDialog(
            title = stringResource(R.string.mail_delete_forever_title),
            onDismissRequest = { pendingPermanentDelete = emptySet() },
            confirmLabel = stringResource(R.string.action_delete_forever),
            onConfirm = { performPermanentDelete(uids) },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            Text(pluralStringResource(R.plurals.mail_delete_forever_message, uids.size, uids.size))
        }
    }

    if (showMoveDialog) {
        MoveToFolderDialog(
            folders = folderOrder.filter { it != currentFolder }.map { name ->
                allFolders.firstOrNull { it.fullName == name } ?: MailFolderInfo(name, MailFolderRole.OTHER)
            },
            onDismiss = { showMoveDialog = false },
            onSelect = { target ->
                showMoveDialog = false
                val uids = selectedUids
                performBulkAction(uids, movedMessage, target.fullName) {
                    OutlookImap.moveMessages(email, currentTokens.accessToken, currentFolder, uids.toList(), target.fullName)
                }
            },
        )
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = selectedUids.isNotEmpty()) { selectedUids = emptySet() }

    BackHandler(enabled = searchQuery != null) { searchQuery = null }

    val statusBarPx = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(androidx.compose.ui.platform.LocalDensity.current)
    ModalNavigationDrawer(
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(
                constraints.copy(
                    minHeight = constraints.minHeight + statusBarPx,
                    maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight + statusBarPx else constraints.maxHeight,
                ),
            )
            layout(placeable.width, placeable.height - statusBarPx) { placeable.place(0, -statusBarPx) }
        },
        drawerState = drawerState,
        drawerContent = {
            FolderDrawerContent(
                unread = unreadCounts,
                allFolders = allFolders,
                folderOrder = folderOrder,
                currentFolder = currentFolder,
                onSelectFolder = { name ->

                    loading = true
                    currentFolder = name
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(top = with(androidx.compose.ui.platform.LocalDensity.current) { statusBarPx.toDp() })) {
        Column(modifier = Modifier.fillMaxSize().background(app.etsprivate.ui.pageColor)) {
            val currentFolderInfo = allFolders.firstOrNull { it.fullName == currentFolder }
            if (selectedUids.isNotEmpty()) {
                SelectionActionBar(
                    count = selectedUids.size,
                    onClose = { selectedUids = emptySet() },
                    onMarkRead = { bulkMarkRead(true) },
                    onMarkUnread = { bulkMarkRead(false) },
                    onArchive = {
                        val uids = selectedUids
                        val token = currentTokens.accessToken
                        val folderNow = currentFolder
                        performBulkAction(uids, archivedMessage, allFolders.firstOrNull { it.role == MailFolderRole.ARCHIVE }?.fullName) { OutlookImap.archiveMessages(email, token, folderNow, uids.toList()) }
                    },
                    onDelete = {
                        val uids = selectedUids
                        if (isTrashFolder) {
                            pendingPermanentDelete = uids
                        } else {
                            val token = currentTokens.accessToken
                            val folderNow = currentFolder
                            performBulkAction(uids, deletedMessage, allFolders.firstOrNull { it.role == MailFolderRole.TRASH }?.fullName) { OutlookImap.deleteMessages(email, token, folderNow, uids.toList()) }
                        }
                    },
                    onMove = { showMoveDialog = true },
                )
            } else {
                FolderHeader(
                    menuDot = unreadCounts.values.any { it > 0 },
                    title = currentFolderInfo?.let { folderLabel(it) } ?: currentFolder,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                )
                if (searchQuery != null) {
                    SearchFilters(
                        senders = remember(messages, serverResults) {
                            val seen = (serverResults.orEmpty() + messages).filter { it.from.isNotBlank() || it.fromAddress.isNotBlank() }
                            val counts = seen.groupingBy { it.fromAddress.lowercase().ifBlank { it.from } }.eachCount()
                            val fromMail = seen.map { it.from to it.fromAddress }
                            val written = MailContacts.all(context).map { it.name to it.address }
                            (fromMail + written)
                                .distinctBy { it.second.lowercase().ifBlank { it.first } }
                                .sortedWith(compareByDescending<Pair<String, String>> { counts[it.second.lowercase().ifBlank { it.first }] ?: 0 }.thenBy { it.first.ifBlank { it.second }.lowercase() })
                        },
                        sortable = !searchQuery.isNullOrBlank(),
                        from = searchFrom, onFrom = { searchFrom = it },
                        attachment = searchAttachment, onAttachment = { searchAttachment = it },
                        unread = searchUnread, onUnread = { searchUnread = it },
                        starred = searchStarred, onStarred = { searchStarred = it },
                        days = searchDays, onDays = { searchDays = it },
                        relevant = searchRelevant, onRelevant = { searchRelevant = it },
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (searchingServer) {
                            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            val currentStatus = status
            val showListLoading = rememberDelayedLoading(loading && messages.isEmpty())
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        val before = topUid
                        isRefreshing = true
                        refreshMessages(background = messages.isNotEmpty())
                        isRefreshing = false
                        if ((messages.maxOfOrNull { it.uid } ?: 0L) > before) listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when {
                    loading && messages.isEmpty() -> { if (showListLoading) FullScreenLoading() }
                    currentStatus != null -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(currentStatus, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                        Button(onClick = { scope.launch { refreshMessages() } }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }

                    messages.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.mail_folder_empty), style = MaterialTheme.typography.bodyLarge)
                    }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                    ) {
                        val shown = (if (searchQuery == null) messages else searchShown()).distinctBy { it.uid }

                        val emptiable = currentFolderInfo?.role.let { it == MailFolderRole.TRASH || it == MailFolderRole.JUNK }
                        if (emptiable && searchQuery == null && messages.isNotEmpty()) {
                            item(key = "empty-folder") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = app.etsprivate.ui.GroupMargin, end = app.etsprivate.ui.GroupMargin, bottom = 12.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(app.etsprivate.ui.groupColor)
                                        .clickable { confirmingEmpty = true }
                                        .heightIn(min = 56.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteForever,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        stringResource(
                                            if (currentFolderInfo?.role == MailFolderRole.JUNK) R.string.mail_empty_junk else R.string.mail_empty_trash,
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 16.dp),
                                    )
                                }
                            }
                        }
                        itemsIndexed(shown, key = { _, it -> it.uid }) { index, message ->

                            MessageRow(
                                modifier = Modifier
                                    .padding(horizontal = app.etsprivate.ui.GroupMargin)
                                    .padding(bottom = app.etsprivate.ui.GroupGap)
                                    .clip(app.etsprivate.ui.groupShape(index, shown.size)),
                                message = message,
                                swipeEnabled = swipeEnabled && selectedUids.isEmpty(),
                                leftAction = leftSwipeAction,
                                rightAction = rightSwipeAction,
                                confirmDeleteFirst = isTrashFolder,
                                selected = message.uid in selectedUids,
                                selectionMode = selectedUids.isNotEmpty(),
                                highlighted = message.uid in highlightedUids,
                                busy = message.uid in busyUids,
                                onClick = {
                                    if (selectedUids.isNotEmpty()) {
                                        selectedUids = if (message.uid in selectedUids) selectedUids - message.uid else selectedUids + message.uid
                                    } else {
                                        selectedMessage = message

                                        if (!message.isRead) applyReadState(setOf(message.uid), true, currentFolder)
                                    }
                                },
                                onLongClick = { selectedUids = selectedUids + message.uid },
                                onAction = { action -> performAction(action, message) },
                                onStar = { toggleStar(message) },
                                highlight = searchQuery?.trim()?.takeIf { it.isNotEmpty() },
                            )
                        }
                        if (loadingMore) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.double),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (selectedUids.isEmpty()) {
            val toastShowing = AppFeedback.hostState.currentSnackbarData != null
            val fabLift by animateDpAsState(if (toastShowing) 72.dp else 0.dp, label = "mail-fab-lift")

            val atTop by remember { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex == 0 } }
            androidx.compose.material3.ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.mail_compose_new)) },
                icon = { Icon(GmailIcons.Edit, contentDescription = null) },
                expanded = atTop,
                onClick = { composeDraft = ComposeDraft() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).offset(y = -fabLift),
            )
        }
        }
    }
    }
}
}

@Composable
private fun FolderDrawerContent(unread: Map<String, Int>, allFolders: List<MailFolderInfo>, folderOrder: List<String>, currentFolder: String, onSelectFolder: (String) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        windowInsets = androidx.compose.foundation.layout.WindowInsets.statusBars,
    ) {
        Text(
            stringResource(R.string.tab_mail),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 12.dp),
        )
        folderOrder.forEach { name ->
            val info = allFolders.firstOrNull { it.fullName == name } ?: MailFolderInfo(name, MailFolderRole.OTHER)
            NavigationDrawerItem(
                label = { Text(folderLabel(info)) },
                icon = { Icon(folderIcon(info), contentDescription = null) },

                badge = { if ((unread[name] ?: 0) > 0) UnreadDot() },
                selected = name == currentFolder,
                onClick = { onSelectFolder(name) },
                modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

@Composable
private fun FolderHeader(
    menuDot: Boolean,
    title: String,
    onOpenDrawer: () -> Unit,
    query: String?,
    onQueryChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    val searching = query != null
    val focus = remember { FocusRequester() }
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }
    Column(modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.pageColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = if (searching) 16.dp else 4.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!searching) {
                IconButton(onClick = onOpenDrawer) {
                    Box {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.mail_open_folders))
                        if (menuDot) UnreadDot(Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = 2.dp))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (searching) 0.dp else Spacing.half, end = if (searching) 0.dp else Spacing.oneHalf)
                    .height(48.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(app.etsprivate.ui.groupColor)
                    .then(if (searching) Modifier else Modifier.clickable { onQueryChange("") })
                    .padding(horizontal = Spacing.half),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (searching) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    )
                    Box(modifier = Modifier.weight(1f).padding(end = Spacing.half)) {
                        BasicTextField(
                            value = query.orEmpty(),
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                        if (query.isNullOrEmpty()) {
                            Text(
                                stringResource(R.string.mail_search_in),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }

                    IconButton(onClick = { if (query.isNullOrEmpty()) onQueryChange(null) else onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_clear))
                    }
                } else {
                    Text(
                        stringResource(R.string.mail_search_in),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (!searching) {
                val name = app.etsprivate.settings.StudentName.shown(context).orEmpty()
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        app.etsprivate.settings.StudentName.initials(name).ifEmpty { "?" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        if (searching) {
            Spacer(Modifier.height(16.dp))
        } else {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = Spacing.oneHalf),
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onClose: () -> Unit,
    onMarkRead: () -> Unit,
    onMarkUnread: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.pageColor).heightIn(min = 64.dp).padding(horizontal = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_cancel)) }
        Text(pluralStringResource(R.plurals.mail_selection_count, count, count), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

        IconButton(onClick = onArchive) { Icon(Icons.Outlined.Archive, contentDescription = stringResource(R.string.mail_swipe_archive)) }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.mail_swipe_delete)) }
        IconButton(onClick = onMarkRead) { Icon(Icons.Outlined.MarkEmailRead, contentDescription = stringResource(R.string.mail_swipe_toggle_read)) }
        Box {
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.mail_mark_unread)) }, leadingIcon = { Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null) }, onClick = { menu = false; onMarkUnread() })
                DropdownMenuItem(text = { Text(stringResource(R.string.mail_move_to_folder)) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = null) }, onClick = { menu = false; onMove() })
            }
        }
    }
}

@Composable
private fun MoveToFolderDialog(folders: List<MailFolderInfo>, onDismiss: () -> Unit, onSelect: (MailFolderInfo) -> Unit) {
    AppBottomSheet(
        title = stringResource(R.string.mail_move_to_folder),
        onDismissRequest = onDismiss,
        dismissLabel = stringResource(R.string.action_cancel),
    ) { close ->

        Column {
            folders.forEach { folder ->
                Row(
                    modifier = Modifier.fillMaxWidth().bleed(Spacing.half).clip(AppShapes.small)
                        .clickable { close { onSelect(folder) } }.padding(horizontal = Spacing.half, vertical = Spacing.oneHalf),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(folderIcon(folder), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(folderLabel(folder), modifier = Modifier.padding(start = Spacing.double))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageRow(
    modifier: Modifier = Modifier,
    message: MailMessage,
    swipeEnabled: Boolean,
    leftAction: MailSwipeAction,
    rightAction: MailSwipeAction,

    confirmDeleteFirst: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    highlighted: Boolean,

    busy: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (MailSwipeAction) -> Unit,
    onStar: () -> Unit,
    highlight: String?,
) {
    if (!swipeEnabled || busy) {
        MessageRowContent(message, selected, highlighted, selectionMode, busy, onClick, onLongClick, onStar, highlight, modifier)
        return
    }

    val latestOnAction = rememberUpdatedState(onAction)

    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { it == SwipeToDismissBoxValue.Settled })
    var width by remember { mutableIntStateOf(0) }
    val leftNow = rememberUpdatedState(leftAction)
    val rightNow = rememberUpdatedState(rightAction)

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(dismissState, width) {
        androidx.compose.runtime.snapshotFlow {
            val offset = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
            pressed && width > 0 && kotlin.math.abs(offset) >= width * SWIPE_ACTS_AT
        }.collect { now ->
            if (now != armed) {
                if (pressed) {
                    haptic.performHapticFeedback(
                        if (now) androidx.compose.ui.hapticfeedback.HapticFeedbackType.GestureThresholdActivate
                        else androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick,
                    )
                }
                armed = now
            }
        }
    }
    SwipeToDismissBox(
        modifier = modifier
            .onSizeChanged { width = it.width }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    do {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                    pressed = false

                    val offset = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
                    if (width > 0 && kotlin.math.abs(offset) >= width * SWIPE_ACTS_AT) {
                        val action = if (offset > 0) rightNow.value else leftNow.value
                        if (action != MailSwipeAction.NONE) latestOnAction.value(action)
                    }
                }
            },
        state = dismissState,
        enableDismissFromStartToEnd = rightAction != MailSwipeAction.NONE,
        enableDismissFromEndToStart = leftAction != MailSwipeAction.NONE,
        backgroundContent = {
            val action = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) rightAction else leftAction
            SwipeActionBackground(action, dismissState.dismissDirection, armed)
        },
    ) {
        MessageRowContent(message, selected, highlighted, selectionMode, busy, onClick, onLongClick, onStar, highlight)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActionBackground(action: MailSwipeAction, direction: SwipeToDismissBoxValue, armed: Boolean = false) {
    val (color, icon) = when (action) {
        MailSwipeAction.ARCHIVE -> MaterialTheme.colorScheme.primaryContainer to Icons.Outlined.Archive
        MailSwipeAction.DELETE -> MaterialTheme.colorScheme.errorContainer to Icons.Outlined.Delete
        MailSwipeAction.TOGGLE_READ -> MaterialTheme.colorScheme.secondaryContainer to Icons.Outlined.MarkEmailRead
        MailSwipeAction.NONE -> Color.Transparent to Icons.Outlined.MarkEmailUnread
    }
    Box(
        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = Spacing.triple),
        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        val scale by androidx.compose.animation.core.animateFloatAsState(
            if (armed) 1f else 0.75f,
            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 600f),
            label = "swipe-armed",
        )
        Icon(icon, contentDescription = null, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
    }
}

private const val SWIPE_ACTS_AT = 0.35f

@Composable
private fun MessageRowContent(
    message: MailMessage,
    selected: Boolean,
    highlighted: Boolean,
    selectionMode: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStar: () -> Unit,
    highlight: String?,
    modifier: Modifier = Modifier,
) {
    val appLocale = LocaleHelper.locale(LocalContext.current)

    val surface = app.etsprivate.ui.groupColor
    val background by animateColorAsState(
        targetValue = when {
            highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f).compositeOver(surface)
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> surface
        },
        animationSpec = tween(if (highlighted) 150 else 700),
        label = "message-row-highlight",
    )
    val context = LocalContext.current
    val sender = OwnName.seenSender(context, message).ifBlank { stringResource(R.string.mail_unknown_sender) }
    val unread = !message.isRead

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onLongClick)) {
            if (selected) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                SenderAvatar(sender, size = 40.dp, endPadding = 0.dp)
            }
        }

        val strong = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        val weight = if (unread) FontWeight.Bold else FontWeight.Normal
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    searchHighlight(sender, highlight),
                    style = MaterialTheme.typography.bodyLarge,
                    color = strong,
                    fontWeight = weight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (message.hasAttachment) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.half).size(16.dp),
                    )
                }
            }
            Text(
                searchHighlight(message.subject, highlight),
                style = MaterialTheme.typography.bodyMedium,
                color = strong,
                fontWeight = weight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (message.preview.isNotBlank()) {
                Text(
                    searchHighlight(message.preview, highlight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight().padding(start = Spacing.half),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp).size(16.dp), strokeWidth = 2.dp)
            } else if (message.date > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Text(
                        remember(message.date, appLocale) { listTime(message.date, appLocale, app.etsprivate.ui.Clock.pattern(context)) },
                        style = MaterialTheme.typography.labelMedium,
                        color = strong,
                        fontWeight = weight,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onStar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (message.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(if (message.isStarred) R.string.mail_unstar else R.string.mail_star),
                    tint = if (message.isStarred) StarColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private var printingView: android.webkit.WebView? = null

internal fun printMail(context: android.content.Context, title: String, html: String) {
    val view = android.webkit.WebView(context)
    printingView = view
    view.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(webView: android.webkit.WebView, url: String?) {
            var host: android.content.Context = context
            while (host !is android.app.Activity && host is android.content.ContextWrapper) host = host.baseContext
            val manager = host.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            manager.print(title, webView.createPrintDocumentAdapter(title), android.print.PrintAttributes.Builder().build())
            printingView = null
        }
    }
    view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
}

@Composable
private fun UnreadDot(modifier: Modifier = Modifier) = app.etsprivate.ui.NoticeDot(modifier)

internal class OwnDraft(val written: String, val header: String?, val quotedHtml: String)

internal fun splitOwnDraft(html: String): OwnDraft? {
    val open = Regex("<blockquote[^>]*>", RegexOption.IGNORE_CASE).find(html) ?: return null
    val close = html.lastIndexOf("</blockquote>", ignoreCase = true)
    if (close < open.range.last) return null
    val quoted = html.substring(open.range.last + 1, close)
    var head = html.substring(0, open.range.first)
    head = Regex("(?is)^.*?<body[^>]*>").replace(head, "")
    var header: String? = null
    val lastP = Regex("(?is)<p[^>]*>(.*?)</p>\\s*$").find(head)
    if (lastP != null) {
        header = htmlText(lastP.groupValues[1]).ifBlank { null }
        head = head.substring(0, lastP.range.first)
    }
    return OwnDraft(htmlText(head), header, quoted)
}

internal fun htmlText(html: String): String =
    androidx.core.text.HtmlCompat.fromHtml(html, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trimEnd()

internal val StarColor = Color(0xFFF4B400)

private fun searchHighlight(text: String, needle: String?): androidx.compose.ui.text.AnnotatedString {
    if (needle.isNullOrBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val lower = text.lowercase()
    val n = needle.lowercase()
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        while (true) {
            val j = lower.indexOf(n, i)
            if (j < 0) { append(text.substring(i)); break }
            append(text.substring(i, j))
            pushStyle(androidx.compose.ui.text.SpanStyle(background = Color(0xFFFDD663), color = Color(0xFF1F1F1F)))
            append(text.substring(j, j + n.length))
            pop()
            i = j + n.length
        }
    }
}

@Composable
private fun SearchFilters(
    senders: List<Pair<String, String>>,
    from: Pair<String, String>?, onFrom: (Pair<String, String>?) -> Unit,
    attachment: Boolean, onAttachment: (Boolean) -> Unit,
    unread: Boolean, onUnread: (Boolean) -> Unit,
    starred: Boolean, onStarred: (Boolean) -> Unit,
    days: Int, onDays: (Int) -> Unit,
    relevant: Boolean, onRelevant: (Boolean) -> Unit,
    sortable: Boolean,
) {
    val dayLabels = listOf(
        0 to stringResource(R.string.mail_date_any),
        7 to stringResource(R.string.mail_date_week),
        30 to stringResource(R.string.mail_date_month),
        365 to stringResource(R.string.mail_date_year),
    )
    Column(modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.pageColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.half),
        ) {
            var picking by remember { mutableStateOf(false) }
            SearchChip(from?.let { it.first.ifBlank { it.second } } ?: stringResource(R.string.mail_filter_from), from != null, dropdown = true) { picking = true }
            if (picking) {
                SenderPicker(senders = senders, onDismiss = { picking = false }, onPick = { picking = false; onFrom(it) })
            }
            SearchChip(stringResource(R.string.mail_filter_attachment), attachment) { onAttachment(!attachment) }
            SearchChip(stringResource(R.string.mail_filter_unread), unread) { onUnread(!unread) }
            SearchChip(stringResource(R.string.mail_filter_starred), starred) { onStarred(!starred) }
            Box {
                var open by remember { mutableStateOf(false) }
                SearchChip(
                    if (days == 0) stringResource(R.string.mail_filter_date) else dayLabels.first { it.first == days }.second,
                    days != 0,
                    dropdown = true,
                ) { open = true }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    dayLabels.forEach { (d, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { open = false; onDays(d) })
                    }
                }
            }
        }

        if (sortable) Box(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            var open by remember { mutableStateOf(false) }
            androidx.compose.material3.TextButton(onClick = { open = true }) {
                Text(
                    stringResource(if (relevant) R.string.mail_sort_relevant else R.string.mail_sort_recent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.mail_sort_recent)) }, onClick = { open = false; onRelevant(false) })
                DropdownMenuItem(text = { Text(stringResource(R.string.mail_sort_relevant)) }, onClick = { open = false; onRelevant(true) })
            }
        }
    }
}

internal fun fromMatches(m: MailMessage, from: Pair<String, String>?): Boolean {
    if (from == null) return true
    return if (from.second.isNotBlank()) m.fromAddress.equals(from.second, ignoreCase = true) else m.from == from.first
}

@Composable
private fun SenderPicker(senders: List<Pair<String, String>>, onDismiss: () -> Unit, onPick: (Pair<String, String>?) -> Unit) {
    var typed by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    app.etsprivate.ui.FullScreenDialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = app.etsprivate.ui.pageColor) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        BasicTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                        if (typed.isEmpty()) {
                            Text(
                                stringResource(R.string.mail_filter_search_senders),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (typed.isNotEmpty()) {
                        IconButton(onClick = { typed = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val needle = typed.trim().lowercase()
                val shown = remember(senders, needle) {
                    if (needle.isEmpty()) senders else senders.filter { it.first.lowercase().contains(needle) || it.second.lowercase().contains(needle) }
                }
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (needle.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.mail_filter_anyone),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth().clickable { onPick(null) }.padding(horizontal = 16.dp, vertical = 16.dp),
                            )
                        }
                    }
                    items(shown.size, key = { shown[it].second.lowercase().ifBlank { shown[it].first } }) { i ->
                        val (name, address) = shown[i]
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onPick(name to address) }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SenderAvatar(name.ifBlank { address }, size = 40.dp, endPadding = 16.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    searchHighlight(name.ifBlank { address }, typed.trim()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (name.isNotBlank() && address.isNotBlank()) {
                                    Text(
                                        searchHighlight(address, typed.trim()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchChip(label: String, selected: Boolean, dropdown: Boolean = false, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        shape = androidx.compose.foundation.shape.CircleShape,
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        trailingIcon = if (dropdown) {
            { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}

private fun listTime(millis: Long, locale: java.util.Locale, clockPattern: String = "HH:mm"): String {
    val then = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val now = java.util.Calendar.getInstance()
    val pattern = when {
        then.get(java.util.Calendar.YEAR) != now.get(java.util.Calendar.YEAR) -> "d MMM yyyy"
        then.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) -> clockPattern
        else -> "d MMM"
    }
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}

@Composable
private fun MessageDetailScreen(
    email: String,
    accessToken: String,
    folder: String,
    message: MailMessage,
    moveTargets: List<MailFolderInfo>,
    isTrashFolder: Boolean,
    isDraftsFolder: Boolean,
    onBack: () -> Unit,
    onCompose: (ComposeDraft) -> Unit,
    onMoveToFolder: (MailFolderInfo) -> Unit,
    onDelete: () -> Unit,

    onArchive: (() -> Unit)?,
    onMarkUnread: () -> Unit,

    folderName: String,
    onToggleStar: () -> Unit,

    visible: Boolean = true,
) {
    val appLocale = LocaleHelper.locale(LocalContext.current)

    MailClock.mark("detail compose uid=${message.uid} visible=$visible")

    androidx.compose.runtime.SideEffect {
        android.view.Choreographer.getInstance().postFrameCallback { MailClock.mark("FRAME DRAWN") }
    }
    val bodyKey = "mail/body/$email/$folder/${message.uid}"
    var body by remember(message.uid) { mutableStateOf(MemoryCache.get<MailBody>(bodyKey)) }
    var error by remember(message.uid) { mutableStateOf<String?>(null) }

    var reloadKey by remember(message.uid) { mutableIntStateOf(0) }

    var openLink by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Triple<String, String, ByteArray>?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val hostView = androidx.compose.ui.platform.LocalView.current
    val loadMessageFailedMessage = stringResource(R.string.mail_error_load_message)
    val shareFailedMessage = stringResource(R.string.action_share_failed)
    val yesterdayLabel = stringResource(R.string.mail_yesterday)
    var preparingShare by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val quotedOnFormat = stringResource(R.string.mail_quoted_on)
    val forwardedHeader = stringResource(R.string.mail_forwarded_header)

    fun replyDraft(replyAll: Boolean): ComposeDraft {
        val current = body
        val toAddress = current?.from?.address ?: message.from
        val ccAddresses = if (replyAll && current != null) {
            (current.to + current.cc).map { it.address }
                .filter { it.isNotBlank() && !it.equals(email, ignoreCase = true) && !it.equals(toAddress, ignoreCase = true) }
                .distinct()
        } else {
            emptyList()
        }
        val subject = if (message.subject.startsWith("Re:", ignoreCase = true)) message.subject else "Re: ${message.subject}"
        val quotedHeader = if (message.date > 0) {
            String.format(quotedOnFormat, SimpleDateFormat(app.etsprivate.ui.Clock.withTime(context, "d MMM yyyy, HH:mm"), LocaleHelper.locale(context)).format(Date(message.date)), current?.from?.let { OwnName.sent(context, it) }?.display ?: message.from)
        } else {
            null
        }
        return ComposeDraft(
            to = toAddress,
            cc = ccAddresses.joinToString(", "),
            subject = subject,
            inReplyTo = current?.messageId,
            references = current?.messageId,
            quoteHeader = quotedHeader,
            quotedHtml = current?.html,
            quotedText = quotableText(current),
            quotedImages = current?.inlineImages.orEmpty(),
        )
    }

    fun forwardDraft(): ComposeDraft {
        val current = body
        val subject = if (message.subject.startsWith("Fwd:", ignoreCase = true)) message.subject else "Fwd: ${message.subject}"
        val header = buildString {
            append(forwardedHeader)
            append('\n')
            append("${context.getString(R.string.mail_from)}: ${current?.from?.let { OwnName.sent(context, it) }?.display ?: message.from}\n")
            if (message.date > 0) append("${context.getString(R.string.mail_forward_date)}: ${SimpleDateFormat(app.etsprivate.ui.Clock.withTime(context, "d MMM yyyy, HH:mm"), LocaleHelper.locale(context)).format(Date(message.date))}\n")
            append("${context.getString(R.string.mail_field_subject)}: ${message.subject}\n")
            val recipients = current?.to.orEmpty()
            if (recipients.isNotEmpty()) append("${context.getString(R.string.mail_to)}: ${recipients.joinToString(", ") { OwnName.sent(context, it).display }}\n")
        }
        return ComposeDraft(
            subject = subject,
            quoteHeader = header.trim(),
            quotedHtml = current?.html,
            quotedText = quotableText(current),
            quotedImages = current?.inlineImages.orEmpty(),

            existingAttachments = current?.attachments.orEmpty(),
            attachmentsFolder = folder,
            attachmentsUid = message.uid,
        )
    }

    if (preparingShare) PreparingOverlay(stringResource(R.string.action_preparing_share))

    if (showMoveDialog) {
        MoveToFolderDialog(
            folders = moveTargets,
            onDismiss = { showMoveDialog = false },
            onSelect = { target ->
                showMoveDialog = false
                onMoveToFolder(target)
            },
        )
    }

    LaunchedEffect(message.uid) { MailSync.clearNotification(context, message.uid) }

    LaunchedEffect(message.uid, reloadKey) {
        error = null
        try {
            loadCached(
                context = context,
                key = bodyKey,
                decode = ::decodeBody,
                encode = ::encodeBody,
                fetch = { withContext(Dispatchers.IO) { OutlookImap.fetchBody(email, accessToken, folder, message.uid) } },
                render = {
                    android.util.Log.i("MailDbg", "body render uid=${message.uid} sameAsShown=${it == body}")
                    body = it
                },
            )
        } catch (e: Exception) {
            android.util.Log.i("MailDbg", "body fetch failed uid=${message.uid} hadBody=${body != null}: $e")
            if (body == null) error = loadMessageFailedMessage
        }
    }

    openLink?.let { url ->
        InAppBrowser(url = url, onBack = { openLink = null })
        return
    }
    preview?.let { (filename, mimeType, bytes) ->
        AttachmentPreviewScreen(filename = filename, mimeType = mimeType, bytes = bytes, onBack = { preview = null })
        return
    }

    if (isDraftsFolder) {
        BackHandler(enabled = visible, onBack = onBack)
        val loaded = body
        if (loaded == null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                if (error != null) {
                    CenteredMessage(error!!)
                } else {
                    FullScreenLoading()
                }
            }
            return
        }
        LaunchedEffect(loaded) {
            val split = loaded.html?.let { splitOwnDraft(it) }
            onCompose(
                ComposeDraft(
                    to = loaded.to.joinToString(", ") { it.address },
                    cc = loaded.cc.joinToString(", ") { it.address },
                    bcc = loaded.bcc.joinToString(", ") { it.address },
                    subject = message.subject,
                    body = split?.written ?: quotableText(loaded),
                    quoteHeader = split?.header,
                    quotedHtml = split?.quotedHtml,
                    quotedText = split?.quotedHtml?.let { htmlText(it) }.orEmpty(),
                    quotedImages = if (split != null) loaded.inlineImages else emptyMap(),
                    editingUid = message.uid,
                    editingFolder = folder,
                    existingAttachments = loaded.attachments,
                ),
            )
        }
        return
    }

    val current = body
    val unknownSenderLabel = stringResource(R.string.mail_unknown_sender)
    var showMoreMenu by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val textScrollState = rememberScrollState()
    val headerOffsetPx = if (current?.html != null) 0f else -textScrollState.value.toFloat().coerceIn(0f, headerHeightPx)
    val headerHeightDp = with(density) { headerHeightPx.toDp() }

    var closingForDelete by remember(message.uid) { mutableStateOf(false) }
    val closeAlpha by animateFloatAsState(
        targetValue = if (closingForDelete) 0f else 1f,
        animationSpec = tween(200),
        label = "message-delete-fade",
    )
    LaunchedEffect(closingForDelete) {
        if (closingForDelete) {
            delay(200)
            onDelete()
        }
    }

    BackHandler(enabled = visible, onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().alpha(closeAlpha).background(app.etsprivate.ui.pageColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = Spacing.half),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.weight(1f))
            if (onArchive != null) {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Outlined.Archive, contentDescription = stringResource(R.string.mail_swipe_archive))
                }
            }
            IconButton(onClick = { closingForDelete = true }) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(if (isTrashFolder) R.string.action_delete_forever else R.string.mail_swipe_delete))
            }
            IconButton(onClick = onMarkUnread) {
                Icon(Icons.Outlined.MarkEmailUnread, contentDescription = stringResource(R.string.mail_mark_unread))
            }
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    val inbox = moveTargets.firstOrNull { it.role == MailFolderRole.INBOX }
                    val junk = moveTargets.firstOrNull { it.role == MailFolderRole.JUNK }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mail_move_to_folder)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = null) },
                        onClick = { showMoreMenu = false; showMoveDialog = true },
                    )
                    if (inbox != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mail_move_to_inbox)) },
                            leadingIcon = { Icon(Icons.Outlined.MoveToInbox, contentDescription = null) },
                            onClick = { showMoreMenu = false; onMoveToFolder(inbox) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(if (message.isStarred) R.string.mail_unstar else R.string.mail_star)) },
                        leadingIcon = { Icon(if (message.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null) },
                        onClick = { showMoreMenu = false; onToggleStar() },
                    )
                    if (current != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mail_print)) },
                            leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                printMail(
                                    hostView.context,
                                    message.subject,
                                    current.html?.let { wrapEmailHtml(it) } ?: wrapEmailHtml("<pre>${escapeHtml(quotableText(current))}</pre>"),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share_text)) },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                ShareAsImage.shareText(
                                    context,
                                    message.subject,
                                    buildString {
                                        append(message.subject).append("\n\n")
                                        append(quotableText(current))
                                    },
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share_image)) },
                            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                scope.launch {
                                    preparingShare = true

                                    val html = current.html?.let { wrapEmailHtml(it) }
                                        ?: wrapEmailHtml("<pre>${quotableText(current)}</pre>")
                                    val file = ShareAsImage.renderOrNull(
                                        context,
                                        html,
                                        "message",
                                        current.inlineImages.mapValues { it.value.mimeType to it.value.base64 },
                                    )
                                    preparingShare = false
                                    if (file == null) AppFeedback.show(shareFailedMessage) else ShareAsImage.sharePng(context, file)
                                }
                            },
                        )
                    }
                    if (junk != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mail_report_spam), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMoreMenu = false; onMoveToFolder(junk) },
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            val showBodyLoading = rememberDelayedLoading(current == null && error == null)

            Box(modifier = Modifier.fillMaxSize().background(app.etsprivate.ui.groupColor)) {
                when {
                    error != null -> Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { reloadKey++ }, modifier = Modifier.padding(top = Spacing.oneHalf)) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                    current == null -> { if (showBodyLoading) FullScreenLoading() }

                    !current.html.isNullOrBlank() -> {
                        Box(modifier = Modifier.fillMaxSize().padding(top = headerHeightDp)) {
                            HtmlBodyView(
                                html = current.html.orEmpty(),
                                inlineImages = current.inlineImages,
                                onLinkClick = { openLinkFromMail(context, it) { inApp -> openLink = inApp } },
                                visible = visible,
                            )
                        }
                    }
                    else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(textScrollState)) {
                        Spacer(Modifier.height(headerHeightDp))
                        Text(
                            linkedText(current.text) { openLinkFromMail(context, it) { inApp -> openLink = inApp } },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            if (current != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .zIndex(1f)
                        .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                        .onGloballyPositioned { if (it.size.height > 0) headerHeightPx = it.size.height.toFloat() }
                        .background(app.etsprivate.ui.pageColor),
                ) {
                    var subjectExpanded by remember(message.uid) { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.Top) {
                    FlowRow(
                        modifier = Modifier.weight(1f).padding(start = 20.dp, top = Spacing.half, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.default),
                        verticalArrangement = Arrangement.spacedBy(Spacing.half),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            message.subject,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = if (subjectExpanded) Int.MAX_VALUE else 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { subjectExpanded = !subjectExpanded },
                        )
                        if (folderName.isNotBlank()) {
                            Text(
                                folderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    IconButton(onClick = onToggleStar, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(
                            if (message.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(if (message.isStarred) R.string.mail_unstar else R.string.mail_star),
                            tint = if (message.isStarred) StarColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    }

                    var headerOpen by remember(message.uid) { mutableStateOf(false) }
                    val fromAddress = current.from?.let { OwnName.seen(context, it) }
                    val senderName = fromAddress?.display?.takeIf { it.isNotBlank() } ?: message.from.ifBlank { unknownSenderLabel }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(app.etsprivate.ui.groupColor)
                            .padding(start = 16.dp, end = Spacing.half, top = 16.dp, bottom = Spacing.default),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SenderAvatar(senderName)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    senderName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (message.date > 0) {
                                    Text(
                                        remember(message.date, appLocale) { gmailTime(message.date, appLocale, yesterdayLabel, app.etsprivate.ui.Clock.pattern(context)) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.padding(start = Spacing.default),
                                    )
                                }
                            }
                            val toMe = current.to.any { it.address.equals(email, ignoreCase = true) }
                            val others = current.to.filterNot { it.address.equals(email, ignoreCase = true) }
                                .joinToString(", ") { OwnName.seen(context, it).display.ifBlank { it.address } }
                            Row(
                                modifier = Modifier.clip(AppShapes.small).clickable { headerOpen = !headerOpen },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    when {
                                        toMe && others.isEmpty() -> stringResource(R.string.mail_to_me)
                                        else -> stringResource(R.string.mail_to_names, others.ifBlank { stringResource(R.string.mail_to_me) })
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Icon(
                                    if (headerOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Box {
                            var messageMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { messageMenu = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                            }
                            DropdownMenu(expanded = messageMenu, onDismissRequest = { messageMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mail_reply)) },
                                    leadingIcon = { Icon(GmailIcons.Reply, contentDescription = null) },
                                    onClick = { messageMenu = false; onCompose(replyDraft(replyAll = false)) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mail_reply_all)) },
                                    leadingIcon = { Icon(GmailIcons.ReplyAll, contentDescription = null) },
                                    onClick = { messageMenu = false; onCompose(replyDraft(replyAll = true)) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mail_forward)) },
                                    leadingIcon = { Icon(GmailIcons.Forward, contentDescription = null) },
                                    onClick = { messageMenu = false; onCompose(forwardDraft()) },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (message.isStarred) R.string.mail_unstar else R.string.mail_star)) },
                                    leadingIcon = { Icon(if (message.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null) },
                                    onClick = { messageMenu = false; onToggleStar() },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mail_print)) },
                                    leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
                                    onClick = {
                                        messageMenu = false
                                        printMail(
                                            hostView.context,
                                            message.subject,
                                            current.html?.let { wrapEmailHtml(it) } ?: wrapEmailHtml("<pre>${escapeHtml(quotableText(current))}</pre>"),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.groupColor)) {
                    if (headerOpen) {
                        var allRecipients by remember(message.uid) { mutableStateOf(false) }
                        val recipientRows = current.to.map { stringResource(R.string.mail_to) to OwnName.seen(context, it) } +
                            current.cc.map { stringResource(R.string.mail_field_cc) to OwnName.seen(context, it) }
                        val shown = if (allRecipients) recipientRows else recipientRows.take(3)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = Spacing.default)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(Spacing.oneHalf)
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            if (fromAddress != null) AddressRow(stringResource(R.string.mail_from), fromAddress)
                            shown.forEach { (label, address) -> AddressRow(label, address) }
                            if (!allRecipients && recipientRows.size > shown.size) {
                                Text(
                                    stringResource(R.string.mail_more_recipients, recipientRows.size - shown.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(AppShapes.small)
                                        .clickable { allRecipients = true }
                                        .padding(vertical = Spacing.quarter),
                                )
                            }
                            if (message.date > 0) {
                                Text(
                                    "${stringResource(R.string.mail_date)}: " + remember(message.date, appLocale) {
                                        SimpleDateFormat(app.etsprivate.ui.Clock.withTime(context, "EEEE d MMMM yyyy, HH:mm"), appLocale).format(Date(message.date))
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.quarter),
                                )
                            }
                        }
                    }
                    AttachmentsSection(email, accessToken, folder, message.uid, current.attachments, onPreview = { f, m, b -> preview = Triple(f, m, b) })
                    }
                }
            }
        }

        if (current != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(app.etsprivate.ui.pageColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReplyPill(stringResource(R.string.mail_reply), GmailIcons.Reply, Modifier.weight(1f)) { onCompose(replyDraft(replyAll = false)) }
                ReplyPill(stringResource(R.string.mail_forward), GmailIcons.Forward, Modifier.weight(1f)) { onCompose(forwardDraft()) }
            }
        }
    }
}

@Composable
private fun ReplyPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.padding(start = Spacing.default))
    }
}

private fun gmailTime(millis: Long, locale: java.util.Locale, yesterday: String, clockPattern: String = "HH:mm"): String {
    val then = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val now = java.util.Calendar.getInstance()
    val before = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    if (then.get(java.util.Calendar.YEAR) == before.get(java.util.Calendar.YEAR) &&
        then.get(java.util.Calendar.DAY_OF_YEAR) == before.get(java.util.Calendar.DAY_OF_YEAR)
    ) return yesterday
    val pattern = when {
        then.get(java.util.Calendar.YEAR) != now.get(java.util.Calendar.YEAR) -> "d MMM yyyy"
        then.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) -> clockPattern
        else -> "d MMM"
    }
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}

private val AvatarColors = listOf(
    0xFF1A73E8, 0xFFD93025, 0xFF188038, 0xFFE37400, 0xFF9334E6, 0xFF007B83, 0xFFC5221F, 0xFF12A4AF, 0xFFE52592, 0xFF5F6368,
).map { androidx.compose.ui.graphics.Color(it) }

internal fun avatarInitials(name: String): String {
    val base = name.trim().let { if ('@' in it && ' ' !in it) it.substringBefore('@') else it }
    val words = base.split(Regex("[\\s,._\\-\"'()<>]+")).filter { it.firstOrNull()?.isLetter() == true }
    return listOfNotNull(words.firstOrNull()?.first(), words.drop(1).lastOrNull()?.first())
        .joinToString("").uppercase().ifEmpty { "?" }
}

@Composable
private fun SenderAvatar(name: String, size: Dp = 48.dp, endPadding: Dp = Spacing.oneHalf) {
    Box(
        modifier = Modifier
            .padding(end = endPadding)
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(AvatarColors[(name.lowercase().hashCode() and 0x7fffffff) % AvatarColors.size]),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            avatarInitials(name),
            style = MaterialTheme.typography.titleMedium,
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}

@Composable
private fun AddressRow(label: String, address: MailAddress) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CopyableValue(
            value = if (address.name.isNotBlank()) "${address.name} <${address.address}>" else address.address,
            label = label,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

internal fun isPreviewable(mimeType: String) =
    mimeType.startsWith("image/") || mimeType == "application/pdf" || isMedia(mimeType)

internal fun isMedia(mimeType: String) = mimeType.startsWith("video/") || mimeType.startsWith("audio/")

private fun quotableText(body: MailBody?): String {
    if (body == null) return ""
    val html = body.html ?: return body.text
    return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()
}

@Composable
private fun AttachmentsSection(
    email: String,
    accessToken: String,
    folder: String,
    uid: Long,
    attachments: List<MailAttachment>,
    onPreview: (filename: String, mimeType: String, bytes: ByteArray) -> Unit,
) {
    if (attachments.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.mail_attachment_saved)
    val saveFailedMessage = stringResource(R.string.mail_attachment_save_failed)
    val requiresAndroid10Message = stringResource(R.string.mail_attachment_requires_android10)

    var openingPath by remember { mutableStateOf<String?>(null) }
    var savingPath by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = Spacing.oneHalf),
        horizontalArrangement = Arrangement.spacedBy(Spacing.default),
    ) {
        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable {
                        scope.launch {
                            openOrPreviewAttachment(
                                context, email, accessToken, folder, uid, attachment, onPreview,
                                onLoadingChange = { openingPath = if (it) attachment.path else null },
                                saveFailedMessage,
                            )
                        }
                    }
                    .padding(start = Spacing.oneHalf, top = Spacing.default, bottom = Spacing.default),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (openingPath == attachment.path) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f).padding(start = Spacing.default)) {
                    Text(
                        attachment.filename,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatAttachmentSize(attachment.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (savingPath == attachment.path) {
                    CircularProgressIndicator(modifier = Modifier.padding(Spacing.oneHalf).size(18.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(
                        onClick = {
                            scope.launch {
                                saveAttachmentToDownloads(
                                    context, email, accessToken, folder, uid, attachment,
                                    onLoadingChange = { savingPath = if (it) attachment.path else null },
                                    savedMessage, saveFailedMessage, requiresAndroid10Message,
                                )
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.mail_attachment_saved), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private suspend fun openOrPreviewAttachment(
    context: android.content.Context,
    email: String,
    accessToken: String,
    folder: String,
    uid: Long,
    attachment: MailAttachment,
    onPreview: (filename: String, mimeType: String, bytes: ByteArray) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    saveFailedMessage: String,
) {
    onLoadingChange(true)
    try {
        val (filename, bytes) = withContext(Dispatchers.IO) { OutlookImap.downloadAttachment(email, accessToken, folder, uid, attachment.path) }
        if (isPreviewable(attachment.mimeType)) {
            onPreview(filename, attachment.mimeType, bytes)
        } else {
            withContext(Dispatchers.IO) { openWithSystemViewer(context, filename, attachment.mimeType, bytes) }
        }
    } catch (e: Exception) {
        AppFeedback.show(saveFailedMessage)
    } finally {
        onLoadingChange(false)
    }
}

private suspend fun saveAttachmentToDownloads(
    context: android.content.Context,
    email: String,
    accessToken: String,
    folder: String,
    uid: Long,
    attachment: MailAttachment,
    onLoadingChange: (Boolean) -> Unit,
    savedMessage: String,
    saveFailedMessage: String,
    requiresAndroid10Message: String,
) {
    onLoadingChange(true)
    val toastMessage = try {
        val (filename, bytes) = withContext(Dispatchers.IO) { OutlookImap.downloadAttachment(email, accessToken, folder, uid, attachment.path) }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            requiresAndroid10Message
        } else {
            val saved = withContext(Dispatchers.IO) { AttachmentSaver.saveToDownloads(context, filename, attachment.mimeType, bytes) }
            if (saved) savedMessage else saveFailedMessage
        }
    } catch (e: Exception) {
        saveFailedMessage
    } finally {
        onLoadingChange(false)
    }
    AppFeedback.show(toastMessage)
}

internal fun formatAttachmentSize(bytes: Long): String = when {
    bytes < 0 -> "?"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}

internal fun openWithSystemViewer(context: android.content.Context, filename: String, mimeType: String, bytes: ByteArray) {
    val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
    val file = File(dir, filename)
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType.ifBlank { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, filename))
}

@Composable
internal fun AttachmentPreviewScreen(filename: String, mimeType: String, bytes: ByteArray, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.mail_attachment_saved)
    val saveFailedMessage = stringResource(R.string.mail_attachment_save_failed)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AppTopBar(title = filename, onBack = onBack, actions = {
            IconButton(onClick = {
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { AttachmentSaver.saveToDownloads(context, filename, mimeType, bytes) }
                    AppFeedback.show(if (saved) savedMessage else saveFailedMessage)
                }
            }) {
                Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.mail_attachment_saved))
            }
        })
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                mimeType == "application/pdf" -> PdfPreview(bytes)
                isMedia(mimeType) -> MediaPreview(filename, mimeType, bytes)
                else -> ImagePreview(bytes)
            }
        }
    }
}

@Composable
private fun MediaPreview(filename: String, mimeType: String, bytes: ByteArray) {
    val context = LocalContext.current
    val file = remember(bytes) {
        val extension = filename.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
        File.createTempFile("media", extension, context.cacheDir).apply { writeBytes(bytes) }
    }
    DisposableEffect(file) { onDispose { file.delete() } }
    MediaPlayerView(android.net.Uri.fromFile(file), null, mimeType, filename)
}

@Composable
internal fun MediaPlayerView(uri: android.net.Uri, headers: Map<String, String>?, mimeType: String, title: String) {
    val audio = mimeType.startsWith("audio/")
    var player by remember { mutableStateOf<android.widget.VideoView?>(null) }
    var ready by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var failed by remember(uri) { mutableStateOf(false) }
    var position by remember(uri) { mutableStateOf(0) }
    var duration by remember(uri) { mutableStateOf(0) }
    var dragging by remember(uri) { mutableStateOf<Float?>(null) }
    LaunchedEffect(player, ready) {
        val view = player ?: return@LaunchedEffect
        while (ready) {
            if (dragging == null) position = view.currentPosition
            playing = view.isPlaying
            delay(250)
        }
    }
    DisposableEffect(Unit) { onDispose { player?.stopPlayback() } }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    android.widget.VideoView(viewContext).apply {
                        player = this
                        setOnPreparedListener {
                            duration = it.duration
                            ready = true
                            start()
                        }
                        setOnCompletionListener { playing = false }
                        setOnErrorListener { _, what, extra ->
                            android.util.Log.i("Preview", "media failed $mimeType what=$what extra=$extra")
                            failed = true
                            true
                        }
                        setVideoURI(uri, headers)
                    }
                },
            )
            when {
                failed -> Text(stringResource(R.string.file_preview_failed), color = Color.White, modifier = Modifier.padding(Spacing.double))

                audio -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Spacing.double)) {
                    Icon(Icons.Outlined.Audiotrack, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Text(title, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.default))
                }
                !ready -> CircularProgressIndicator(color = Color.White)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.default, vertical = Spacing.half),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = ready,
                onClick = {
                    val view = player ?: return@IconButton
                    if (view.isPlaying) view.pause() else view.start()
                    playing = view.isPlaying
                },
            ) {
                Icon(
                    if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(if (playing) R.string.media_pause else R.string.media_play),
                    tint = Color.White,
                )
            }
            Slider(
                value = dragging ?: position.toFloat(),
                valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                enabled = ready && duration > 0,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { player?.seekTo(it.toInt()); position = it.toInt() }
                    dragging = null
                },
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.half),
            )
            Text(
                "${mediaTime((dragging ?: position.toFloat()).toInt())} / ${mediaTime(duration)}",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun mediaTime(millis: Int): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return if (seconds >= 3600) {
        "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    } else {
        "%d:%02d".format(seconds / 60, seconds % 60)
    }
}

@Composable
private fun ImagePreview(bytes: ByteArray) {
    val bitmap = remember(bytes) { runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull() }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            ZoomableBox(modifier = Modifier.fillMaxSize()) {
                Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun PdfPreview(bytes: ByteArray) {
    val context = LocalContext.current
    var pages by remember(bytes) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var failed by remember(bytes) { mutableStateOf(false) }

    LaunchedEffect(bytes) {
        val started = android.os.SystemClock.uptimeMillis()
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val file = File.createTempFile("preview", ".pdf", context.cacheDir)
                try {
                    file.writeBytes(bytes)
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            for (index in 0 until renderer.pageCount) {
                                val bitmap = renderer.openPage(index).use { page ->
                                    Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888).also {
                                        it.eraseColor(android.graphics.Color.WHITE)
                                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    }
                                }
                                withContext(Dispatchers.Main) { pages = pages + bitmap }
                                if (index == 0) {
                                    android.util.Log.i("Preview", "first of ${renderer.pageCount} pages in ${android.os.SystemClock.uptimeMillis() - started}ms")
                                }
                            }
                        }
                    }
                } finally {
                    file.delete()
                }
            }.isSuccess
        }
        if (!ok && pages.isEmpty()) failed = true
    }
    when {
        failed -> Text(
            stringResource(R.string.file_preview_failed),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(Spacing.double),
        )

        pages.isEmpty() -> if (rememberDelayedLoading(true)) FullScreenLoading()
        else -> ZoomableBox(modifier = Modifier.fillMaxSize().background(Color.White)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pages) { page ->
                    Image(page.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().padding(4.dp), contentScale = ContentScale.FillWidth)
                }
            }
        }
    }
}

@Composable
private fun linkedText(raw: String, onLink: (String) -> Unit): androidx.compose.ui.text.AnnotatedString {
    val style = androidx.compose.ui.text.TextLinkStyles(
        style = androidx.compose.ui.text.SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
        ),
    )
    return androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        LINK_REGEX.findAll(raw).forEach { found ->

            val text = found.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '>', '"', '\'')
            append(raw.substring(last, found.range.first))
            val url = if (text.startsWith("www.", ignoreCase = true)) "https://$text" else text
            withLink(
                androidx.compose.ui.text.LinkAnnotation.Clickable(
                    tag = url,
                    styles = style,
                    linkInteractionListener = { onLink(url) },
                ),
            ) {
                append(text)
            }
            last = found.range.first + text.length
        }
        append(raw.substring(last))
    }
}

private val LINK_REGEX = Regex("""(https?://|www\.)[^\s<>"']+""", RegexOption.IGNORE_CASE)

private fun bodyWebViewClient(
    inlineImages: Map<String, MailInlineImage>,
    onLinkClick: (String) -> Unit,
    onFinished: () -> Unit,
) = object : android.webkit.WebViewClient() {
    override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        onLinkClick(url)
        return true
    }

    override fun shouldInterceptRequest(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        if (!url.startsWith("cid:")) return null
        val image = inlineImages[url.removePrefix("cid:")] ?: return null
        val bytes = Base64.decode(image.base64, Base64.NO_WRAP)
        return android.webkit.WebResourceResponse(image.mimeType, "utf-8", java.io.ByteArrayInputStream(bytes))
    }

    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
        onFinished()
    }

    override fun onPageCommitVisible(view: android.webkit.WebView?, url: String?) {
        onFinished()
    }

    override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
        android.util.Log.w("MailBody", "error ${error?.errorCode} ${error?.description} ${request?.url?.toString()?.take(160)}")
    }

    override fun onReceivedHttpError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
        android.util.Log.w("MailBody", "http ${errorResponse?.statusCode} ${request?.url?.toString()?.take(160)}")
    }
}

object MailClock {
    private var start = 0L
    fun begin(tab: String) {
        start = android.os.SystemClock.uptimeMillis()
        android.util.Log.i("MailDbg", "=== nav tap $tab ===")
    }
    fun mark(what: String) {
        if (start == 0L) return
        android.util.Log.i("MailDbg", "${android.os.SystemClock.uptimeMillis() - start}ms $what")
    }
}

private object MailDbg {
    private var nextScreen = 0
    fun nextScreenId() = ++nextScreen
    fun id(view: android.webkit.WebView?) =
        if (view == null) "none" else "v@" + Integer.toHexString(System.identityHashCode(view))
    fun log(screen: Int, what: String) {
        MailClock.mark("#$screen $what")
    }
    fun holder(what: String) {
        MailClock.mark("   holder: $what")
    }
}

private object LastRenderedBody {
    private var html: String? = null
    private var owner: android.app.Activity? = null
    private var view: android.webkit.WebView? = null

    private fun activityOf(context: android.content.Context): android.app.Activity? {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper) {
            if (c is android.app.Activity) return c
            c = c.baseContext
        }
        return null
    }

    fun matches(context: android.content.Context, forHtml: String): Boolean {
        val result = view != null && html == forHtml && owner === activityOf(context)
        MailDbg.holder("matches=$result held=${MailDbg.id(view)} sameHtml=${html == forHtml} sameOwner=${owner === activityOf(context)}")
        return result
    }

    fun take(context: android.content.Context): android.webkit.WebView? {
        if (owner !== activityOf(context)) {
            clear()
            return null
        }

        val taken = view ?: run { MailDbg.holder("take -> empty"); return null }
        MailDbg.holder("take -> ${MailDbg.id(taken)} (parent=${taken.parent != null})")
        view = null
        html = null
        owner = null
        (taken.parent as? android.view.ViewGroup)?.removeView(taken)
        return taken
    }

    fun keep(context: android.content.Context, forHtml: String, webView: android.webkit.WebView) {
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        if (view != null && view !== webView) {
            MailDbg.holder("keep ${MailDbg.id(webView)} REJECTED, slot has ${MailDbg.id(view)} -> destroying")
            webView.destroy()
            return
        }
        MailDbg.holder("keep ${MailDbg.id(webView)}")
        html = forHtml
        owner = activityOf(context)
        view = webView
    }

    fun clear() {
        MailDbg.holder("clear (held=${MailDbg.id(view)})")
        view?.takeIf { it.parent == null }?.destroy()
        view = null
        html = null
        owner = null
    }
}

@Composable
private fun HtmlBodyView(
    html: String,
    inlineImages: Map<String, MailInlineImage>,
    onLinkClick: (String) -> Unit,

    visible: Boolean = true,
) {
    val screenId = remember { MailDbg.nextScreenId() }
    val wrapped = remember(html) { wrapEmailHtml(html) }
    MailDbg.log(screenId, "compose html=${html.length}/${wrapped.length} chars")
    val context = LocalContext.current

    val reusing = remember(wrapped) { LastRenderedBody.matches(context, wrapped) }
    remember(wrapped) { MailDbg.log(screenId, "reusing=$reusing") }

    val loadedHtml = remember(wrapped) {
        androidx.compose.runtime.mutableStateOf(if (reusing) wrapped else null)
    }
    var pageFinished by remember(html) { mutableStateOf(reusing) }
    var hasLoadedOnce by remember(html) { mutableStateOf(reusing) }

    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    val currentHtml by rememberUpdatedState(wrapped)
    DisposableEffect(Unit) {
        onDispose {
            MailDbg.log(screenId, "dispose, view=${MailDbg.id(webView)}")
            webView?.let { LastRenderedBody.keep(context, currentHtml, it) }
        }
    }
    LaunchedEffect(pageFinished) {
        if (pageFinished) {
            delay(180)
            hasLoadedOnce = true
            MailDbg.log(screenId, "revealed")
        }
    }

    val revealAlpha = if (hasLoadedOnce) 1f else 0f
    val showLoading = rememberDelayedLoading(!hasLoadedOnce)
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.quarter, vertical = Spacing.half)
                .alpha(revealAlpha),
            factory = { viewContext ->
                LastRenderedBody.take(viewContext)?.also {
                    webView = it
                    MailDbg.log(screenId, "factory took ${MailDbg.id(it)}")
                } ?: android.webkit.WebView(viewContext).apply {
                    webView = this
                    MailDbg.log(screenId, "factory CREATED ${MailDbg.id(this)}")

                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(android.graphics.Color.WHITE)
                    settings.javaScriptEnabled = false

                    settings.setSupportMultipleWindows(true)
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onCreateWindow(
                            view: android.webkit.WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?,
                        ): Boolean {
                            val here = view ?: return false
                            val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport ?: return false

                            val popup = android.webkit.WebView(here.context)
                            var handed = false
                            fun handOver(target: String?) {
                                if (handed || target.isNullOrBlank() || target == "about:blank") return
                                handed = true
                                onLinkClick(target)
                                popup.post { popup.destroy() }
                            }
                            popup.webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    v: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                ): Boolean {
                                    handOver(request?.url?.toString())
                                    return true
                                }

                                override fun onPageStarted(v: android.webkit.WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                    handOver(pageUrl)
                                }
                            }
                            transport.webView = popup
                            resultMsg.sendToTarget()
                            return true
                        }
                    }
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        @Suppress("DEPRECATION")
                        settings.forceDark = android.webkit.WebSettings.FORCE_DARK_OFF
                    }
                    loadedHtml.value = wrapped
                    tag = wrapped
                    post {
                        MailDbg.log(screenId, "factory load runs, stillWanted=${tag == wrapped}")
                        if (tag == wrapped) loadDataWithBaseURL(null, wrapped, "text/html", "utf-8", null)
                    }
                }
            },

            update = { view ->

                view.visibility = if (visible) android.view.View.VISIBLE else android.view.View.INVISIBLE

                view.webViewClient = bodyWebViewClient(inlineImages, onLinkClick) {
                    pageFinished = true
                }
                MailDbg.log(screenId, "update on ${MailDbg.id(view)} needsLoad=${loadedHtml.value != wrapped} visible=$visible")
                if (loadedHtml.value != wrapped) {
                    loadedHtml.value = wrapped

                    view.post {
                        view.loadDataWithBaseURL(null, wrapped, "text/html", "utf-8", null)
                    }
                }
            },
        )
        if (showLoading) {
            FullScreenLoading(modifier = Modifier.background(Color.White), onLightBackground = true)
        }
    }
}

private val CHARSET_META_REGEX = Regex("<meta[^>]*charset=[^>]*>", RegexOption.IGNORE_CASE)
private val VIEWPORT_META_REGEX = Regex("<meta[^>]*name=[\"']viewport[\"'][^>]*>", RegexOption.IGNORE_CASE)
private val HEAD_TAG_REGEX = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)
private val HTML_TAG_REGEX = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)

internal fun wrapEmailHtml(html: String): String {
    val cleaned = html.replace(CHARSET_META_REGEX, "").replace(VIEWPORT_META_REGEX, "")

    val baseline = """
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width">
        <style>body { width: 100% !important; box-sizing: border-box !important; margin: 0 !important; padding: 0 !important; overflow-wrap: break-word; word-wrap: break-word; }</style>
    """.trimIndent()
    return insertAfterFirst(cleaned, HEAD_TAG_REGEX, baseline)
        ?: insertAfterFirst(cleaned, HTML_TAG_REGEX, "<head>$baseline</head>")
        ?: "<html><head>$baseline</head><body>$cleaned</body></html>"
}

private fun insertAfterFirst(html: String, tag: Regex, insertion: String): String? {
    val match = tag.find(html) ?: return null
    val insertAt = match.range.last + 1
    return html.substring(0, insertAt) + insertion + html.substring(insertAt)
}

internal fun openLinkFromMail(context: android.content.Context, url: String, openHere: (String) -> Unit) {
    val uri = android.net.Uri.parse(url)
    val host = uri.host.orEmpty().lowercase()
    val ets = host == "etsmtl.ca" || host.endsWith(".etsmtl.ca")
    val web = uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)
    if (web && ets) {
        openHere(url)
        return
    }
    val opened = runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, uri).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess

    if (!opened && web) openHere(url)
}

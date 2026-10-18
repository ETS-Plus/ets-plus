package app.etsprivate.mail

import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.automirrored.outlined.Send
import app.etsprivate.ui.theme.AppShapes
import app.etsprivate.ui.bleed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.viewinterop.AndroidView
import app.etsprivate.ui.ConfirmDialog
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.InputChip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.etsprivate.R
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.AppTopBar
import app.etsprivate.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ComposeDraft(
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
    val inReplyTo: String? = null,
    val references: String? = null,

    val quoteHeader: String? = null,

    val quotedHtml: String? = null,
    val quotedText: String = "",

    val quotedImages: Map<String, MailInlineImage> = emptyMap(),

    val editingUid: Long? = null,
    val editingFolder: String? = null,

    val existingAttachments: List<MailAttachment> = emptyList(),

    val attachmentsFolder: String? = null,
    val attachmentsUid: Long? = null,

    val sharedAttachments: List<OutgoingAttachment> = emptyList(),
)

@Composable
fun ComposeMailScreen(fromEmail: String, accessToken: String, draft: ComposeDraft, onBack: () -> Unit, onSent: () -> Unit) {
    var to by remember { mutableStateOf(splitAddresses(draft.to)) }
    var cc by remember { mutableStateOf(splitAddresses(draft.cc)) }
    var bcc by remember { mutableStateOf(splitAddresses(draft.bcc)) }
    var typingTo by remember { mutableStateOf("") }
    var typingCc by remember { mutableStateOf("") }
    var typingBcc by remember { mutableStateOf("") }
    var showCcBcc by remember { mutableStateOf(draft.cc.isNotBlank() || draft.bcc.isNotBlank()) }
    var subject by remember { mutableStateOf(draft.subject) }

    val signatureContext = LocalContext.current
    val openingBody = remember {
        val signature = if (draft.editingUid == null) MailSignature.text(signatureContext) else null
        if (signature == null) draft.body else draft.body + "\n\n" + signature
    }
    var body by remember { mutableStateOf(openingBody) }

    fun wrote() = body.isNotBlank() && body != openingBody
    var attachments by remember { mutableStateOf(draft.sharedAttachments) }
    var sending by remember { mutableStateOf(false) }
    var confirmingDiscard by remember { mutableStateOf(false) }

    var discarded by remember { mutableStateOf(false) }

    var restoringAttachments by remember { mutableStateOf(draft.existingAttachments.isNotEmpty()) }

    var pendingAttachments by remember { mutableStateOf(draft.existingAttachments) }

    var attachmentsOpen by remember { mutableStateOf(draft.attachmentsUid == null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sentMessage = stringResource(R.string.mail_compose_sent)
    val sendFailedMessage = stringResource(R.string.mail_compose_send_failed)
    val noRecipientMessage = stringResource(R.string.mail_compose_no_recipient)

    val attachmentsMissing = stringResource(R.string.mail_compose_attachments_missing)
    LaunchedEffect(draft.editingUid, draft.attachmentsUid) {
        val folder = draft.editingFolder ?: draft.attachmentsFolder
        val uid = draft.editingUid ?: draft.attachmentsUid
        if (folder == null || uid == null) {
            restoringAttachments = false
            pendingAttachments = emptyList()
            return@LaunchedEffect
        }

        val started = android.os.SystemClock.uptimeMillis()
        val fetched = withContext(Dispatchers.IO) {
            withTimeoutOrNull(90_000) {
                runCatching {
                    OutlookImap.downloadAttachments(fromEmail, accessToken, folder, uid, draft.existingAttachments.map { it.path })
                }.onFailure { android.util.Log.w("MailCompose", "attachments failed: $it") }.getOrNull()
            }.orEmpty()
        }
        val restored = draft.existingAttachments.map { attachment ->
            fetched[attachment.path]?.let { (filename, bytes) -> OutgoingAttachment(filename, attachment.mimeType, bytes) }
        }
        val kept = restored.filterNotNull()
        android.util.Log.i(
            "MailCompose",
            "asked for " + draft.existingAttachments.joinToString { "'${it.filename}' ${it.mimeType} ${it.sizeBytes} ${it.path}" } +
                ", got " + kept.joinToString { "'${it.filename}' ${it.mimeType} ${it.bytes.size}" },
        )
        android.util.Log.i("MailCompose", "attachments ${kept.size} of ${draft.existingAttachments.size} in ${android.os.SystemClock.uptimeMillis() - started} ms")
        attachments = attachments + kept
        restoringAttachments = false
        pendingAttachments = emptyList()
        if (kept.size < draft.existingAttachments.size) AppFeedback.show(attachmentsMissing)
    }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val picked = withContext(Dispatchers.IO) { uris.mapNotNull { readAttachment(context, it) } }
            attachments = attachments + picked

            if (picked.isNotEmpty()) attachmentsOpen = true
        }
    }

    fun forgetOriginalDraft() {
        val folder = draft.editingFolder ?: return
        val uid = draft.editingUid ?: return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { OutlookImap.deleteMessage(fromEmail, accessToken, folder, uid) }
            android.util.Log.i("MailDbg", "forgetOriginalDraft folder=$folder uid=$uid -> $result")
        }
    }

    fun send() {
        val toAddresses = to + splitAddresses(typingTo)
        val ccAddresses = cc + splitAddresses(typingCc)
        val bccAddresses = bcc + splitAddresses(typingBcc)
        if (toAddresses.isEmpty() && ccAddresses.isEmpty() && bccAddresses.isEmpty()) {
            AppFeedback.show(noRecipientMessage)
            return
        }
        sending = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    OutlookSmtp.send(
                        fromEmail = fromEmail,
                        fromName = app.etsprivate.settings.StudentName.outgoing(context),
                        accessToken = accessToken,
                        to = toAddresses,
                        cc = ccAddresses,
                        bcc = bccAddresses,
                        subject = subject,
                        body = fullText(body, draft),
                        htmlBody = fullHtml(body, draft),
                        attachments = attachments,
                        inReplyTo = draft.inReplyTo,
                        references = draft.references,
                    )
                }
                discarded = true
                forgetOriginalDraft()

                MailContacts.remember(context, toAddresses + ccAddresses + bccAddresses)
                AppFeedback.show(sentMessage)
                onSent()
            } catch (e: Exception) {
                AppFeedback.show(sendFailedMessage)
            } finally {
                sending = false
            }
        }
    }

    val savedMessage = stringResource(R.string.mail_draft_saved)
    val deleteLabel = stringResource(R.string.mail_draft_delete)
    val deletedMessage = stringResource(R.string.mail_draft_deleted)
    val notDeletedMessage = stringResource(R.string.mail_draft_not_deleted)

    fun keepAsDraft() {
        if (discarded || sending) return
        val hasContent = (to + cc + bcc).isNotEmpty() ||
            listOf(typingTo, typingCc, typingBcc, subject).any { it.isNotBlank() } || wrote() ||
            draft.quotedText.isNotBlank() || attachments.isNotEmpty()
        if (!hasContent) {
            forgetOriginalDraft()
            return
        }

        val openedWith = (draft.existingAttachments.map { it.filename } + draft.sharedAttachments.map { it.filename }).sorted()
        val nowHolds = (attachments.map { it.filename } + pendingAttachments.map { it.filename }).sorted()
        val unchanged = to + splitAddresses(typingTo) == splitAddresses(draft.to) &&
            cc + splitAddresses(typingCc) == splitAddresses(draft.cc) &&
            bcc + splitAddresses(typingBcc) == splitAddresses(draft.bcc) &&
            subject == draft.subject &&
            body == openingBody &&
            nowHolds == openedWith
        if (unchanged) return
        val text = fullText(body, draft)
        val html = fullHtml(body, draft)

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                OutlookImap.saveDraft(
                    email = fromEmail,
                    accessToken = accessToken,
                    to = to + splitAddresses(typingTo),
                    cc = cc + splitAddresses(typingCc),
                    bcc = bcc + splitAddresses(typingBcc),
                    subject = subject,
                    body = text,
                    htmlBody = html,
                )
            }.onSuccess { saved ->

                if (saved == null) {
                    AppFeedback.show(savedMessage)
                } else {
                    AppFeedback.show(savedMessage, actionLabel = deleteLabel) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            val moved = runCatching { OutlookImap.deleteMessage(fromEmail, accessToken, saved.first, saved.second) }
                            AppFeedback.show(if (moved.isSuccess) deletedMessage else notDeletedMessage)
                        }
                    }
                }
                forgetOriginalDraft()
            }
        }
    }

    fun leave() {
        keepAsDraft()
        onBack()
    }

    BackHandler(onBack = ::leave)
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AppTopBar(
            title = "",
            leading = {
                IconButton(onClick = ::leave) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            },
            actions = {
                IconButton(onClick = { pickFiles.launch("*/*") }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = stringResource(R.string.mail_add_attachment))
                }
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = Spacing.oneHalf).size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { send() }, enabled = !restoringAttachments) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.action_send))
                    }
                }
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.mail_discard_draft)) },
                            onClick = {
                                menu = false

                                val written = (to + cc + bcc).isNotEmpty() ||
                                    listOf(typingTo, typingCc, typingBcc, subject).any { it.isNotBlank() } || wrote() ||
                                    attachments.isNotEmpty()
                                if (written) {
                                    confirmingDiscard = true
                                } else {
                                    discarded = true
                                    forgetOriginalDraft()
                                    onBack()
                                }
                            },
                        )
                    }
                }
            },
        )
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            HeaderRow(label = stringResource(R.string.mail_from)) {
                Text(
                    fromEmail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RecipientField(
                label = stringResource(R.string.mail_to),
                recipients = to,
                onRecipientsChange = { to = it },
                typed = typingTo,
                onTypedChange = { typingTo = it },

                trailing = {
                    IconButton(onClick = { showCcBcc = !showCcBcc }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (showCcBcc) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = stringResource(R.string.mail_add_cc_bcc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            if (showCcBcc) {
                RecipientField(
                    label = stringResource(R.string.mail_field_cc),
                    recipients = cc,
                    onRecipientsChange = { cc = it },
                    typed = typingCc,
                    onTypedChange = { typingCc = it },
                )
                RecipientField(
                    label = stringResource(R.string.mail_field_bcc),
                    recipients = bcc,
                    onRecipientsChange = { bcc = it },
                    typed = typingBcc,
                    onTypedChange = { typingBcc = it },
                )
            }
            HeaderField(
                label = stringResource(R.string.mail_field_subject),
                value = subject,
                onValueChange = { subject = it },
            )

            val attachmentCount = pendingAttachments.size + attachments.size
            if (attachmentCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .bleed(Spacing.half)
                        .clip(AppShapes.small)
                        .clickable { attachmentsOpen = !attachmentsOpen }
                        .padding(horizontal = Spacing.half, vertical = Spacing.default),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(
                        pluralStringResource(R.plurals.mail_attachment_count, attachmentCount, attachmentCount),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).padding(start = Spacing.half),
                    )
                    Icon(
                        if (attachmentsOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (attachmentsOpen) {
                    pendingAttachments.forEach { pending ->
                        AttachmentLine(pending.filename, pending.sizeBytes, loading = true, onRemove = null)
                    }
                    attachments.forEachIndexed { index, attachment ->
                        AttachmentLine(attachment.filename, attachment.bytes.size.toLong(), loading = false) {
                            attachments = attachments.toMutableList().also { it.removeAt(index) }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            BasicTextField(
                value = body,
                onValueChange = { body = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { field ->
                    if (bodyText(body).isEmpty()) {
                        Text(stringResource(R.string.mail_compose_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    field()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .padding(horizontal = 16.dp, vertical = Spacing.oneHalf),
            )

            if (draft.quotedText.isNotBlank() || draft.quotedHtml != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                draft.quoteHeader?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = Spacing.default),
                    )
                }
                QuotedMessage(
                    html = draft.quotedHtml,
                    text = draft.quotedText,
                    images = draft.quotedImages,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmingDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.mail_discard_draft_title),
            onDismissRequest = { confirmingDiscard = false },
            confirmLabel = stringResource(R.string.mail_discard_draft),
            onConfirm = {
                confirmingDiscard = false
                discarded = true
                forgetOriginalDraft()
                onBack()
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {}
    }
}

private fun bodyText(body: Any): String = if (body is androidx.compose.ui.text.input.TextFieldValue) body.text else body.toString()

private fun splitAddresses(raw: String): List<String> = raw.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }

internal fun readAttachment(context: Context, uri: Uri): OutgoingAttachment? = runCatching {
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    var filename = "attachment"
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) filename = cursor.getString(nameIndex)
    }
    OutgoingAttachment(filename, mimeType, bytes)
}.getOrNull()

@Composable
private fun HeaderRow(label: String, trailing: (@Composable () -> Unit)? = null, field: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label.isNotEmpty()) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 53.dp),
            )
        }
        field()
        trailing?.invoke()
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun HeaderField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    HeaderRow(label = "", trailing = trailing) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { field ->
                if (value.isEmpty()) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                field()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipientField(
    label: String,
    recipients: List<String>,
    onRecipientsChange: (List<String>) -> Unit,
    typed: String,
    onTypedChange: (String) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val suggestions = remember(typed, recipients) {
        MailContacts.matching(context, typed).filterNot { contact ->
            recipients.any { it.contains(contact.address, ignoreCase = true) }
        }
    }

    val focus = remember { FocusRequester() }

    fun settle(raw: String) {
        val cleaned = raw.trim().trim(',', ';').trim()
        if (cleaned.isEmpty()) return
        if (recipients.none { it.equals(cleaned, ignoreCase = true) }) onRecipientsChange(recipients + cleaned)
        onTypedChange("")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { focus.requestFocus() }
            .padding(horizontal = 16.dp, vertical = Spacing.half),

        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.height(32.dp).widthIn(min = 53.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.quarter),
        ) {
            recipients.forEach { recipient ->

                Box(
                    modifier = Modifier.height(32.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                InputChip(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    selected = false,
                    onClick = { onRecipientsChange(recipients - recipient) },
                    label = { Text(recipient.substringBefore('<').trim().ifBlank { recipient }, maxLines = 1) },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
            BasicTextField(
                value = typed,
                onValueChange = { text ->

                    if (text.endsWith(",") || text.endsWith(";")) settle(text) else onTypedChange(text)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onDone = { settle(typed) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 120.dp)
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false
                            event.key == Key.Enter -> { settle(typed); true }

                            event.key == Key.Backspace && typed.isEmpty() && recipients.isNotEmpty() -> {
                                onRecipientsChange(recipients.dropLast(1))
                                true
                            }
                            else -> false
                        }
                    },
            )
            }
        }

        trailing?.let {
            Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) { it() }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    suggestions.forEach { contact ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { settle(contact.display) }
                .padding(horizontal = 16.dp, vertical = Spacing.default),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.default)) {
                if (contact.name.isNotBlank()) {
                    Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    contact.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            contact.from?.let { course ->
                Text(
                    course,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun QuotedMessage(
    html: String?,
    text: String,
    images: Map<String, MailInlineImage>,
    modifier: Modifier = Modifier,
) {
    var height by remember(html, text) { mutableStateOf(240.dp) }

    val nestedScroll = rememberNestedScrollInteropConnection()

    val document = remember(html, text) {
        wrapEmailHtml(html ?: "<pre style=\"white-space:pre-wrap;font-family:inherit\">${escapeHtml(text)}</pre>")
    }
    Row(modifier = modifier.height(height)) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = Spacing.half, bottom = Spacing.half)
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = Spacing.default)
                .nestedScroll(nestedScroll),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.WHITE)
                    settings.javaScriptEnabled = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    isVerticalScrollBarEnabled = false
                    isNestedScrollingEnabled = true
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            listOf(0L, 250L, 800L).forEach { delay ->
                                view?.postDelayed({
                                    val measured = view.contentHeight
                                    if (measured > 0) height = maxOf(height, (measured + 16).dp)
                                }, delay)
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                        ): android.webkit.WebResourceResponse? {
                            val url = request?.url?.toString() ?: return null
                            if (!url.startsWith("cid:")) return null
                            val image = images[url.removePrefix("cid:")] ?: return null
                            val bytes = android.util.Base64.decode(image.base64, android.util.Base64.NO_WRAP)
                            return android.webkit.WebResourceResponse(
                                image.mimeType,
                                "utf-8",
                                java.io.ByteArrayInputStream(bytes),
                            )
                        }
                    }
                }
            },
            update = { view ->
                if (view.tag != document) {
                    view.tag = document
                    view.loadDataWithBaseURL(null, document, "text/html", "UTF-8", null)
                }
            },
        )
    }
}

internal fun escapeHtml(raw: String): String = raw
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun fullText(written: String, draft: ComposeDraft): String = buildString {
    append(written)
    if (draft.quotedText.isBlank()) return@buildString
    append("\n\n")
    draft.quoteHeader?.let { append(it).append('\n') }
    append(draft.quotedText.lines().joinToString("\n") { "> $it" })
}

private fun fullHtml(written: String, draft: ComposeDraft): String? {
    val quotes = draft.quotedText.isNotBlank() || draft.quotedHtml != null
    val writtenHtml = MailSignature.linked(escapeHtml(written).replace("\n", "<br>"))
    if (!quotes && writtenHtml == null) return null
    return buildString {
        append("<html><body>")
        append(writtenHtml ?: escapeHtml(written).replace("\n", "<br>"))
        if (!quotes) {
            append("</body></html>")
            return@buildString
        }
        val quoted = draft.quotedHtml ?: "<pre style=\"white-space:pre-wrap;font-family:inherit\">${escapeHtml(draft.quotedText)}</pre>"
        draft.quoteHeader?.let { append("<p>").append(escapeHtml(it)).append("</p>") }
        append("<blockquote style=\"margin:0 0 0 8px;padding-left:12px;border-left:2px solid #888\">")
        append(quoted)
        append("</blockquote></body></html>")
    }
}

@Composable
private fun AttachmentLine(name: String, size: Long, loading: Boolean, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = Spacing.half, top = Spacing.quarter, bottom = Spacing.quarter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = Spacing.half),
        )
        Text(
            formatAttachmentSize(size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.half),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_cancel))
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

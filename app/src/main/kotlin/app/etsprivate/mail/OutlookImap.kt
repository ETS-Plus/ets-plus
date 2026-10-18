package app.etsprivate.mail

import android.util.Base64
import app.etsprivate.cache.AuthRequired
import com.sun.mail.imap.IMAPFolder
import java.util.Properties
import javax.mail.FetchProfile
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimePart

data class MailMessage(
    val uid: Long,
    val subject: String,
    val from: String,
    val date: Long,
    val isRead: Boolean,
    val hasAttachment: Boolean,

    val fromAddress: String = "",

    val isStarred: Boolean = false,

    val preview: String = "",
)

data class MailAddress(val name: String, val address: String) {
    val display: String get() = name.ifBlank { address }
}

data class MailAttachment(val filename: String, val mimeType: String, val sizeBytes: Long, val path: String)

data class MailInlineImage(val mimeType: String, val base64: String)

data class MailBody(
    val text: String,
    val html: String?,
    val from: MailAddress?,
    val to: List<MailAddress>,
    val cc: List<MailAddress>,

    val bcc: List<MailAddress>,
    val attachments: List<MailAttachment>,
    val inlineImages: Map<String, MailInlineImage>,

    val messageId: String?,
)

enum class MailFolderRole { INBOX, SENT, DRAFTS, TRASH, JUNK, ARCHIVE, OTHER }

data class MailFolderInfo(val fullName: String, val role: MailFolderRole)

object OutlookImap {
    class AuthException(message: String) : Exception(message), AuthRequired

    private const val HOST = "outlook.office365.com"

    private val ROLE_ATTRIBUTES = mapOf(
        "\\Inbox" to MailFolderRole.INBOX,
        "\\Sent" to MailFolderRole.SENT,
        "\\Drafts" to MailFolderRole.DRAFTS,
        "\\Trash" to MailFolderRole.TRASH,
        "\\Junk" to MailFolderRole.JUNK,
        "\\Archive" to MailFolderRole.ARCHIVE,
    )
    private val ROLE_NAMES = mapOf(
        "Inbox" to MailFolderRole.INBOX,
        "Sent Items" to MailFolderRole.SENT,
        "Sent" to MailFolderRole.SENT,
        "Drafts" to MailFolderRole.DRAFTS,
        "Deleted Items" to MailFolderRole.TRASH,
        "Trash" to MailFolderRole.TRASH,
        "Junk Email" to MailFolderRole.JUNK,
        "Junk" to MailFolderRole.JUNK,
        "Spam" to MailFolderRole.JUNK,
        "Archive" to MailFolderRole.ARCHIVE,
    )

    private class Pooled(val key: String, val store: Store) {
        var users = 0
        var closing: java.util.concurrent.ScheduledFuture<*>? = null
    }
    private val pool = HashMap<String, Pooled>()
    private val leased = java.util.IdentityHashMap<Store, Pooled>()
    private val closer = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()

    private fun connect(email: String, accessToken: String): Store {
        val key = "$email\n$accessToken"
        synchronized(pool) {
            val held = pool[key]
            if (held != null && held.store.isConnected) {
                held.closing?.cancel(false)
                held.users++
                return held.store
            }
            if (held != null) pool.remove(key)
        }
        val store = openStore(email, accessToken)
        synchronized(pool) {
            val pooled = Pooled(key, store).also { it.users = 1 }
            pool[key] = pooled
            leased[store] = pooled
        }
        return store
    }

    private fun release(store: Store) {
        synchronized(pool) {
            val pooled = leased[store] ?: run { runCatching { store.close() }; return }
            pooled.users--
            if (pooled.users > 0) return
            pooled.closing = closer.schedule({
                synchronized(pool) {
                    if (pooled.users == 0) {
                        if (pool[pooled.key] === pooled) pool.remove(pooled.key)
                        leased.remove(store)
                        runCatching { store.close() }
                    }
                }
            }, 60, java.util.concurrent.TimeUnit.SECONDS)
        }
    }

    private fun openStore(email: String, accessToken: String): Store {
        val props = Properties().apply {
            put("mail.mime.decodetext.strict", "false")
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.port", "993")
            put("mail.imap.auth.mechanisms", "XOAUTH2")

            put("mail.imap.fetchsize", (4 * 1024 * 1024).toString())
        }
        val store = Session.getInstance(props).getStore("imap")
        connectWithRetry(store, email, accessToken, retriesLeft = 1)
        return store
    }

    private fun connectWithRetry(store: Store, email: String, accessToken: String, retriesLeft: Int) {
        try {
            store.connect(HOST, email, accessToken)
        } catch (e: javax.mail.MessagingException) {
            if (e.cause is java.io.IOException && retriesLeft > 0) {
                connectWithRetry(store, email, accessToken, retriesLeft - 1)
                return
            }
            if (e.cause is java.io.IOException) {
                throw java.io.IOException(e.message ?: "Couldn't reach the mail server", e)
            }
            throw AuthException(e.message ?: "Authentication failed")
        }
    }

    private fun folderRole(folder: Folder): MailFolderRole {
        val attrs = (folder as? IMAPFolder)?.attributes.orEmpty()
        return ROLE_ATTRIBUTES.entries.firstOrNull { (attr, _) -> attrs.any { it.equals(attr, ignoreCase = true) } }?.value
            ?: ROLE_NAMES.entries.firstOrNull { (name, _) -> folder.name.equals(name, ignoreCase = true) }?.value
            ?: MailFolderRole.OTHER
    }

    private fun copyKeepingUids(folder: IMAPFolder, pairs: List<Pair<Long, Message>>, dest: Folder): Map<Long, Long> {
        val ids = pairs.associate { (uid, m) -> uid to runCatching { (m as? javax.mail.internet.MimeMessage)?.messageID }.getOrNull() }
        val appended = folder.copyUIDMessages(pairs.map { it.second }.toTypedArray(), dest)
        val landed = HashMap<Long, Long>()
        pairs.forEachIndexed { i, (uid, _) -> appended.getOrNull(i)?.uid?.takeIf { it > 0 }?.let { landed[uid] = it } }
        val missing = pairs.map { it.first }.filter { it !in landed && ids[it] != null }
        if (missing.isEmpty()) return landed
        val target = dest as IMAPFolder
        runCatching {
            target.open(Folder.READ_ONLY)
            try {
                missing.forEach { uid ->
                    target.search(javax.mail.search.HeaderTerm("Message-ID", ids[uid]!!)).lastOrNull()?.let { landed[uid] = target.getUID(it) }
                }
            } finally {
                target.close(false)
            }
        }
        return landed
    }

    fun folderNameForRole(email: String, accessToken: String, role: MailFolderRole): String? {
        val store = connect(email, accessToken)
        try {
            return findFolderByRole(store, role)?.fullName
        } finally {
            release(store)
        }
    }

    private fun findFolderByRole(store: Store, role: MailFolderRole): Folder? =
        store.defaultFolder.list("*").firstOrNull { folderRole(it) == role }

    fun listFolders(email: String, accessToken: String): List<MailFolderInfo> {
        app.etsprivate.demo.Demo.source?.let { return it.folders() }
        val store = connect(email, accessToken)
        try {
            return store.defaultFolder.list("*")
                .filter { (it.type and Folder.HOLDS_MESSAGES) != 0 }
                .map { MailFolderInfo(it.fullName, folderRole(it)) }
        } finally {
            release(store)
        }
    }

    fun fetchFolder(email: String, accessToken: String, folderName: String, limit: Int = 25, withPreviews: Boolean = true): List<MailMessage> {
        app.etsprivate.demo.Demo.source?.let { return it.messages(folderName) }
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val messages = folder.messages
                val start = maxOf(0, messages.size - limit)
                val slice = messages.copyOfRange(start, messages.size)
                folder.fetch(
                    slice,
                    FetchProfile().apply {
                        add(FetchProfile.Item.ENVELOPE)
                        add(FetchProfile.Item.FLAGS)
                        add(FetchProfile.Item.CONTENT_INFO)
                    },
                )

                val opening = if (withPreviews) previews(folder, slice) else emptyMap()
                return slice.mapNotNull { message -> toMailMessage(folder, message, opening[message].orEmpty()) }.sortedByDescending { it.date }
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    private fun toMailMessage(folder: IMAPFolder, message: Message, preview: String = ""): MailMessage? = runCatching {
        val uid = folder.getUID(message).takeIf { it > 0 } ?: return null
        MailMessage(
            uid = uid,
            subject = message.subject ?: "(no subject)",
            from = (message.from?.firstOrNull() as? InternetAddress)?.displayName() ?: message.from?.firstOrNull()?.toString().orEmpty(),
            date = message.sentDate?.time ?: 0L,
            isRead = message.flags.contains(Flags.Flag.SEEN),

            hasAttachment = runCatching { message.contentType }.getOrNull()?.startsWith("multipart/mixed", ignoreCase = true) == true,
            fromAddress = (message.from?.firstOrNull() as? InternetAddress)?.address.orEmpty(),
            isStarred = message.flags.contains(Flags.Flag.FLAGGED),
            preview = preview,
        )
    }.getOrNull()

    private class TextPart(val section: String, val encoding: String?, val charset: String, val html: Boolean)

    private val sectionField by lazy {
        com.sun.mail.imap.IMAPBodyPart::class.java.getDeclaredField("sectionId").apply { isAccessible = true }
    }

    private fun textPart(message: Message): TextPart? {
        fun of(part: Part, section: String): TextPart {
            val type = javax.mail.internet.ContentType(part.contentType)
            return TextPart(
                section,
                (part as? MimePart)?.encoding,
                type.getParameter("charset") ?: "UTF-8",
                type.match("text/html"),
            )
        }
        if (message.isMimeType("text/plain") || message.isMimeType("text/html")) return of(message, "TEXT")
        if (!message.isMimeType("multipart/*")) return null
        var html: TextPart? = null
        fun walk(multipart: Multipart): TextPart? {
            for (i in 0 until multipart.count) {
                val part = multipart.getBodyPart(i)
                if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) continue
                val section = (part as? com.sun.mail.imap.IMAPBodyPart)?.let { sectionField.get(it) as? String } ?: continue
                when {
                    part.isMimeType("multipart/*") -> walk(part.content as Multipart)?.let { return it }
                    part.isMimeType("text/plain") -> return of(part, section)
                    part.isMimeType("text/html") && html == null -> html = of(part, section)
                }
            }
            return null
        }
        return walk(message.content as Multipart) ?: html
    }

    private fun previews(folder: IMAPFolder, messages: Array<out Message>): Map<Message, String> {
        val plans = messages.mapNotNull { m -> runCatching { textPart(m) }.getOrNull()?.let { m to it } }
        val out = HashMap<Message, String>()
        plans.groupBy { it.second.section }.forEach { (section, group) ->
            val byNumber = group.associateBy { it.first.messageNumber }
            runCatching {
                folder.doCommand { protocol ->
                    val responses = protocol.command(
                        "FETCH ${byNumber.keys.joinToString(",")} (BODY.PEEK[$section]<0.3000>)",
                        null,
                    )
                    responses.forEach { response ->
                        if (response is com.sun.mail.imap.protocol.FetchResponse) {
                            val body = response.getItem(com.sun.mail.imap.protocol.BODY::class.java)
                            val plan = byNumber[response.number]
                            val bytes = body?.byteArray?.let { it.bytes.copyOfRange(it.start, it.start + it.count) }
                            if (plan != null && bytes != null) out[plan.first] = previewText(bytes, plan.second)
                        }
                    }
                    protocol.notifyResponseHandlers(responses)
                    protocol.handleResult(responses.last())
                    null
                }
            }
        }
        return out
    }

    private fun previewText(raw: ByteArray, part: TextPart): String {
        val decoded = when (part.encoding?.lowercase()) {
            "base64" -> {
                val clean = raw.filter { !it.toInt().toChar().isWhitespace() }.toByteArray()
                runCatching { Base64.decode(clean.copyOf(clean.size - clean.size % 4), Base64.DEFAULT) }.getOrDefault(ByteArray(0))
            }
            "quoted-printable" -> runCatching {
                javax.mail.internet.MimeUtility.decode(java.io.ByteArrayInputStream(raw.copyOf((raw.size - 3).coerceAtLeast(0))), "quoted-printable").readBytes()
            }.getOrDefault(raw)
            else -> raw
        }
        var text = runCatching { String(decoded, charset(part.charset)) }.getOrElse { String(decoded) }
        if (part.html) {
            text = text.replace(Regex("(?is)<(style|script|head|title)[^>]*>.*?(</\\1>|$)"), " ")
            text = androidx.core.text.HtmlCompat.fromHtml(text, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        }
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith(">") }
            .joinToString(" ")

            .replace(Regex("<?https?://\\S+>?"), " ")
            .replace(Regex("\\*\\*|__|->|\\[|\\]|\\(\\s*\\)"), " ")
            .replace(Regex("\\s+"), " ")
            .replace("\uFFFC", "")
            .trim()
            .take(200)
    }

    fun search(email: String, accessToken: String, folderName: String, query: String, limit: Int = 100, inText: Boolean = true): List<MailMessage> {
        app.etsprivate.demo.Demo.source?.let { demo ->
            return demo.messages(folderName).filter { it.subject.contains(query, true) || it.from.contains(query, true) }
        }
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val terms = listOfNotNull<javax.mail.search.SearchTerm>(
                    javax.mail.search.SubjectTerm(query),
                    javax.mail.search.FromStringTerm(query),
                    if (inText) javax.mail.search.BodyTerm(query) else null,
                )
                val found = folder.search(javax.mail.search.OrTerm(terms.toTypedArray()))
                val slice = found.takeLast(limit).toTypedArray()
                folder.fetch(
                    slice,
                    FetchProfile().apply {
                        add(FetchProfile.Item.ENVELOPE)
                        add(FetchProfile.Item.FLAGS)
                        add(FetchProfile.Item.CONTENT_INFO)
                    },
                )
                val opening = previews(folder, slice)
                return slice.mapNotNull { toMailMessage(folder, it, opening[it].orEmpty()) }.sortedByDescending { it.date }
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    fun fetchOlder(email: String, accessToken: String, folderName: String, beforeUid: Long, limit: Int = 25): List<MailMessage> {
        app.etsprivate.demo.Demo.source?.let { return emptyList() }
        if (beforeUid <= 1) return emptyList()
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val older = folder.getMessagesByUID(1, beforeUid - 1).filterNotNull()
                val slice = older.takeLast(limit).toTypedArray()
                folder.fetch(
                    slice,
                    FetchProfile().apply {
                        add(FetchProfile.Item.ENVELOPE)
                        add(FetchProfile.Item.FLAGS)
                        add(FetchProfile.Item.CONTENT_INFO)
                    },
                )

                val opening = emptyMap<Message, String>()
                return slice.mapNotNull { toMailMessage(folder, it, opening[it].orEmpty()) }.sortedByDescending { it.date }
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    fun previewsFor(email: String, accessToken: String, folderName: String, uids: List<Long>): Map<Long, String> {
        app.etsprivate.demo.Demo.source?.let { return emptyMap() }
        if (uids.isEmpty()) return emptyMap()
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val found = folder.getMessagesByUID(uids.toLongArray()).filterNotNull().toTypedArray()
                folder.fetch(found, FetchProfile().apply { add(FetchProfile.Item.CONTENT_INFO) })
                val opening = previews(folder, found)
                return found.associate { folder.getUID(it) to opening[it].orEmpty() }
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    fun emptyFolder(email: String, accessToken: String, folderName: String) {
        app.etsprivate.demo.Demo.source?.let { return }
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_WRITE)
            try {
                val all = folder.messages
                if (all.isNotEmpty()) folder.setFlags(all, Flags(Flags.Flag.DELETED), true)
            } finally {
                folder.close(true)
            }
        } finally {
            release(store)
        }
    }

    fun unreadCounts(email: String, accessToken: String, folderNames: List<String>): Map<String, Int> {
        app.etsprivate.demo.Demo.source?.let { return emptyMap() }
        val store = connect(email, accessToken)
        try {
            return folderNames.associateWith { name ->
                runCatching { store.getFolder(name).unreadMessageCount }.getOrDefault(0).coerceAtLeast(0)
            }
        } finally {
            release(store)
        }
    }

    fun setStarred(email: String, accessToken: String, folderName: String, uids: List<Long>, starred: Boolean) {
        app.etsprivate.demo.Demo.source?.let { return }
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_WRITE)
            try {
                uids.forEach { uid -> folder.getMessageByUID(uid)?.setFlag(Flags.Flag.FLAGGED, starred) }
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    fun setRead(email: String, accessToken: String, folderName: String, uid: Long, read: Boolean) =
        setReadMessages(email, accessToken, folderName, listOf(uid), read)

    fun setReadMessages(email: String, accessToken: String, folderName: String, uids: List<Long>, read: Boolean) {
        app.etsprivate.demo.Demo.source?.let { return }
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_WRITE)
            try {
                uids.forEach { uid -> folder.getMessageByUID(uid)?.setFlag(Flags.Flag.SEEN, read) }
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    fun deleteMessage(email: String, accessToken: String, folderName: String, uid: Long): Map<Long, Long> =
        deleteMessages(email, accessToken, folderName, listOf(uid))

    fun deleteMessages(email: String, accessToken: String, folderName: String, uids: List<Long>): Map<Long, Long> {
        val store = connect(email, accessToken)
        return try {
            val trash = findFolderByRole(store, MailFolderRole.TRASH)
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_WRITE)
            try {
                val pairs = uids.mapNotNull { uid -> folder.getMessageByUID(uid)?.let { uid to it } }
                android.util.Log.i(
                    "MailDbg",
                    "deleteMessages folder=$folderName asked=$uids found=${pairs.map { it.first }} " +
                        "trash=${trash?.fullName}",
                )
                if (pairs.isEmpty()) return emptyMap()
                val newUidByOld = if (trash != null && trash.fullName != folderName) {
                    pairs.forEach { runCatching { it.second.setFlag(Flags.Flag.SEEN, true) } }

                    copyKeepingUids(folder, pairs, trash)
                } else {
                    emptyMap()
                }
                pairs.forEach { it.second.setFlag(Flags.Flag.DELETED, true) }
                newUidByOld
            } finally {
                folder.close(true)
            }
        } finally {
            release(store)
        }
    }

    fun archiveMessage(email: String, accessToken: String, folderName: String, uid: Long): Map<Long, Long> =
        archiveMessages(email, accessToken, folderName, listOf(uid))

    fun archiveMessages(email: String, accessToken: String, folderName: String, uids: List<Long>): Map<Long, Long> {
        val store = connect(email, accessToken)
        return try {
            val archive = findFolderByRole(store, MailFolderRole.ARCHIVE) ?: return emptyMap()
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_WRITE)
            try {
                val pairs = uids.mapNotNull { uid -> folder.getMessageByUID(uid)?.let { uid to it } }
                if (pairs.isEmpty()) return emptyMap()
                val newUidByOld = copyKeepingUids(folder, pairs, archive)
                pairs.forEach { it.second.setFlag(Flags.Flag.DELETED, true) }
                newUidByOld
            } finally {
                folder.close(true)
            }
        } finally {
            release(store)
        }
    }

    fun moveMessage(email: String, accessToken: String, folderName: String, uid: Long, targetFolderName: String): Map<Long, Long> =
        moveMessages(email, accessToken, folderName, listOf(uid), targetFolderName)

    fun moveMessages(email: String, accessToken: String, folderName: String, uids: List<Long>, targetFolderName: String): Map<Long, Long> {
        val store = connect(email, accessToken)
        return try {
            val target = store.getFolder(targetFolderName)
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_WRITE)
            try {
                val pairs = uids.mapNotNull { uid -> folder.getMessageByUID(uid)?.let { uid to it } }
                if (pairs.isEmpty()) return emptyMap()
                val newUidByOld = copyKeepingUids(folder, pairs, target)
                pairs.forEach { it.second.setFlag(Flags.Flag.DELETED, true) }
                newUidByOld
            } finally {
                folder.close(true)
            }
        } finally {
            release(store)
        }
    }

    fun saveDraft(
        email: String,
        accessToken: String,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        body: String,
        htmlBody: String?,
    ): Pair<String, Long>? {
        app.etsprivate.demo.Demo.source?.let { return null }
        val store = connect(email, accessToken)
        try {
            val drafts = findFolderByRole(store, MailFolderRole.DRAFTS) ?: return null
            val message = OutlookSmtp.compose(
                fromEmail = email,
                to = to,
                cc = cc,
                bcc = bcc,
                subject = subject,
                body = body,
                htmlBody = htmlBody,
                attachments = emptyList(),
            ).apply {
                setFlag(Flags.Flag.DRAFT, true)
                setFlag(Flags.Flag.SEEN, true)
            }
            drafts.open(Folder.READ_WRITE)
            try {
                val appended = (drafts as? IMAPFolder)?.appendUIDMessages(arrayOf(message))
                    ?: run { drafts.appendMessages(arrayOf(message)); null }
                return appended?.firstOrNull()?.let { drafts.fullName to it.uid }
            } finally {
                drafts.close(false)
            }
        } finally {
            runCatching { release(store) }
        }
    }

    fun fetchBody(email: String, accessToken: String, folderName: String, uid: Long): MailBody {
        app.etsprivate.demo.Demo.source?.let { return it.body(uid) }
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val message = folder.getMessageByUID(uid)
                    ?: throw AuthException("Message not found")
                val parts = BodyParts()
                walkParts(message, "", parts)
                android.util.Log.i(
                    "MailBody",
                    "uid=$uid type=${runCatching { message.contentType }.getOrNull()} size=${runCatching { message.size }.getOrNull()} " +
                        "html=${parts.html?.length} text=${parts.text?.length} attachments=${parts.attachments.size} images=${parts.inlineImages.size}",
                )
                return MailBody(
                    text = parts.text ?: parts.html.orEmpty(),
                    html = parts.html,
                    from = runCatching { (message.from?.firstOrNull() as? InternetAddress)?.toMailAddress() }.getOrNull(),
                    to = runCatching {
                        message.getRecipients(Message.RecipientType.TO).orEmpty().filterIsInstance<InternetAddress>().map { it.toMailAddress() }
                    }.getOrDefault(emptyList()),
                    cc = runCatching {
                        message.getRecipients(Message.RecipientType.CC).orEmpty().filterIsInstance<InternetAddress>().map { it.toMailAddress() }
                    }.getOrDefault(emptyList()),
                    bcc = runCatching {
                        message.getRecipients(Message.RecipientType.BCC).orEmpty().filterIsInstance<InternetAddress>().map { it.toMailAddress() }
                    }.getOrDefault(emptyList()),
                    attachments = parts.attachments,
                    inlineImages = parts.inlineImages,
                    messageId = runCatching { (message as? javax.mail.internet.MimeMessage)?.messageID }.getOrNull(),
                )
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    fun downloadAttachments(email: String, accessToken: String, folderName: String, uid: Long, paths: List<String>): Map<String, Pair<String, ByteArray>> {
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val message = folder.getMessageByUID(uid) ?: throw AuthException("Message not found")
                return paths.mapNotNull { path ->
                    runCatching {
                        val part = resolvePart(message, path) ?: return@mapNotNull null
                        path to (sanitizeFilename(part.fileName ?: "attachment") to part.inputStream.readBytes())
                    }.onFailure { android.util.Log.w("MailCompose", "attachment $path failed: $it") }.getOrNull()
                }.toMap()
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    data class FoundFile(val from: String, val uid: Long, val text: String)

    fun findFiles(email: String, accessToken: String, filename: String, afterUid: Long, firstLook: Int = 200): Pair<Long, List<FoundFile>> {
        app.etsprivate.demo.Demo.source?.let { return afterUid to emptyList() }
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder("INBOX") as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val total = folder.messageCount
                if (total == 0) return afterUid to emptyList()
                val messages = if (afterUid > 0) {
                    folder.getMessagesByUID(afterUid + 1, javax.mail.UIDFolder.LASTUID).filterNotNull()
                } else {
                    folder.getMessages(maxOf(1, total - firstLook + 1), total).toList()
                }
                folder.fetch(
                    messages.toTypedArray(),
                    FetchProfile().apply {
                        add(FetchProfile.Item.ENVELOPE)
                        add(FetchProfile.Item.CONTENT_INFO)
                        add(javax.mail.UIDFolder.FetchProfileItem.UID)
                    },
                )
                var highest = afterUid
                val found = mutableListOf<FoundFile>()
                messages.forEach { message ->
                    val uid = folder.getUID(message)
                    if (uid <= afterUid) return@forEach
                    highest = maxOf(highest, uid)
                    val part = runCatching { findNamed(message, filename) }.getOrNull() ?: return@forEach
                    val from = (message.from?.firstOrNull() as? InternetAddress)?.address?.lowercase() ?: return@forEach
                    val text = runCatching { part.inputStream.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
                        ?: return@forEach
                    found.add(FoundFile(from, uid, text))
                }
                return highest to found
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    private fun findNamed(part: Part, filename: String): Part? {
        if (part.isMimeType("multipart/*")) {
            val multipart = part.content as? Multipart ?: return null
            for (i in 0 until multipart.count) findNamed(multipart.getBodyPart(i), filename)?.let { return it }
            return null
        }
        return part.takeIf { runCatching { it.fileName }.getOrNull() == filename }
    }

    fun downloadAttachment(email: String, accessToken: String, folderName: String, uid: Long, path: String): Pair<String, ByteArray> {
        val store = connect(email, accessToken)
        try {
            val folder = store.getFolder(folderName) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            try {
                val message = folder.getMessageByUID(uid) ?: throw AuthException("Message not found")
                val part = resolvePart(message, path) ?: throw AuthException("Attachment not found")
                val filename = sanitizeFilename(part.fileName ?: "attachment")
                return filename to part.inputStream.readBytes()
            } finally {
                folder.close(false)
            }
        } finally {
            release(store)
        }
    }

    private fun sanitizeFilename(raw: String): String {
        val decoded = runCatching { javax.mail.internet.MimeUtility.decodeText(raw) }.getOrDefault(raw)
        return decoded.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").trim().ifBlank { "attachment" }
    }

    private fun InternetAddress.toMailAddress() = MailAddress(personal.orEmpty(), address.orEmpty())

    private fun InternetAddress.displayName(): String = personal?.takeIf { it.isNotBlank() } ?: address.orEmpty()

    private fun resolvePart(root: Part, path: String): Part? {
        if (path.isBlank()) return root
        var current: Part = root
        for (indexText in path.split(".")) {
            val index = indexText.toIntOrNull() ?: return null
            val content = current.content as? Multipart ?: return null
            if (index !in 0 until content.count) return null
            current = content.getBodyPart(index)
        }
        return current
    }

    private class BodyParts {
        var text: String? = null
        var html: String? = null
        val attachments = mutableListOf<MailAttachment>()
        val inlineImages = mutableMapOf<String, MailInlineImage>()
    }

    private fun walkParts(part: Part, path: String, out: BodyParts) {
        runCatching { walkPartUnsafe(part, path, out) }.onFailure { e ->

            android.util.Log.w("MailBody", "part '$path' ${runCatching { part.contentType }.getOrNull()} failed: $e")
        }
    }

    private fun readText(part: Part, path: String): String? {
        val content = runCatching { part.content }
        content.exceptionOrNull()?.let { android.util.Log.w("MailBody", "part '$path' content failed: $it") }
        val value = content.getOrNull()
        if (value != null && value !is String) android.util.Log.w("MailBody", "part '$path' content is ${value.javaClass.name}, not text")
        return value as? String
    }

    private fun walkPartUnsafe(part: Part, path: String, out: BodyParts) {
        if (part.isMimeType("multipart/*")) {
            val multipart = part.content as? Multipart ?: return
            for (i in 0 until multipart.count) {
                walkParts(multipart.getBodyPart(i), if (path.isEmpty()) "$i" else "$path.$i", out)
            }
            return
        }
        val contentId = runCatching { (part as? MimePart)?.getHeader("Content-ID")?.firstOrNull() }.getOrNull()?.trim('<', '>')
        if (contentId != null && part.isMimeType("image/*")) {
            val mimeType = part.contentType.substringBefore(';').trim()
            val base64 = Base64.encodeToString(part.inputStream.readBytes(), Base64.NO_WRAP)
            out.inlineImages[contentId] = MailInlineImage(mimeType, base64)
            return
        }
        val filename = runCatching { part.fileName }.getOrNull()
        val isAttachment = runCatching { part.disposition }.getOrNull()?.equals(Part.ATTACHMENT, ignoreCase = true) == true ||
            (filename != null && !part.isMimeType("text/plain") && !part.isMimeType("text/html"))
        if (isAttachment && filename != null) {
            out.attachments.add(MailAttachment(sanitizeFilename(filename), part.contentType.substringBefore(';').trim(), runCatching { part.size.toLong() }.getOrDefault(-1L), path))
            return
        }
        if (out.html == null && part.isMimeType("text/html")) {
            out.html = readText(part, path)
        } else if (out.text == null && part.isMimeType("text/plain")) {
            out.text = readText(part, path)
        }
    }
}

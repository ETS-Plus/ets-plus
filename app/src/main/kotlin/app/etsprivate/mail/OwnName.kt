package app.etsprivate.mail

import android.content.Context
import app.etsprivate.auth.TokenStore
import app.etsprivate.settings.StudentName

internal object OwnName {
    private var readAt = -1
    private var address: String? = null

    private fun mine(context: Context): String? {
        if (readAt != TokenStore.revision) {
            address = TokenStore.load(context, "mail")?.email?.lowercase()
            readAt = TokenStore.revision
        }
        return address
    }

    fun isMine(context: Context, address: String?): Boolean =
        !address.isNullOrBlank() && address.equals(mine(context), ignoreCase = true)

    fun seen(context: Context, address: MailAddress): MailAddress {
        StudentName.revision
        if (!isMine(context, address.address)) return address
        return StudentName.shown(context)?.let { address.copy(name = it) } ?: address
    }

    fun sent(context: Context, address: MailAddress): MailAddress {
        if (!isMine(context, address.address)) return address
        return StudentName.outgoing(context)?.let { address.copy(name = it) } ?: address
    }

    fun seenSender(context: Context, message: MailMessage): String {
        StudentName.revision
        if (!isMine(context, message.fromAddress)) return message.from
        return StudentName.shown(context) ?: message.from
    }
}

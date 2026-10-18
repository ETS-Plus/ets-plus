package app.etsprivate.mail

import java.util.Properties
import javax.activation.DataHandler
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

data class OutgoingAttachment(val filename: String, val mimeType: String, val bytes: ByteArray)

object OutlookSmtp {
    private const val HOST = "smtp.office365.com"

    fun compose(
        fromEmail: String,

        fromName: String? = null,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        body: String,
        htmlBody: String? = null,
        attachments: List<OutgoingAttachment>,
        inReplyTo: String? = null,
        references: String? = null,
    ): MimeMessage {
        val props = Properties().apply {
            put("mail.smtp.host", HOST)
            put("mail.smtp.port", "587")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.auth", "true")
            put("mail.smtp.auth.mechanisms", "XOAUTH2")
        }
        val session = Session.getInstance(props)
        return MimeMessage(session).apply {
            setFrom(if (fromName.isNullOrBlank()) InternetAddress(fromEmail) else InternetAddress(fromEmail, fromName, "UTF-8"))
            to.forEach { addRecipient(Message.RecipientType.TO, InternetAddress(it)) }
            cc.forEach { addRecipient(Message.RecipientType.CC, InternetAddress(it)) }
            bcc.forEach { addRecipient(Message.RecipientType.BCC, InternetAddress(it)) }
            setSubject(subject, "UTF-8")
            inReplyTo?.let { setHeader("In-Reply-To", it) }
            references?.let { setHeader("References", it) }

            val textPart = MimeBodyPart().apply { setText(body, "UTF-8") }
            val alternative = if (htmlBody == null) null else MimeMultipart("alternative").apply {
                addBodyPart(textPart)
                addBodyPart(MimeBodyPart().apply { setContent(htmlBody, "text/html; charset=UTF-8") })
            }
            if (attachments.isEmpty()) {
                if (alternative != null) setContent(alternative) else setText(body, "UTF-8")
            } else {
                val multipart = MimeMultipart()
                multipart.addBodyPart(
                    if (alternative != null) MimeBodyPart().apply { setContent(alternative) } else textPart,
                )
                attachments.forEach { attachment ->
                    multipart.addBodyPart(
                        MimeBodyPart().apply {
                            dataHandler = DataHandler(ByteArrayDataSource(attachment.bytes, attachment.mimeType))
                            fileName = attachment.filename
                        },
                    )
                }
                setContent(multipart)
            }
            saveChanges()
        }
    }

    fun send(
        fromEmail: String,
        fromName: String? = null,
        accessToken: String,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        body: String,
        htmlBody: String? = null,
        attachments: List<OutgoingAttachment>,
        inReplyTo: String? = null,
        references: String? = null,
    ) {
        app.etsprivate.demo.Demo.source?.let { return }
        val message = compose(fromEmail, fromName, to, cc, bcc, subject, body, htmlBody, attachments, inReplyTo, references)
        val session = message.session
        val transport = session.getTransport("smtp")
        try {
            transport.connect(HOST, fromEmail, accessToken)
            transport.sendMessage(message, message.allRecipients)
        } finally {
            transport.close()
        }
    }
}

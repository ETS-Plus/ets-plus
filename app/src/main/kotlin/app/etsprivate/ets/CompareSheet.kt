package app.etsprivate.ets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.etsprivate.R
import app.etsprivate.mail.ClassContacts
import app.etsprivate.mail.MailContact
import app.etsprivate.mail.MailContacts
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.ExpressiveSwitch
import app.etsprivate.ui.groupColor
import app.etsprivate.ui.groupShape
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private fun isStudent(address: String) = address.lowercase().endsWith("@ens.etsmtl.ca")

internal class CompareOverlay(
    val id: String,
    val name: String,
    val color: Color,
    val byDate: Map<String, List<SharedClass>>,

    val examsByDate: Map<String, List<CalendarExam>> = emptyMap(),
) {
    val initial: String get() = app.etsprivate.mail.avatarInitials(name)
}

private object CompareChoice {
    private fun prefs(context: android.content.Context) =
        context.getSharedPreferences("schedule_shares", android.content.Context.MODE_PRIVATE)

    var revision by mutableStateOf(0)
        private set

    fun hidden(context: android.content.Context): Set<String> =
        prefs(context).getStringSet("unselected", emptySet()).orEmpty().toSet()

    fun setHidden(context: android.content.Context, next: Set<String>) {
        val edit = prefs(context).edit()
        if (next.isEmpty()) edit.remove("unselected") else edit.putStringSet("unselected", HashSet(next))
        edit.apply()
        revision++
    }
}

private fun personColor(index: Int): Color = app.etsprivate.ui.IconColors[index % app.etsprivate.ui.IconColors.size]

@Composable
internal fun rememberCompareOverlays(): List<CompareOverlay> {
    val context = LocalContext.current
    return remember(ScheduleShares.revision, CompareChoice.revision) {
        val hidden = CompareChoice.hidden(context)
        ScheduleShares.people(context).mapIndexedNotNull { index, person ->
            val schedule = person.schedule ?: return@mapIndexedNotNull null
            if (person.email in hidden) return@mapIndexedNotNull null
            CompareOverlay(
                person.email, person.name, personColor(index), schedule.classes.groupBy { it.date },
                schedule.exams.groupBy { it.date }.mapValues { (_, list) -> list.map { CalendarExam(it.acronym, it.title, time = it.time, end = it.end) } },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CompareSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val locale = LocaleHelper.locale(context)
    val sentMessage = stringResource(R.string.compare_sent)
    val sendFailedMessage = stringResource(R.string.compare_send_failed)

    var people by remember { mutableStateOf(ScheduleShares.people(context)) }
    val hidden = remember(CompareChoice.revision) { CompareChoice.hidden(context) }

    val mailReady = remember(app.etsprivate.auth.TokenStore.revision) { ScheduleShares.mailReady(context) }
    var adding by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var sendingTo by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<LinkedPerson?>(null) }

    LaunchedEffect(ScheduleShares.revision) {
        val fresh = ScheduleShares.people(context)
        if (fresh != people) people = fresh
    }

    LaunchedEffect(mailReady) {
        if (!mailReady) return@LaunchedEffect
        ScheduleShares.resendIfChanged(context)
        while (true) {
            ScheduleShares.receive(context)
            kotlinx.coroutines.delay(20_000)
        }
    }

    fun share(email: String) {
        if (sendingTo != null) return
        sendingTo = email
        focus.clearFocus()
        scope.launch {
            val ok = ScheduleShares.shareWith(context, email)
            sendingTo = null
            AppFeedback.show(if (ok) sentMessage else sendFailedMessage)
            if (ok) {
                people = ScheduleShares.people(context)
                if (email == address.trim().lowercase()) {
                    address = ""
                    adding = false
                }
            }
        }
    }

    AppBottomSheet(title = stringResource(R.string.compare_title), onDismissRequest = onDismiss, followsKeyboard = !adding) { close ->
        when {
            !mailReady -> Text(
                stringResource(R.string.compare_needs_mail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            people.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(36.dp))
                }
                Text(
                    stringResource(R.string.compare_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.compare_empty_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )
                FilledTonalButton(onClick = { adding = true }) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.compare_add_person), modifier = Modifier.padding(start = 8.dp))
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(app.etsprivate.ui.GroupGap)) {
                    people.forEachIndexed { index, person ->
                        val schedule = person.schedule
                        val shown = schedule != null && person.email !in hidden
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(groupShape(index, people.size))
                                .background(groupColor)

                                .combinedClickable(
                                    onClick = {
                                        if (schedule != null) {
                                            CompareChoice.setHidden(context, if (shown) hidden + person.email else hidden - person.email)
                                        }
                                    },
                                    onLongClick = { removing = person },
                                )
                                .heightIn(min = 72.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(personColor(index)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(app.etsprivate.mail.avatarInitials(person.name), color = Color.White, style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(person.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    when {
                                        schedule == null -> stringResource(R.string.compare_waiting_theirs)
                                        !person.sharing -> stringResource(R.string.compare_received_text)
                                        else -> stringResource(
                                            R.string.compare_received_on,
                                            DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(schedule.sentAt)),
                                        )
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            when {
                                schedule != null && !person.sharing -> FilledTonalButton(
                                    onClick = { share(person.email) },
                                    enabled = sendingTo == null,
                                ) { Text(stringResource(R.string.compare_send_mine)) }
                                schedule != null -> ExpressiveSwitch(
                                    checked = shown,
                                    onChange = { on -> CompareChoice.setHidden(context, if (on) hidden - person.email else hidden + person.email) },
                                )
                            }
                        }
                    }
                }

                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.OutlinedButton(onClick = { adding = true }) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.compare_add_person), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (adding && mailReady) {
        AppBottomSheet(title = stringResource(R.string.compare_add_person), onDismissRequest = { adding = false }) {
            AddPerson(
                address = address,
                onAddress = { address = it },
                people = people,
                sending = sendingTo,
                onSend = { share(it) },
            )
        }
    }

    removing?.let { person ->
        ConfirmDialog(
            title = stringResource(R.string.compare_remove_title, person.name),
            onDismissRequest = { removing = null },
            confirmLabel = stringResource(R.string.compare_remove),
            onConfirm = {
                ScheduleShares.remove(context, person.email)
                people = ScheduleShares.people(context)
                removing = null
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            Text(stringResource(R.string.compare_remove_explained), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AddPerson(
    address: String,
    onAddress: (String) -> Unit,
    people: List<LinkedPerson>,
    sending: String?,
    onSend: (String) -> Unit,
) {
    val context = LocalContext.current

    val teachers = remember { ClassContacts.all(context).map { it.address.lowercase() }.toSet() }
    val linked = people.map { it.email }.toSet()
    val suggestions = remember(address, people) {
        val typed = address.trim().lowercase()
        if (typed.isEmpty()) {
            emptyList()
        } else {
            MailContacts.all(context)
                .filter { isStudent(it.address) && it.address.lowercase() !in teachers && it.address.lowercase() !in linked }
                .filter { it.address.lowercase().contains(typed) || it.name.lowercase().contains(typed) }
                .filterNot { it.address.lowercase() == typed }
                .sortedWith(compareByDescending<MailContact> { it.times }.thenByDescending { it.lastUsed })
                .take(4)
        }
    }
    val canSend = address.trim().let { it.contains('@') && it.substringAfter('@').contains('.') }

    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450)
        runCatching { focus.requestFocus() }
    }
    OutlinedTextField(
        value = address,
        onValueChange = onAddress,
        singleLine = true,
        label = { Text(stringResource(R.string.compare_address_label)) },
        placeholder = { Text(stringResource(R.string.compare_address_hint)) },
        trailingIcon = {
            if (sending != null && sending == address.trim().lowercase()) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { onSend(address.trim().lowercase()) }, enabled = canSend && sending == null) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.compare_send))
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (canSend) onSend(address.trim().lowercase()) }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
    suggestions.forEach { contact ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onAddress(contact.address) }
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(contact.name.ifBlank { contact.address }, style = MaterialTheme.typography.bodyLarge)
                if (contact.name.isNotBlank()) {
                    Text(contact.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    Text(
        stringResource(R.string.compare_share_explained),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

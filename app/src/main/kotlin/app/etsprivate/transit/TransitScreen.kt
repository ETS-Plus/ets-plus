package app.etsprivate.transit

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import kotlin.math.roundToInt
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import app.etsprivate.R
import app.etsprivate.ui.theme.AppShapes
import androidx.compose.ui.draw.clip
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.cache.ImageCache
import app.etsprivate.cache.MemoryCache
import app.etsprivate.settings.Settings
import app.etsprivate.cache.DiskCache
import app.etsprivate.ets.AccentRow
import app.etsprivate.ets.SectionLabel
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.CopyableValue
import app.etsprivate.ui.AppTopBar
import app.etsprivate.ui.ImageSlot
import app.etsprivate.ui.TopLoadingBar
import app.etsprivate.ui.ZoomableBox
import app.etsprivate.ui.rememberDelayedLoading
import app.etsprivate.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TransitPage { HOME, PLANNER, MAP, CARD }

private const val CARD_PAGE = "card"

private fun Long.asClockTime(formatter: SimpleDateFormat): String =
    if (this <= 0) "--:--" else formatter.format(Date(this * 1000))

@Composable
fun TransitScreen(
    rootPage: String? = null,

    visible: Boolean = true,
) {
    val context = LocalContext.current

    val language = if (context.resources.configuration.locales[0].language == "fr") "fr" else "en"
    val client = remember { ChronoClient(context.applicationContext) }
    client.language = language

    if (MemoryCache.get<String>("transit/language") != language) {
        MemoryCache.put("transit/language", language)
        MemoryCache.put("transit/catalog", null)
        MemoryCache.put("transit/zoneImage", null)
        MemoryCache.put("transit/decoded", null)
        MemoryCache.put("transit/results", null)
    }

    var page by remember {
        mutableStateOf(
            MemoryCache.get<String>(app.etsprivate.PAGE_TRANSIT)
                ?.let { runCatching { TransitPage.valueOf(it) }.getOrNull() }
                ?: TransitPage.HOME,
        )
    }
    LaunchedEffect(page) { MemoryCache.put(app.etsprivate.PAGE_TRANSIT, page.name) }
    val hasCard = app.etsprivate.plugin.Plugins.byId(CARD_PAGE) != null

    var saved by remember { mutableStateOf(SavedTrips.load(context)) }

    LaunchedEffect(page) { saved = SavedTrips.load(context) }

    fun openTrip(trip: Itinerary?) {
        val resuming = TripFollow.active && trip != null && TripFollow.isSameTrip(trip)
        if (TripFollow.active && !resuming) TripFollow.stop(context)
        MemoryCache.put("transit/mapTrip", if (resuming) TripFollow.trip else trip)

        if (trip != null) app.etsprivate.ui.Immersive.hideBottomBar = true
        page = TransitPage.MAP
    }

    when (page) {
        TransitPage.CARD -> {
            val card = app.etsprivate.plugin.Plugins.byId(CARD_PAGE)
            if (card == null) {
                page = TransitPage.HOME
            } else {
                BackHandler(enabled = visible) { page = TransitPage.HOME }
                card.Content(app.etsprivate.plugin.PluginHost(context, visible, onBack = { page = TransitPage.HOME }))
            }
        }
        TransitPage.MAP -> {
            val from = MemoryCache.get<String>("transit/mapFrom")?.let { runCatching { TransitPage.valueOf(it) }.getOrNull() }
                ?: TransitPage.PLANNER
            TransitMapScreen(
                itinerary = if (TripFollow.active) TripFollow.trip else MemoryCache.get("transit/mapTrip"),
                visible = visible,
                onBack = if (rootPage == TransitPage.MAP.name) null else ({
                    app.etsprivate.ui.Immersive.hideBottomBar = false
                    page = from
                }),
                overlay = null,
            )
        }
        TransitPage.HOME -> {
            TransitMapScreen(
                itinerary = null,
                onBack = null,
                visible = visible,
                overlay = {
                    MapSearch(
                        following = TripFollow.active && TripFollow.trip != null,
                        hasCard = hasCard,
                        saved = saved,
                        onTrip = { trip ->
                            MemoryCache.put("transit/originPlace", trip.origin)
                            MemoryCache.put("transit/destPlace", trip.destination)
                            MemoryCache.put("transit/results", null)
                            MemoryCache.put(PLAN_ON_OPEN, true)
                            page = TransitPage.PLANNER
                        },
                        onSearch = { page = TransitPage.PLANNER },
                        onResume = {
                            MemoryCache.put("transit/mapFrom", TransitPage.HOME.name)
                            openTrip(TripFollow.trip)
                        },
                        onCard = { page = TransitPage.CARD },
                    )
                },
            )
        }
        TransitPage.PLANNER -> {
            BackHandler(enabled = visible) { page = TransitPage.HOME }
            PlannerPage(
                visible = visible,
                client = client,
                onBack = { page = TransitPage.HOME },
                onSavedChange = { saved = it },
                onOpenMap = { trip ->
                    MemoryCache.put("transit/mapFrom", TransitPage.PLANNER.name)
                    openTrip(trip)
                },
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.MapSearch(
    following: Boolean,
    hasCard: Boolean,
    saved: List<SavedTrip>,
    onTrip: (SavedTrip) -> Unit,
    onSearch: () -> Unit,
    onResume: () -> Unit,
    onCard: () -> Unit,
) {
    Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(top = 20.dp)) {
        androidx.compose.material3.Surface(
            onClick = onSearch,
            shape = CircleShape,
            color = app.etsprivate.ui.groupColor,
            shadowElevation = 3.dp,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(48.dp),
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    pluginString(R.string.transit_where_to),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        if (following || hasCard || saved.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (following) {
                    MapChip(pluginString(R.string.transit_trip_in_progress), Icons.Outlined.Navigation, highlighted = true, onClick = onResume)
                }
                if (saved.isNotEmpty()) {
                    var picking by remember { mutableStateOf(false) }
                    MapChip(pluginString(R.string.transit_favorites), Icons.Outlined.Star, trailingArrow = true, onClick = { picking = true })
                    if (picking) {
                        FavoritesPicker(saved, onDismiss = { picking = false }, onPick = { picking = false; onTrip(it) })
                    }
                }
                if (hasCard) MapChip(pluginString(R.string.transit_my_card), Icons.Outlined.CreditCard, onClick = onCard)
            }
        }
    }
}

@Composable
private fun FavoritesPicker(trips: List<SavedTrip>, onDismiss: () -> Unit, onPick: (SavedTrip) -> Unit) {
    var typed by remember { mutableStateOf("") }
    app.etsprivate.ui.FullScreenDialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = app.etsprivate.ui.pageColor) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = pluginString(R.string.action_back))
                    }
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (typed.isEmpty()) {
                            Text(
                                pluginString(R.string.transit_favorites_search),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (typed.isNotEmpty()) {
                        IconButton(onClick = { typed = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = pluginString(R.string.action_clear))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val needle = typed.trim().lowercase()
                val shown = remember(trips, needle) {
                    if (needle.isEmpty()) trips else trips.filter { it.label.lowercase().contains(needle) || it.origin.name.lowercase().contains(needle) || it.destination.name.lowercase().contains(needle) }
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    shown.forEach { trip ->
                        val named = trip.name?.takeIf { it.isNotBlank() }
                        SuggestionRow(
                            title = named ?: trip.destination.name.substringBefore(", "),
                            subtitle = "${trip.origin.name.substringBefore(", ")} → ${trip.destination.name.substringBefore(", ")}",
                            icon = Icons.Outlined.Star,
                            accent = true,
                            onClick = { onPick(trip) },
                        )
                    }
                    Spacer(Modifier.windowInsetsBottomHeight(androidx.compose.foundation.layout.WindowInsets.ime))
                }
            }
        }
    }
}

private const val PLAN_ON_OPEN = "transit/planOnOpen"

@Composable
private fun MapChip(label: String, icon: ImageVector, highlighted: Boolean = false, trailingArrow: Boolean = false, onClick: () -> Unit) {
    val container = if (highlighted) MaterialTheme.colorScheme.primaryContainer else app.etsprivate.ui.groupColor
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    androidx.compose.material3.Surface(onClick = onClick, shape = CircleShape, color = container, shadowElevation = 2.dp) {
        Row(modifier = Modifier.heightIn(min = 36.dp).padding(start = 12.dp, end = if (trailingArrow) 8.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            if (trailingArrow) Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlannerPage(
    client: ChronoClient,
    onBack: () -> Unit,
    onOpenMap: (Itinerary?) -> Unit,
    onSavedChange: (List<SavedTrip>) -> Unit,
    visible: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val myPositionLabel = pluginString(R.string.transit_my_position)
    var origin by remember {
        mutableStateOf(
            MemoryCache.get<Place>("transit/originPlace")
                ?: CurrentPosition.lastKnownPlace(context, myPositionLabel)?.also { MemoryCache.put("transit/originPlace", it) },
        )
    }
    var destination by remember { mutableStateOf(MemoryCache.get<Place>("transit/destPlace")) }
    var saved by remember { mutableStateOf(SavedTrips.load(context)) }
    var confirmingRemove by remember { mutableStateOf<SavedTrip?>(null) }
    val savedMessage = pluginString(R.string.transit_trip_saved)
    var results by remember { mutableStateOf(MemoryCache.get<List<Itinerary>>("transit/results").orEmpty()) }

    var tripTime by remember { mutableStateOf(TripTime()) }
    var pickingTime by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var planning by remember { mutableStateOf(false) }

    var editing by remember { mutableStateOf<PlaceMark?>(if (destination == null) PlaceMark.TO else null) }
    var originText by remember { mutableStateOf(origin?.name.orEmpty()) }
    var destText by remember { mutableStateOf(destination?.name.orEmpty()) }
    var suggestions by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    var answeredFor by remember { mutableStateOf<String?>(null) }
    val originFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val destFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val noItinerariesMessage = pluginString(R.string.transit_no_itineraries)

    val planFailedMessage = pluginString(R.string.transit_error_plan)
    val timeInPastMessage = pluginString(R.string.transit_time_in_past)
    val noPositionMessage = pluginString(R.string.transit_no_position)

    fun plan(from: Place?, to: Place?, at: TripTime = tripTime) {
        if (from == null || to == null) return

        if (at.at != null && at.at < System.currentTimeMillis() / 1000) {
            results = emptyList()
            MemoryCache.put("transit/results", null)
            status = timeInPastMessage
            planning = false
            return
        }
        planning = true
        status = null

        saved = SavedTrips.touch(context, from.name, to.name)
        onSavedChange(saved)
        scope.launch {
            try {
                results = withContext(Dispatchers.IO) {
                    client.ensureAuthenticated()
                    client.getItineraries(
                        from,
                        to,
                        departAt = at.at?.takeIf { !at.arriving },
                        arriveBy = at.at?.takeIf { at.arriving },
                    )
                }
                MemoryCache.put("transit/results", results)
                if (results.isEmpty()) status = noItinerariesMessage
            } catch (e: Exception) {
                status = planFailedMessage
            } finally {
                planning = false
            }
        }
    }

    fun choose(end: PlaceMark, place: Place) {
        suggestions = emptyList()
        if (end == PlaceMark.FROM) {
            origin = place
            originText = place.name
            MemoryCache.put("transit/originPlace", place)
        } else {
            destination = place
            destText = place.name
            MemoryCache.put("transit/destPlace", place)
        }
        if (origin == null) {
            editing = PlaceMark.FROM
            originFocus.requestFocus()
        } else if (destination == null) {
            editing = PlaceMark.TO
            destFocus.requestFocus()
        } else {
            editing = null
            focus.clearFocus()
            plan(origin, destination)
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    fun chooseHere(end: PlaceMark) {
        if (!CurrentPosition.granted(context)) {
            locationPermission.launch(CurrentPosition.PERMISSIONS)
            return
        }
        scope.launch {
            val here = CurrentPosition.place(context, myPositionLabel)
            if (here == null) AppFeedback.show(noPositionMessage) else choose(end, here)
        }
    }

    val query = when (editing) {
        PlaceMark.FROM -> originText.takeIf { it != origin?.name }
        PlaceMark.TO -> destText.takeIf { it != destination?.name }
        null -> null
    }.orEmpty()
    LaunchedEffect(query) {
        if (query.length < 3) {
            suggestions = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(300)
        searching = true
        suggestions = withContext(Dispatchers.IO) {
            runCatching { client.ensureAuthenticated(); client.suggestPlaces(query) }.getOrDefault(emptyList())
        }
        answeredFor = query
        searching = false
    }

    LaunchedEffect(Unit) {
        if (MemoryCache.get<Boolean>(PLAN_ON_OPEN) == true) {
            MemoryCache.put(PLAN_ON_OPEN, null)
            planning = true
            val from = origin?.refreshedIfHere(context, myPositionLabel)
            val to = destination?.refreshedIfHere(context, myPositionLabel)
            origin = from
            destination = to
            MemoryCache.put("transit/originPlace", from)
            MemoryCache.put("transit/destPlace", to)
            plan(from, to)
        } else if (editing == PlaceMark.TO) {
            runCatching { destFocus.requestFocus() }
        }
    }

    BackHandler(enabled = visible && editing != null && destination != null) {
        originText = origin?.name.orEmpty()
        destText = destination?.name.orEmpty()
        editing = null
        focus.clearFocus()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val currentTrip = origin?.let { o -> destination?.let { d -> SavedTrip(o, d) } }
        val savedTrip = currentTrip?.let { trip ->
            saved.firstOrNull { it.origin.name == trip.origin.name && it.destination.name == trip.destination.name }
        }
        val outlineColor = MaterialTheme.colorScheme.outline
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = pluginString(R.string.action_back))
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(app.etsprivate.ui.groupColor)
                    .drawBehind {
                        val x = 4.dp.toPx() + 22.dp.toPx()
                        drawLine(
                            outlineColor,
                            Offset(x, 4.dp.toPx() + 26.dp.toPx() + 11.dp.toPx()),
                            Offset(x, 4.dp.toPx() + 79.dp.toPx() - 14.dp.toPx()),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(0.1f, 5.dp.toPx())),
                        )
                    }
                    .padding(4.dp),
            ) {
                EndField(
                    mark = PlaceMark.FROM,
                    label = pluginString(R.string.transit_from),
                    text = originText,
                    isHere = origin?.lieuId == null && origin?.name == myPositionLabel && originText == myPositionLabel,
                    editing = editing == PlaceMark.FROM,
                    focusRequester = originFocus,
                    onText = { originText = it },
                    onFocus = { editing = PlaceMark.FROM },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 44.dp, end = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                EndField(
                    mark = PlaceMark.TO,
                    label = pluginString(R.string.transit_to),
                    text = destText,
                    isHere = destination?.lieuId == null && destination?.name == myPositionLabel && destText == myPositionLabel,
                    editing = editing == PlaceMark.TO,
                    focusRequester = destFocus,
                    onText = { destText = it },
                    onFocus = { editing = PlaceMark.TO },
                )
            }
            Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                IconButton(
                    onClick = {
                        if (savedTrip != null) {
                            confirmingRemove = savedTrip
                        } else if (currentTrip != null) {
                            saved = SavedTrips.add(context, currentTrip)
                            onSavedChange(saved)
                            AppFeedback.show(savedMessage)
                        }
                    },
                    enabled = currentTrip != null,
                ) {
                    Icon(
                        if (savedTrip != null) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = pluginString(if (savedTrip != null) R.string.transit_remove_trip else R.string.transit_save_trip),
                        tint = if (savedTrip != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = {
                    val swapped = origin
                    origin = destination
                    destination = swapped
                    originText = origin?.name.orEmpty()
                    destText = destination?.name.orEmpty()
                    MemoryCache.put("transit/originPlace", origin)
                    MemoryCache.put("transit/destPlace", destination)
                    editing = null
                    focus.clearFocus()
                    plan(origin, destination)
                }) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = pluginString(R.string.transit_swap))
                }
            }
        }

        confirmingRemove?.takeIf { visible }?.let { trip ->
            ConfirmDialog(
                title = pluginString(R.string.transit_remove_trip_title),
                onDismissRequest = { confirmingRemove = null },
                confirmLabel = pluginString(R.string.transit_remove_trip),
                onConfirm = {
                    saved = SavedTrips.remove(context, trip)
                    onSavedChange(saved)
                    confirmingRemove = null
                },
                dismissLabel = pluginString(R.string.action_cancel),
            ) {
            }
        }

        val end = editing
        if (end != null) {
            if (searching) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(CircleShape),
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                SuggestionRow(
                    title = myPositionLabel,
                    subtitle = null,
                    icon = Icons.Outlined.MyLocation,
                    accent = true,
                    onClick = { chooseHere(end) },
                )
                suggestions.forEach { place ->

                    val title = place.name.substringBefore(", ")
                    val rest = place.name.substringAfter(", ", "").ifBlank { null }
                    SuggestionRow(
                        title = title,
                        subtitle = rest,
                        icon = if (place.isTransitStop) Icons.Outlined.DirectionsBus else Icons.Outlined.Place,
                        onClick = { choose(end, place) },
                    )
                }
                if (!searching && suggestions.isEmpty() && query.length >= 3 && answeredFor == query) {
                    Text(
                        pluginString(R.string.transit_no_place),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }

                Spacer(Modifier.windowInsetsBottomHeight(androidx.compose.foundation.layout.WindowInsets.ime))
            }
            return@Column
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                PlannerChip(
                    label = tripTimeLabel(tripTime),
                    icon = Icons.Outlined.Schedule,
                    selected = !tripTime.isNow,
                    trailingArrow = true,
                    onClick = { pickingTime = true },
                )

                if (saved.isNotEmpty()) {
                    var picking by remember { mutableStateOf(false) }
                    Spacer(Modifier.width(8.dp))
                    PlannerChip(
                        label = pluginString(R.string.transit_favorites),
                        icon = Icons.Outlined.Star,
                        trailingArrow = true,
                        onClick = { picking = true },
                    )
                    if (picking) {
                        FavoritesPicker(saved, onDismiss = { picking = false }, onPick = { trip ->
                            picking = false
                            scope.launch {
                                planning = true
                                val from = trip.origin.refreshedIfHere(context, myPositionLabel)
                                val to = trip.destination.refreshedIfHere(context, myPositionLabel)
                                origin = from
                                destination = to
                                originText = from.name
                                destText = to.name
                                MemoryCache.put("transit/originPlace", from)
                                MemoryCache.put("transit/destPlace", to)
                                plan(from, to)
                            }
                        })
                    }
                }
            }

            if (pickingTime && visible) {
                TripTimeSheet(
                    current = tripTime,
                    onDismiss = { pickingTime = false },
                    onPick = {
                        tripTime = it
                        pickingTime = false
                        plan(origin, destination, it)
                    },
                )
            }

            if (planning && results.isEmpty()) TopLoadingBar()
            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = app.etsprivate.ui.GroupMargin + 16.dp, vertical = 8.dp),
                )
            }
            if (results.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(app.etsprivate.ui.GroupGap),
                ) {
                    results.forEachIndexed { index, itinerary ->
                        RouteRow(itinerary, app.etsprivate.ui.groupShape(index, results.size)) { onOpenMap(itinerary) }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.default))
        }
    }
}

@Composable
private fun SuggestionRow(title: String, subtitle: String?, icon: ImageVector, accent: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun Milestone.routeColor(): Color? {
    if (!isRail()) return null
    val raw = color ?: return null
    return runCatching {
        Color(android.graphics.Color.parseColor(if (raw.startsWith("#")) raw else "#$raw"))
    }.getOrNull()
}

internal fun Milestone.routeLabel(): String? {
    val short = routeShortName?.trim().orEmpty()
    val looksLikeCode = short.isEmpty() ||
        short.equals(vehicleType?.trim(), ignoreCase = true) ||
        short.equals(mode?.trim(), ignoreCase = true) ||
        short.uppercase() in setOf("LOC", "BUS", "METRO", "MÉTRO", "TRAIN", "REM")
    if (!looksLikeCode) return short
    return title?.trim()?.takeIf { it.isNotEmpty() } ?: short.takeIf { it.isNotEmpty() }
}

internal fun Milestone.isRail(): Boolean {
    val type = (vehicleType ?: mode).orEmpty()
    return listOf("METRO", "MÉTRO", "TRAIN", "REM", "SUBWAY", "RAIL").any { type.contains(it, ignoreCase = true) }
}

internal fun Milestone.icon(): ImageVector = when {
    !isTransit -> Icons.AutoMirrored.Outlined.DirectionsWalk
    isRail() -> Icons.Outlined.Train
    else -> Icons.Outlined.DirectionsBus
}

private enum class PlaceMark { FROM, TO }

@Composable
private fun EndField(
    mark: PlaceMark,
    label: String,
    text: String,
    isHere: Boolean,
    editing: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
    onText: (String) -> Unit,
    onFocus: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            when (mark) {
                PlaceMark.FROM -> Box(modifier = Modifier.size(14.dp).border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape))
                PlaceMark.TO -> Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
            }
        }
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = onText,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (isHere) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    field()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocus() },
        )
        if (editing && text.isNotEmpty()) {
            IconButton(onClick = { onText("") }) {
                Icon(Icons.Outlined.Close, contentDescription = pluginString(R.string.action_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun PlannerChip(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    trailingArrow: Boolean = false,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else app.etsprivate.ui.groupColor
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(start = 12.dp, end = if (trailingArrow) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        if (trailingArrow) Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RouteRow(itinerary: Itinerary, shape: androidx.compose.ui.graphics.Shape, onOpen: () -> Unit) {
    val context = LocalContext.current
    val locale = LocaleHelper.locale(context)
    val timeFormat = SimpleDateFormat(app.etsprivate.ui.Clock.pattern(context), locale)
    val firstRide = itinerary.milestones.firstOrNull { it.isTransit }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(app.etsprivate.ui.groupColor)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                RouteLegs(itinerary)
            }
            itinerary.durationText?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
        Text(
            "${itinerary.departureTime.asClockTime(timeFormat)} – ${itinerary.arrivalTime.asClockTime(timeFormat)}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 6.dp),
        )
        firstRide?.let { ride ->
            val from = ride.departureStop?.takeIf { it.isNotBlank() }
            if (from != null && ride.departureTime > 0) {
                Text(
                    pluginString(R.string.transit_leaves_from, ride.departureTime.asClockTime(timeFormat), from),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun RouteLegs(itinerary: Itinerary) {
    val legs = itinerary.milestones.filter { it.isTransit || it.durationSeconds >= 60 }
    legs.forEachIndexed { index, leg ->
        if (index > 0) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        LegChip(leg)
    }
}

@Composable
internal fun LegChip(leg: Milestone) {
    if (!leg.isTransit) {
        Row(verticalAlignment = Alignment.Bottom) {
            Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(
                "${(leg.durationSeconds / 60).roundToInt().coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val lineColor = leg.routeColor()
    val container = lineColor ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val content = when {
        lineColor == null -> MaterialTheme.colorScheme.onSurface
        lineColor.luminance() > 0.45f -> Color.Black
        else -> Color.White
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(leg.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        leg.routeLabel()?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 3.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(container)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

private val HERE_NAMES = setOf("Ma position", "My position")

internal suspend fun Place.refreshedIfHere(context: android.content.Context, myPositionLabel: String): Place =

    if (lieuId == null && (name == myPositionLabel || name in HERE_NAMES)) {
        CurrentPosition.place(context, myPositionLabel) ?: this
    } else {
        this
    }

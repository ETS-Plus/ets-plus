package app.etsprivate.transit

import app.etsprivate.R
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.etsprivate.plugin.PluginWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransportWidget {
    private class Departure(val at: Long, val line: String?, val summary: String)

    private var trip: SavedTrip? = null
    private var departures: List<Departure> = emptyList()
    private var askedAt = 0L
    private var failed = false

    val widget = PluginWidget(
        id = "ets.transport.departures",
        name = { context -> Plug.resources(context).getString(R.string.widget_departures) },
        page = "transit",
        prepare = { context, asked -> load(context, asked) },
        content = { Content() },
    ).apply {
        settings = { TripChoice() }
    }

    private const val PREFS = "transit_widget"
    private const val KEY_TRIP = "trip"

    private fun chosen(context: Context): SavedTrip? {
        val trips = SavedTrips.load(context)
        val wanted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TRIP, null)
        return trips.firstOrNull { it.label == wanted } ?: trips.firstOrNull()
    }

    @androidx.compose.runtime.Composable
    private fun TripChoice() {
        val context = androidx.compose.ui.platform.LocalContext.current
        val trips = androidx.compose.runtime.remember { SavedTrips.load(context) }
        if (trips.isEmpty()) return
        var picked by androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(chosen(context)?.label)
        }
        androidx.compose.material3.Text(
            Plug.string(R.string.widget_trip),
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            modifier = androidx.compose.ui.Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        )
        trips.forEach { trip ->
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .clickable {
                        picked = trip.label
                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(KEY_TRIP, trip.label).apply()
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                androidx.compose.material3.RadioButton(
                    selected = picked == trip.label,
                    onClick = {
                        picked = trip.label
                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(KEY_TRIP, trip.label).apply()
                    },
                )
                androidx.compose.material3.Text(
                    trip.label,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = androidx.compose.ui.Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    private suspend fun load(context: Context, asked: Boolean) {
        val saved = chosen(context)

        if (saved?.label != trip?.label) departures = emptyList()
        trip = saved
        if (saved == null) return
        val now = System.currentTimeMillis()

        val usable = departures.filter { it.at * 1000 > now }
        if (!asked && usable.size >= 2 && now - askedAt < 25 * 60 * 1000L) {
            departures = usable
            return
        }
        val found = withContext(Dispatchers.IO) {
            runCatching {
                val client = ChronoClient(context.applicationContext)
                client.language = if (Plug.resources(context).configuration.locales[0].language == "fr") "fr" else "en"
                client.ensureAuthenticated()

                val myPositionLabel = Plug.string(R.string.transit_my_position)
                val from = saved.origin.refreshedIfHere(context, myPositionLabel)
                val to = saved.destination.refreshedIfHere(context, myPositionLabel)
                client.getItineraries(from, to, departAt = now / 1000, arriveBy = null)
                    .take(4)
                    .map { itinerary ->
                        val ride = itinerary.milestones.firstOrNull { it.isTransit }
                        Departure(
                            at = itinerary.departureTime,
                            line = ride?.routeLabel(),
                            summary = ride?.summary ?: itinerary.durationText.orEmpty(),
                        )
                    }
            }.getOrNull()
        }
        failed = found.isNullOrEmpty() && usable.isEmpty()
        if (!found.isNullOrEmpty()) {
            departures = found
            askedAt = now
        } else {
            departures = usable
        }
    }

    @androidx.compose.runtime.Composable
    private fun Content() {
        val context = androidx.glance.LocalContext.current
        val words = Plug.resources(context)
        val clock = SimpleDateFormat(app.etsprivate.ui.Clock.pattern(context), Locale.getDefault())
        val saved = trip

        app.etsprivate.widget.WidgetHeader(saved?.label ?: words.getString(R.string.widget_departures), end = 32.dp)
        when {
            saved == null -> Text(
                words.getString(R.string.widget_departures_no_trip),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.padding(start = 4.dp),
            )
            departures.isEmpty() -> Text(
                words.getString(if (failed) R.string.transit_error_plan else R.string.transit_no_itineraries),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.padding(start = 4.dp),
            )
            else -> app.etsprivate.widget.WidgetGroup(
                departures.take(4).map { departure ->
                    @androidx.compose.runtime.Composable {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                clock.format(Date(departure.at * 1000)),
                                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium),
                            )
                            Text(
                                listOfNotNull(departure.line, departure.summary.takeIf { it.isNotBlank() }).joinToString(" · "),
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
                            )
                        }
                    }
                },
            )
        }
    }
}

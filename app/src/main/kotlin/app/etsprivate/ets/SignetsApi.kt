package app.etsprivate.ets

import app.etsprivate.cache.getOrNullDouble
import app.etsprivate.cache.getOrNullInt
import app.etsprivate.cache.getOrNullString
import app.etsprivate.cache.mapObjects
import app.etsprivate.cache.putOrBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

private const val BASE = "https://etsmobileapi.etsmtl.ca"

data class SignetsSession(val shortName: String, val name: String, val startDate: String, val endDate: String)

internal fun SignetsSession.toJson(): JSONObject = JSONObject()
    .put("shortName", shortName).put("name", name).put("startDate", startDate).put("endDate", endDate)
internal fun sessionFromJson(o: JSONObject): SignetsSession =
    SignetsSession(o.getString("shortName"), o.getString("name"), o.getString("startDate"), o.getString("endDate"))

data class SignetsProfile(
    val firstName: String,
    val lastName: String,
    val permanentCode: String,
    val universalCode: String,
    val balance: String,
)

internal fun SignetsProfile.toJson(): JSONObject = JSONObject()
    .put("firstName", firstName).put("lastName", lastName).put("permanentCode", permanentCode)
    .put("universalCode", universalCode).put("balance", balance)
internal fun profileFromJson(o: JSONObject): SignetsProfile = SignetsProfile(
    firstName = o.getString("firstName"),
    lastName = o.getString("lastName"),
    permanentCode = o.getString("permanentCode"),
    universalCode = o.getString("universalCode"),
    balance = o.getString("balance"),
)

data class SignetsCourse(
    val acronym: String,
    val title: String,
    val group: String,
    val session: String,
    val credits: String,
    val grade: String?,
)

internal fun SignetsCourse.toJson(): JSONObject = JSONObject()
    .put("acronym", acronym).put("title", title).put("group", group).put("session", session).put("credits", credits)
    .apply { putOrBlank("grade", grade) }
internal fun courseFromJson(o: JSONObject): SignetsCourse = SignetsCourse(
    acronym = o.getString("acronym"),
    title = o.getString("title"),
    group = o.getString("group"),
    session = o.getString("session"),
    credits = o.getString("credits"),
    grade = o.getOrNullString("grade"),
)

data class SignetsScheduleActivity(
    val courseAcronym: String,
    val courseTitle: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val activityCode: String,
    val courseGroup: String = "",
    val activityName: String = "",
)

internal fun SignetsScheduleActivity.toJson(): JSONObject = JSONObject()
    .put("courseAcronym", courseAcronym).put("courseTitle", courseTitle).put("dayOfWeek", dayOfWeek)
    .put("startTime", startTime).put("endTime", endTime).put("activityCode", activityCode)
    .put("courseGroup", courseGroup).put("activityName", activityName)
internal fun scheduleActivityFromJson(o: JSONObject): SignetsScheduleActivity = SignetsScheduleActivity(
    courseAcronym = o.getString("courseAcronym"),
    courseTitle = o.getString("courseTitle"),
    dayOfWeek = o.getInt("dayOfWeek"),
    startTime = o.getString("startTime"),
    endTime = o.getString("endTime"),
    activityCode = o.getString("activityCode"),
    courseGroup = o.getString("courseGroup"),
    activityName = o.getString("activityName"),
)

data class SignetsProgram(
    val name: String,
    val code: String,
    val average: String,
    val accumulatedCredits: String,
    val registeredCredits: String,
    val completedCourses: String,
    val failedCourses: String,
    val equivalentCourses: String,
    val status: String,
)

internal fun SignetsProgram.toJson(): JSONObject = JSONObject()
    .put("name", name).put("code", code).put("average", average).put("accumulatedCredits", accumulatedCredits)
    .put("registeredCredits", registeredCredits).put("completedCourses", completedCourses)
    .put("failedCourses", failedCourses).put("equivalentCourses", equivalentCourses).put("status", status)
internal fun programFromJson(o: JSONObject): SignetsProgram = SignetsProgram(
    name = o.getString("name"),
    code = o.getString("code"),
    average = o.getString("average"),
    accumulatedCredits = o.getString("accumulatedCredits"),
    registeredCredits = o.getString("registeredCredits"),
    completedCourses = o.getString("completedCourses"),
    failedCourses = o.getString("failedCourses"),
    equivalentCourses = o.getString("equivalentCourses"),
    status = o.getString("status"),
)

data class SignetsActivityOccurrence(
    val courseGroup: String,
    val courseName: String,
    val activityName: String,
    val location: String,
    val start: String,
    val end: String,
)

internal fun SignetsActivityOccurrence.toJson(): JSONObject = JSONObject()
    .put("courseGroup", courseGroup).put("courseName", courseName).put("activityName", activityName)
    .put("location", location).put("start", start).put("end", end)
internal fun activityFromJson(o: JSONObject): SignetsActivityOccurrence = SignetsActivityOccurrence(
    courseGroup = o.getString("courseGroup"),
    courseName = o.getString("courseName"),
    activityName = o.getString("activityName"),
    location = o.getString("location"),
    start = o.getString("start"),
    end = o.getString("end"),
)

data class SignetsTeacher(
    val lastName: String,
    val firstName: String,
    val email: String,
    val office: String,
    val phone: String,
) {
    val name: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

data class SignetsCourseHoraire(
    val acronym: String,
    val group: String,
    val activityCode: String,
    val activityName: String,
    val startTime: String,
    val teachers: List<SignetsTeacher>,
)

internal fun SignetsTeacher.toJson(): JSONObject = JSONObject()
    .put("lastName", lastName).put("firstName", firstName).put("email", email)
    .put("office", office).put("phone", phone)
internal fun teacherFromJson(o: JSONObject): SignetsTeacher = SignetsTeacher(
    lastName = o.getString("lastName"),
    firstName = o.getString("firstName"),
    email = o.getString("email"),
    office = o.getString("office"),
    phone = o.getString("phone"),
)

internal fun SignetsCourseHoraire.toJson(): JSONObject = JSONObject()
    .put("acronym", acronym).put("group", group).put("activityCode", activityCode)
    .put("activityName", activityName).put("startTime", startTime)
    .put("teachers", JSONArray(teachers.map { it.toJson() }))
internal fun courseHoraireFromJson(o: JSONObject): SignetsCourseHoraire = SignetsCourseHoraire(
    acronym = o.getString("acronym"),
    group = o.getString("group"),
    activityCode = o.getString("activityCode"),
    activityName = o.getString("activityName"),
    startTime = o.getString("startTime"),
    teachers = o.getJSONArray("teachers").mapObjects { teacherFromJson(it) },
)

data class SignetsReplacedDay(val originalDate: String, val replacementDate: String, val description: String)

internal fun SignetsReplacedDay.toJson(): JSONObject = JSONObject()
    .put("originalDate", originalDate).put("replacementDate", replacementDate).put("description", description)
internal fun replacedDayFromJson(o: JSONObject): SignetsReplacedDay =
    SignetsReplacedDay(o.getString("originalDate"), o.getString("replacementDate"), o.getString("description"))

data class SignetsEvaluation(
    val title: String,
    val mark: Double?,
    val outOf: Double,
    val weight: Double,
    val passMark: Double?,
    val standardDeviation: Double?,
    val median: Double?,
    val percentileRank: Int?,
    val published: Boolean,
    val ignored: Boolean,
    val teacherMessage: String,
    val targetDate: String?,
) {
    val weightedGrade: Double?
        get() = if (mark == null || outOf == 0.0 || weight == 0.0) null else (mark / outOf) * weight
}

internal fun SignetsEvaluation.toJson(): JSONObject = JSONObject()
    .put("title", title).put("outOf", outOf).put("weight", weight)
    .put("published", published).put("ignored", ignored).put("teacherMessage", teacherMessage)
    .apply {
        putOrBlank("mark", mark)
        putOrBlank("passMark", passMark)
        putOrBlank("standardDeviation", standardDeviation)
        putOrBlank("median", median)
        putOrBlank("percentileRank", percentileRank)
        putOrBlank("targetDate", targetDate)
    }
internal fun evaluationFromJson(o: JSONObject): SignetsEvaluation = SignetsEvaluation(
    title = o.getString("title"),
    mark = o.getOrNullDouble("mark"),
    outOf = o.getDouble("outOf"),
    weight = o.getDouble("weight"),
    passMark = o.getOrNullDouble("passMark"),
    standardDeviation = o.getOrNullDouble("standardDeviation"),
    median = o.getOrNullDouble("median"),
    percentileRank = o.getOrNullInt("percentileRank"),
    published = o.getBoolean("published"),
    ignored = o.getBoolean("ignored"),
    teacherMessage = o.getString("teacherMessage"),
    targetDate = o.getOrNullString("targetDate"),
)

data class SignetsCourseSummary(
    val currentMark: Double?,
    val currentMarkPercent: Double?,
    val markOutOf: Double,
    val passMark: Double?,
    val standardDeviation: Double?,
    val median: Double?,
    val percentileRank: Int?,
    val evaluations: List<SignetsEvaluation>,
)

internal fun SignetsCourseSummary.toJson(): JSONObject = JSONObject()
    .put("markOutOf", markOutOf)
    .put("evaluations", JSONArray(evaluations.map { it.toJson() }))
    .apply {
        putOrBlank("currentMark", currentMark)
        putOrBlank("currentMarkPercent", currentMarkPercent)
        putOrBlank("passMark", passMark)
        putOrBlank("standardDeviation", standardDeviation)
        putOrBlank("median", median)
        putOrBlank("percentileRank", percentileRank)
    }
internal fun courseSummaryFromJson(o: JSONObject): SignetsCourseSummary = SignetsCourseSummary(
    currentMark = o.getOrNullDouble("currentMark"),
    currentMarkPercent = o.getOrNullDouble("currentMarkPercent"),
    markOutOf = o.getDouble("markOutOf"),
    passMark = o.getOrNullDouble("passMark"),
    standardDeviation = o.getOrNullDouble("standardDeviation"),
    median = o.getOrNullDouble("median"),
    percentileRank = o.getOrNullInt("percentileRank"),
    evaluations = o.getJSONArray("evaluations").mapObjects { evaluationFromJson(it) },
)

open class SignetsApiException(message: String) : Exception(message)

class SignetsAuthException(message: String) : SignetsApiException(message), app.etsprivate.cache.AuthRequired

class SignetsApi(private val token: String) {
    private val http = OkHttpClient()

    suspend fun getSessions(): List<SignetsSession> = withContext(Dispatchers.IO) {
        app.etsprivate.demo.Demo.source?.let { return@withContext it.sessions() }
        val root = request("/api/Etudiant/listeSessions")
        parseElements(root, "Session").map {
            SignetsSession(
                shortName = it["abrege"].orEmpty(),
                name = it["auLong"].orEmpty(),
                startDate = it["dateDebut"].orEmpty(),
                endDate = it["dateFin"].orEmpty(),
            )
        }
    }

    suspend fun getProfile(): SignetsProfile = withContext(Dispatchers.IO) {
        app.etsprivate.demo.Demo.source?.let { return@withContext it.profile() }
        val fields = parseFields(request("/api/Etudiant/infoEtudiant"))
        SignetsProfile(
            firstName = fields["prenom"].orEmpty().trimEnd(),
            lastName = fields["nom"].orEmpty().trimEnd(),
            permanentCode = fields["codePerm"].orEmpty(),
            universalCode = fields["codeUniversel"].orEmpty(),
            balance = fields["soldeTotal"].orEmpty(),
        )
    }

    suspend fun getCourses(): List<SignetsCourse> = withContext(Dispatchers.IO) {
        app.etsprivate.demo.Demo.source?.let { return@withContext it.courses() }
        parseElements(request("/api/Etudiant/listeCours"), "Cours").map {
            SignetsCourse(
                acronym = it["sigle"].orEmpty(),
                title = it["titreCours"].orEmpty(),
                group = it["groupe"].orEmpty(),
                session = it["session"].orEmpty(),
                credits = it["nbCredits"].orEmpty(),
                grade = it["cote"]?.takeIf { grade -> grade.isNotBlank() },
            )
        }
    }

    suspend fun getSchedule(session: String): List<SignetsScheduleActivity> = withContext(Dispatchers.IO) {
        app.etsprivate.demo.Demo.source?.let { return@withContext it.schedule() }
        val path = "/api/Etudiant/listeHoraireEtProf?session=$session"
        parseElements(request(path), "HoraireActivite").map {
            SignetsScheduleActivity(
                courseAcronym = it["sigle"].orEmpty(),
                courseTitle = it["titreCours"].orEmpty(),
                dayOfWeek = it["jour"]?.toIntOrNull() ?: 0,
                startTime = it["heureDebut"].orEmpty(),
                endTime = it["heureFin"].orEmpty(),
                activityCode = it["codeActivite"].orEmpty(),
                courseGroup = it["groupe"].orEmpty(),
                activityName = it["nomActivite"].orEmpty(),
            )
        }
    }

    suspend fun getPrograms(): List<SignetsProgram> = withContext(Dispatchers.IO) {
        app.etsprivate.demo.Demo.source?.let { return@withContext it.programs() }
        parseElements(request("/api/Etudiant/listeProgrammes"), "Programme").map {
            SignetsProgram(
                name = it["libelle"].orEmpty(),
                code = it["code"].orEmpty(),
                average = it["moyenne"].orEmpty(),
                accumulatedCredits = it["nbCreditsCompletes"].orEmpty(),
                registeredCredits = it["nbCreditsInscrits"].orEmpty(),
                completedCourses = it["nbCrsReussis"].orEmpty(),
                failedCourses = it["nbCrsEchoues"].orEmpty(),
                equivalentCourses = it["nbEquivalences"].orEmpty(),
                status = it["statut"].orEmpty(),
            )
        }
    }

    suspend fun getActivitiesBetween(session: String, startDate: String, endDate: String): List<SignetsActivityOccurrence> =
        withContext(Dispatchers.IO) {
            app.etsprivate.demo.Demo.source?.let { return@withContext it.activities(startDate, endDate) }
            val path = "/api/Etudiant/lireHoraireDesSeances?session=$session&dateDebut=$startDate&dateFin=$endDate"
            parseElements(request(path), "Seance").map {
                SignetsActivityOccurrence(
                    courseGroup = it["coursGroupe"].orEmpty(),
                    courseName = it["libelleCours"].orEmpty(),
                    activityName = it["nomActivite"].orEmpty(),
                    location = it["local"].orEmpty(),
                    start = it["dateDebut"].orEmpty(),
                    end = it["dateFin"].orEmpty(),
                )
            }
        }

    suspend fun getReplacedDays(session: String): List<SignetsReplacedDay> = withContext(Dispatchers.IO) {
        app.etsprivate.demo.Demo.source?.let { return@withContext emptyList() }
        parseElements(request("/api/Etudiant/lireJoursRemplaces?session=$session"), "JourRemplace").map {
            SignetsReplacedDay(
                originalDate = it["dateOrigine"].orEmpty(),
                replacementDate = it["dateRemplacement"].orEmpty(),
                description = it["description"].orEmpty(),
            )
        }
    }

    suspend fun getCourseSummary(session: String, acronym: String, group: String): SignetsCourseSummary? =
        withContext(Dispatchers.IO) {
            app.etsprivate.demo.Demo.source?.let { return@withContext it.summary(acronym) }
            val path = "/api/Etudiant/listeElementsEvaluation?session=$session&sigle=$acronym&groupe=$group"
            val xml = request(path)
            val evaluations = parseElements(xml, "ElementEvaluation").map {
                SignetsEvaluation(
                    title = it["nom"].orEmpty(),
                    mark = it["note"]?.toCommaDouble(),
                    outOf = it["corrigeSur"]?.toCommaDouble() ?: 0.0,
                    weight = it["ponderation"]?.toCommaDouble() ?: 0.0,
                    passMark = it["moyenne"]?.toCommaDouble(),
                    standardDeviation = it["ecartType"]?.toCommaDouble(),
                    median = it["mediane"]?.toCommaDouble(),
                    percentileRank = it["rangCentile"]?.toIntOrNull(),
                    published = it["publie"] == "Oui",
                    ignored = it["ignoreDuCalcul"] == "Oui",
                    teacherMessage = it["messageDuProf"].orEmpty(),
                    targetDate = it["dateCible"]?.takeIf { d -> d.isNotBlank() },
                )
            }
            if (evaluations.isEmpty()) return@withContext null
            val fields = parseFields(xml)
            SignetsCourseSummary(
                currentMark = fields["scoreFinalSur100"]?.toCommaDouble(),
                currentMarkPercent = fields["noteACeJour"]?.toCommaDouble(),
                markOutOf = fields["tauxPublication"]?.toCommaDouble() ?: 0.0,
                passMark = fields["moyenneClasse"]?.toCommaDouble(),
                standardDeviation = fields["ecartTypeClasse"]?.toCommaDouble(),
                median = fields["medianeClasse"]?.toCommaDouble(),
                percentileRank = fields["rangCentileClasse"]?.toIntOrNull(),
                evaluations = evaluations,
            )
        }

    suspend fun getCourseHoraire(session: String, prefix: String): List<SignetsCourseHoraire> =
        withContext(Dispatchers.IO) {
            app.etsprivate.demo.Demo.source?.let { return@withContext it.horaire(prefix) }
            val xml = request("/api/Etudiant/lireHoraire?session=$session&prefixe=$prefix")

            xml.split("<CoursHoraire>").drop(1).mapNotNull { chunk ->

                val element = "<CoursHoraire>${chunk.substringBefore("</CoursHoraire>")}</CoursHoraire>"
                val fields = parseElements(element.substringBefore("<listeProf") + "</CoursHoraire>", "CoursHoraire").firstOrNull()
                    ?: return@mapNotNull null
                SignetsCourseHoraire(
                    acronym = fields["sigle"].orEmpty(),
                    group = fields["groupe"].orEmpty(),
                    activityCode = fields["codeActivite"].orEmpty(),
                    activityName = fields["nomActivite"].orEmpty(),
                    startTime = fields["heureDebut"].orEmpty(),
                    teachers = parseElements(element, "Enseignant").map {
                        SignetsTeacher(
                            lastName = it["nom"].orEmpty(),
                            firstName = it["prenom"].orEmpty(),
                            email = it["courriel"].orEmpty(),
                            office = it["localBureau"].orEmpty(),
                            phone = it["telephone"].orEmpty(),
                        )
                    },
                )
            }
        }

    private fun request(path: String): String {
        val request = Request.Builder()
            .url(BASE + path)
            .header("Accept", "application/xml")
            .header("Authorization", "Bearer $token")
            .build()
        val response = http.newCall(request).execute()
        val body = response.body.string()
        response.close()
        android.util.Log.d("SignetsApi", "GET $path -> ${response.code}: ${body.take(4000)}")
        if (response.code == 401) throw SignetsAuthException("HTTP 401: $body")
        if (!response.isSuccessful) throw SignetsApiException("HTTP ${response.code}: $body")
        return body
    }

    private fun parseElements(xml: String, tag: String): List<Map<String, String>> {
        val parser = newParser(xml)
        val results = mutableListOf<Map<String, String>>()
        var current: MutableMap<String, String>? = null
        var currentChildTag: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == tag) {
                        current = mutableMapOf()
                    } else if (current != null) {
                        currentChildTag = parser.name
                    }
                }
                XmlPullParser.TEXT -> {
                    if (current != null && currentChildTag != null && parser.text.isNotBlank()) {
                        current[currentChildTag] = parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == tag && current != null) {
                        results.add(current)
                        current = null
                    }
                    currentChildTag = null
                }
            }
            event = parser.next()
        }
        return results
    }

    private fun parseFields(xml: String): Map<String, String> {
        val parser = newParser(xml)
        val fields = mutableMapOf<String, String>()
        var currentTag: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> currentTag = parser.name
                XmlPullParser.TEXT -> if (currentTag != null && parser.text.isNotBlank()) fields[currentTag] = parser.text
                XmlPullParser.END_TAG -> currentTag = null
            }
            event = parser.next()
        }
        return fields
    }

    private fun newParser(xml: String): XmlPullParser {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        return parser
    }
}

private fun String.toCommaDouble(): Double? = takeIf { it.isNotBlank() }?.replace(",", ".")?.toDoubleOrNull()

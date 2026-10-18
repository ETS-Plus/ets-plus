package app.etsprivate.ets

import java.text.Normalizer
import java.util.zip.Inflater

internal object ExamSchedulePdf {
    data class Exam(val acronym: String, val date: String, val start: String, val end: String)

    data class Schedule(val session: String, val final: Boolean, val exams: List<Exam>)

    data class Word(val x: Double, val y: Double, val text: String)
    data class Segment(val x1: Double, val y1: Double, val x2: Double, val y2: Double)
    data class Page(val words: List<Word>, val segments: List<Segment>)

    fun parse(pdf: ByteArray): Schedule? =
        runCatching { read(pdf).firstNotNullOfOrNull { runCatching { layout(it) }.getOrNull() } }.getOrNull()

    private val MONTHS = listOf(
        "janvier", "fevrier", "mars", "avril", "mai", "juin",
        "juillet", "aout", "septembre", "octobre", "novembre", "decembre",
    )
    private val WEEKDAYS = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
    private val CODE = Regex("^[A-Z]{2,4}[0-9]{2,4}[A-Z]?$")

    fun layout(page: Page): Schedule? {
        val all = page.words.joinToString(" ") { it.text }
        val plain = fold(all)
        val (season, year) = Regex("examens finaux\\s*-\\s*(automne|hiver|ete)\\s+(\\d{4})").find(plain)
            ?.destructured ?: return null
        val session = when (season) { "automne" -> "A"; "hiver" -> "H"; else -> "E" } + year
        val final = "version finale" in plain

        val verticals = page.segments.filter { abs(it.x1 - it.x2) < 0.5 && abs(it.y1 - it.y2) > 20 }
            .map { it.x1 }.let(::distinct)

        val flat = page.segments.filter { abs(it.y1 - it.y2) < 0.5 }
        val width = (verticals.maxOrNull() ?: return null) - (verticals.minOrNull() ?: return null)
        val horizontals = distinct(flat.map { it.y1 }).filter { y ->
            flat.filter { abs(it.y1 - y) <= 1 }.sumOf { abs(it.x1 - it.x2) } >= width / 2
        }.sortedDescending()

        val headers = page.words.filter { word -> WEEKDAYS.any { fold(word.text).startsWith(it) } }
        if (headers.isEmpty()) return null
        val headerBottom = horizontals.filter { y -> headers.all { it.y > y } }.maxOrNull() ?: return null
        val columns = verticals.zipWithNext().mapNotNull { (left, right) ->
            val inside = page.words.filter { it.x >= left - 1 && it.x < right - 1 && it.y > headerBottom }
            if (inside.none { word -> WEEKDAYS.any { fold(word.text).startsWith(it) } }) return@mapNotNull null
            val text = fold(inside.sortedWith(compareBy({ -it.y }, { it.x })).joinToString(" ") { it.text })
            val match = Regex("(\\d{1,2})\\s+(${MONTHS.joinToString("|")})").find(text) ?: return@mapNotNull null
            val day = match.groupValues[1].toInt()
            val month = MONTHS.indexOf(match.groupValues[2]) + 1
            Triple(left, right, "%s-%02d-%02d".format(year, month, day))
        }
        if (columns.isEmpty()) return null
        val labelRight = columns.minOf { it.first }

        val bands = horizontals.filter { it < headerBottom + 0.5 }.zipWithNext().mapNotNull { (top, bottom) ->
            val label = page.words.filter { it.x < labelRight && it.y < top && it.y > bottom }
                .sortedWith(compareBy({ -it.y }, { it.x })).joinToString(" ") { it.text }
            val (start, end) = slot(label) ?: return@mapNotNull null
            Triple(top, bottom, start to end)
        }
        if (bands.isEmpty()) return null
        val gridBottom = bands.minOf { it.second }

        val durations = durations(
            page.words.filter { it.y < gridBottom }
                .sortedWith(compareBy({ -Math.round(it.y) }, { it.x })).joinToString(" ") { it.text },
        )

        val exams = mutableListOf<Exam>()
        for ((left, right, date) in columns) for ((top, bottom, hours) in bands) {
            val cell = page.words.filter { it.x >= left - 1 && it.x < right - 1 && it.y < top && it.y > bottom }

            val lines = cell.groupBy { Math.round(it.y * 2) }.values
                .map { line -> line.sortedBy { it.x }.joinToString("") { it.text.trim() } }
            for (code in lines.filter { CODE.matches(it) }) {
                val end = durations[code]?.let { clock(minutes(hours.first) + it) } ?: hours.second
                exams += Exam(code, date, hours.first, end)
            }
        }
        if (exams.isEmpty()) return null
        return Schedule(session, final, exams)
    }

    internal fun slot(label: String): Pair<String, String>? {
        val time = "(\\d{1,2})\\s*h\\s*(\\d{2})?"
        val match = Regex("^\\s*$time\\s+a\\s+($time|midi|minuit)\\s*$").find(fold(label)) ?: return null
        val g = match.groupValues
        val start = "%02d:%s".format(g[1].toInt(), g[2].ifEmpty { "00" })
        val end = when (g[3]) {
            "midi" -> "12:00"
            "minuit" -> "24:00"
            else -> "%02d:%s".format(g[4].toInt(), g[5].ifEmpty { "00" })
        }
        return start to end
    }

    internal fun durations(remarks: String): Map<String, Int> {
        val out = mutableMapOf<String, Int>()
        val text = fold(remarks)
        val heads = Regex("duree\\s*(\\d+)\\s*h\\s*(\\d{1,2})?\\s*:").findAll(text).toList()
        heads.forEachIndexed { index, head ->
            val minutes = head.groupValues[1].toInt() * 60 + (head.groupValues[2].toIntOrNull() ?: 0)
            val until = heads.getOrNull(index + 1)?.range?.first ?: text.length

            val upper = text.substring(head.range.last + 1, until).uppercase().replace(" ", "")
            Regex("[A-Z]{2,4}[0-9]{3,4}[A-Z]?").findAll(upper).forEach { out[it.value] = minutes }
        }
        return out
    }

    private fun minutes(clock: String) = clock.take(2).toInt() * 60 + clock.takeLast(2).toInt()
    private fun clock(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

    private fun abs(value: Double) = Math.abs(value)
    private fun distinct(values: List<Double>): List<Double> =
        values.sorted().fold(mutableListOf<Double>()) { kept, v -> if (kept.isEmpty() || v - kept.last() > 1) kept += v; kept }

    fun read(pdf: ByteArray): List<Page> {
        val raw = String(pdf, Charsets.ISO_8859_1)
        val objects = objects(raw, pdf)
        return objects.values.filter { Regex("/Type\\s*/Page(?![s\\w])").containsMatchIn(it.dict) }.mapNotNull { page ->
            val contents = Regex("/Contents\\s*(\\[[^\\]]*\\]|\\d+\\s+\\d+\\s+R)").find(page.dict)?.groupValues?.get(1)
                ?: return@mapNotNull null
            val stream = Regex("(\\d+)\\s+\\d+\\s+R").findAll(contents)
                .mapNotNull { objects[it.groupValues[1].toInt()]?.data }
                .joinToString("\n") { String(it, Charsets.ISO_8859_1) }
            Interpreter().run(stream)
        }
    }

    private class PdfObject(val dict: String, val data: ByteArray?)

    private fun objects(raw: String, bytes: ByteArray): Map<Int, PdfObject> {
        val out = mutableMapOf<Int, PdfObject>()
        val heads = Regex("(\\d+)\\s+\\d+\\s+obj\\b").findAll(raw).toList()
        heads.forEachIndexed { index, head ->
            val bodyStart = head.range.last + 1
            val bodyEnd = heads.getOrNull(index + 1)?.range?.first ?: raw.length
            val body = raw.substring(bodyStart, bodyEnd)
            val streamAt = Regex("stream\\r?\\n").find(body)
            if (streamAt == null) {
                out[head.groupValues[1].toInt()] = PdfObject(body.substringBefore("endobj"), null)
                return@forEachIndexed
            }
            val dict = body.substring(0, streamAt.range.first)
            val from = bodyStart + streamAt.range.last + 1
            val length = Regex("/Length\\s+(\\d+)(?!\\s+\\d+\\s+R)").find(dict)?.groupValues?.get(1)?.toInt()
            val to = if (length != null && from + length <= bytes.size) from + length
            else raw.indexOf("endstream", from).let { end -> if (end > 0 && raw[end - 1] == '\n') end - 1 else end }
                .let { end -> if (end > 0 && raw[end - 1] == '\r') end - 1 else end }
            var data = bytes.copyOfRange(from, to)
            if ("FlateDecode" in dict) data = inflate(data)
            out[head.groupValues[1].toInt()] = PdfObject(dict, data)
        }

        for (holder in out.values.filter { "/ObjStm" in it.dict && it.data != null }) {
            val text = String(holder.data!!, Charsets.ISO_8859_1)
            val first = Regex("/First\\s+(\\d+)").find(holder.dict)?.groupValues?.get(1)?.toInt() ?: continue
            val pairs = text.substring(0, first).trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.chunked(2)
            pairs.forEachIndexed { index, pair ->
                if (pair.size < 2) return@forEachIndexed
                val start = first + pair[1]
                val end = pairs.getOrNull(index + 1)?.getOrNull(1)?.let { first + it } ?: text.length
                if (start in 0..end && end <= text.length) out.putIfAbsent(pair[0], PdfObject(text.substring(start, end), null))
            }
        }
        return out
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            out.write(buffer, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }

    private class Interpreter {
        private val words = mutableListOf<Word>()
        private val segments = mutableListOf<Segment>()
        private var ctm = identity()
        private val saved = ArrayDeque<DoubleArray>()
        private var tm = identity()
        private var tlm = identity()
        private var leading = 0.0
        private var point: Pair<Double, Double>? = null

        fun run(content: String): Page {
            val stack = mutableListOf<Any>()
            for (token in tokens(content)) {
                if (token !is Op) { stack += token; continue }
                operate(token.name, stack)
                stack.clear()
            }
            return Page(words, segments)
        }

        private fun num(stack: List<Any>, fromEnd: Int) = (stack.getOrNull(stack.size - fromEnd) as? Double) ?: 0.0

        private fun operate(op: String, s: List<Any>) {
            when (op) {
                "q" -> saved.addLast(ctm.copyOf())
                "Q" -> ctm = saved.removeLastOrNull() ?: ctm
                "cm" -> ctm = multiply(doubleArrayOf(num(s, 6), num(s, 5), num(s, 4), num(s, 3), num(s, 2), num(s, 1)), ctm)
                "BT" -> { tm = identity(); tlm = identity() }
                "Tm" -> { tm = doubleArrayOf(num(s, 6), num(s, 5), num(s, 4), num(s, 3), num(s, 2), num(s, 1)); tlm = tm.copyOf() }
                "Td" -> move(num(s, 2), num(s, 1))
                "TD" -> { leading = -num(s, 1); move(num(s, 2), num(s, 1)) }
                "TL" -> leading = num(s, 1)
                "T*" -> move(0.0, -leading)
                "Tj" -> show((s.lastOrNull() as? Str)?.text)
                "'" -> { move(0.0, -leading); show((s.lastOrNull() as? Str)?.text) }
                "\"" -> { move(0.0, -leading); show((s.lastOrNull() as? Str)?.text) }
                "TJ" -> show((s.lastOrNull() as? List<*>)?.filterIsInstance<Str>()?.joinToString("") { it.text })
                "m" -> point = transform(num(s, 2), num(s, 1), ctm)
                "l" -> {
                    val to = transform(num(s, 2), num(s, 1), ctm)
                    point?.let { from -> segments += Segment(from.first, from.second, to.first, to.second) }
                    point = to
                }
                "re" -> {
                    val x = num(s, 4); val y = num(s, 3); val w = num(s, 2); val h = num(s, 1)
                    val corners = listOf(x to y, x + w to y, x + w to y + h, x to y + h).map { transform(it.first, it.second, ctm) }
                    for (i in 0..3) {
                        val a = corners[i]; val b = corners[(i + 1) % 4]
                        segments += Segment(a.first, a.second, b.first, b.second)
                    }
                }
            }
        }

        private fun move(tx: Double, ty: Double) {
            tlm = multiply(doubleArrayOf(1.0, 0.0, 0.0, 1.0, tx, ty), tlm)
            tm = tlm.copyOf()
        }

        private fun show(text: String?) {
            if (text.isNullOrBlank()) return
            val at = transform(0.0, 0.0, multiply(tm, ctm))
            words += Word(at.first, at.second, text)
        }
    }

    private class Op(val name: String)
    private class Str(val text: String)

    private fun tokens(content: String): Sequence<Any> = sequence {
        var i = 0
        val arrays = ArrayDeque<MutableList<Any>>()
        suspend fun SequenceScope<Any>.emit(value: Any) {
            if (arrays.isNotEmpty()) arrays.last().add(value) else yield(value)
        }
        while (i < content.length) {
            val c = content[i]
            when {
                c.isWhitespace() -> i++
                c == '%' -> { while (i < content.length && content[i] != '\n' && content[i] != '\r') i++ }
                c == '(' -> {
                    val out = StringBuilder()
                    var depth = 1
                    i++
                    while (i < content.length && depth > 0) {
                        val ch = content[i]
                        when (ch) {
                            '\\' -> {
                                i++
                                val next = content.getOrNull(i) ?: break
                                when (next) {
                                    'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t')
                                    'b' -> out.append('\b'); 'f' -> out.append('\u000C')
                                    in '0'..'7' -> {
                                        var end = i
                                        while (end < content.length && end < i + 3 && content[end] in '0'..'7') end++
                                        out.append(content.substring(i, end).toInt(8).toChar())
                                        i = end - 1
                                    }
                                    '\r', '\n' -> {}
                                    else -> out.append(next)
                                }
                            }
                            '(' -> { depth++; out.append(ch) }
                            ')' -> { depth--; if (depth > 0) out.append(ch) }
                            else -> out.append(ch)
                        }
                        i++
                    }
                    emit(Str(winAnsi(out.toString())))
                }
                c == '<' && content.getOrNull(i + 1) == '<' -> {
                    var depth = 0
                    while (i < content.length) {
                        if (content.startsWith("<<", i)) { depth++; i += 2; continue }
                        if (content.startsWith(">>", i)) { depth--; i += 2; if (depth == 0) break; continue }
                        i++
                    }
                }
                c == '<' -> {
                    val end = content.indexOf('>', i).let { if (it < 0) content.length else it }
                    val hex = content.substring(i + 1, end).filter { !it.isWhitespace() }.let { if (it.length % 2 == 1) it + "0" else it }
                    emit(Str(winAnsi(hex.chunked(2).map { it.toInt(16).toChar() }.joinToString(""))))
                    i = end + 1
                }
                c == '[' -> { arrays.addLast(mutableListOf()); i++ }
                c == ']' -> { val done = arrays.removeLastOrNull(); i++; if (done != null) emit(done) }
                c == '/' -> { i++; while (i < content.length && !isDelimiter(content[i])) i++ }
                c == '+' || c == '-' || c == '.' || c.isDigit() -> {
                    val start = i
                    i++
                    while (i < content.length && (content[i].isDigit() || content[i] == '.')) i++
                    emit(content.substring(start, i).toDoubleOrNull() ?: 0.0)
                }
                else -> {
                    val start = i
                    while (i < content.length && !isDelimiter(content[i])) i++
                    if (i == start) i++
                    val name = content.substring(start, i)
                    if (name == "BI") {
                        val end = content.indexOf("EI", i)
                        i = if (end < 0) content.length else end + 2
                    } else {
                        emit(Op(name))
                    }
                }
            }
        }
    }

    private fun isDelimiter(c: Char) = c.isWhitespace() || c in "()<>[]{}/%"

    private fun winAnsi(text: String): String =
        String(ByteArray(text.length) { text[it].code.toByte() }, charset("windows-1252"))

    private fun identity() = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    private fun multiply(a: DoubleArray, b: DoubleArray) = doubleArrayOf(
        a[0] * b[0] + a[1] * b[2], a[0] * b[1] + a[1] * b[3],
        a[2] * b[0] + a[3] * b[2], a[2] * b[1] + a[3] * b[3],
        a[4] * b[0] + a[5] * b[2] + b[4], a[4] * b[1] + a[5] * b[3] + b[5],
    )

    private fun transform(x: Double, y: Double, m: DoubleArray) = (m[0] * x + m[2] * y + m[4]) to (m[1] * x + m[3] * y + m[5])
}

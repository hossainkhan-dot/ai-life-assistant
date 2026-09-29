package com.hossain.lifeassistant.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ApiException(message: String) : Exception(message)

data class AiItem(
    val type: String,
    val title: String,
    val content: String,
    val date: String?,
    val time: String?,
    val priority: String,
    val needsConfirm: Boolean,
    val note: String
)

sealed interface VoiceState {
    object Idle : VoiceState
    object Recording : VoiceState
    data class Processing(val stage: String) : VoiceState
    data class Confirm(val items: List<AiItem>, val transcript: String) : VoiceState
    data class Message(val text: String, val ok: Boolean) : VoiceState
}

// ---------- Microphone recorder ----------
class VoiceRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()

    fun start() {
        val f = File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val r = newRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(16000)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            f.delete()
            throw e
        }
        rec = r
        file = f
        startedAt = SystemClock.elapsedRealtime()
    }

    /** রেকর্ড শেষ করে ফাইল ফেরত দেয়। খুব ছোট বা ব্যর্থ হলে null। */
    fun stop(): File? {
        val r = rec ?: return null
        val f = file
        rec = null
        file = null
        val length = SystemClock.elapsedRealtime() - startedAt
        var ok = true
        try {
            r.stop()
        } catch (e: RuntimeException) {
            ok = false
        }
        r.release()
        if (!ok || length < 700 || f == null) {
            f?.delete()
            return null
        }
        return f
    }
}

// ---------- OpenAI (Whisper + Chat) ----------
object OpenAi {
    private const val BASE = "https://api.openai.com/v1"
    private val HM = DateTimeFormatter.ofPattern("HH:mm")
    private val TYPES = setOf(
        "diary", "activity", "task", "reminder", "event",
        "birthday", "idea", "shopping", "important", "call"
    )
    private val FUTURE_TYPES = setOf("task", "reminder", "event", "call")

    private val SYSTEM = """
You are the parsing engine of a personal diary and task app.
The user speaks Bengali, English, Banglish or a mix. Split the transcript into separate records.
Return ONLY a JSON object: {"items":[{"type":"","title":"","content":"","date":null,"time":null,"priority":"normal","needs_confirmation":false,"note":""}]}

Rules:
- type is one of: diary, activity, task, reminder, event, birthday, idea, shopping, important, call.
- Things the user already did or happened (past) are "activity". Feelings and thoughts are "diary".
- Something to do in the future without an explicit alert request is "task". "Remind me" / "মনে করিয়ে দিও" is "reminder". Meetings and appointments are "event". Birthdays are "birthday". Phone calls to make are "call". Ideas are "idea". Things to buy are "shopping".
- title: short label. content: the user's meaning in the SAME language and script they used. Never translate. Never add information the user did not say.
- date: YYYY-MM-DD or null. Resolve relative words (today/আজ, tomorrow/কাল/আগামীকাল, yesterday/গতকাল, next Friday/আগামী শুক্রবার) using the current date given. Convert Bengali digits to normal digits.
- time: 24-hour HH:mm or null. Fill it only if the user said a clock time. সকাল ৭টা = 07:00, দুপুর ১টা = 13:00, বিকেল ৫টা = 17:00, সন্ধ্যা ৭টা = 19:00, রাত ১০টা = 22:00. If the user gave only a vague period like "সকালে" or a number with no period (like "৫টায়"), set time null and needs_confirmation true.
- Keep the order in which the user said things.
- NEVER invent facts, names, dates or times. If unsure, use null, set needs_confirmation true and write a short note in the user's language.
- priority is "high" only if the user says it is urgent or important, otherwise "normal".
- If there is nothing meaningful, return {"items":[]}.
""".trimIndent()

    fun transcribe(key: String, file: File): String {
        val boundary = "----LifeAssistant" + System.currentTimeMillis()
        val conn = URL("$BASE/audio/transcriptions").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20000
        conn.readTimeout = 90000
        conn.setRequestProperty("Authorization", "Bearer $key")
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        DataOutputStream(conn.outputStream).use { out ->
            fun field(name: String, value: String) {
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                out.write(value.toByteArray(Charsets.UTF_8))
                out.writeBytes("\r\n")
            }
            field("model", "whisper-1")
            field("response_format", "json")
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"voice.m4a\"\r\n")
            out.writeBytes("Content-Type: audio/mp4\r\n\r\n")
            file.inputStream().use { it.copyTo(out) }
            out.writeBytes("\r\n--$boundary--\r\n")
            out.flush()
        }
        val text = finish(conn)
        return JSONObject(text).optString("text", "").trim()
    }

    fun analyze(key: String, transcript: String, today: LocalDate, now: LocalTime): List<AiItem> {
        val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val userMsg = "Current date: $today ($dayName)\nCurrent time: ${now.format(HM)}\nTranscript:\n$transcript"
        val body = JSONObject()
            .put("model", "gpt-4o-mini")
            .put("temperature", 0)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM))
                    .put(JSONObject().put("role", "user").put("content", userMsg))
            )
        val conn = URL("$BASE/chat/completions").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20000
        conn.readTimeout = 90000
        conn.setRequestProperty("Authorization", "Bearer $key")
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val resp = finish(conn)
        val content = JSONObject(resp)
            .getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
        return parseItems(content, today)
    }

    private fun parseItems(json: String, today: LocalDate): List<AiItem> {
        val arr = JSONObject(json).optJSONArray("items") ?: return emptyList()
        val out = mutableListOf<AiItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            var type = o.str("type")?.lowercase() ?: "diary"
            if (type !in TYPES) type = "diary"
            var needs = o.optBoolean("needs_confirmation", false)
            var note = o.str("note") ?: ""

            val rawDate = o.str("date")
            val d = rawDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (rawDate != null && d == null) {
                needs = true; note = addNote(note, "তারিখ বোঝা যায়নি")
            }
            if (d != null && d.isBefore(today) && type in FUTURE_TYPES) {
                needs = true; note = addNote(note, "তারিখটি অতীতের")
            }

            val rawTime = o.str("time")
            val t = rawTime
                ?.let { runCatching { LocalTime.parse(it.padStart(5, '0')) }.getOrNull() }
                ?.format(HM)
            if (rawTime != null && t == null) {
                needs = true; note = addNote(note, "সময় বোঝা যায়নি")
            }
            if (type == "reminder" && t == null) {
                needs = true; note = addNote(note, "সময় বলা হয়নি")
            }

            val content0 = o.str("content")
            val title0 = o.str("title")
            if (content0 == null && title0 == null) continue
            val content = content0 ?: title0!!
            val title = title0 ?: content.take(40)
            val priority = if (o.str("priority")?.lowercase() == "high") "High" else "Normal"
            out.add(AiItem(type, title, content, d?.toString(), t, priority, needs, note))
        }
        return out
    }

    private fun addNote(old: String, add: String) = if (old.isBlank()) add else "$old; $add"

    private fun JSONObject.str(k: String): String? {
        if (!has(k) || isNull(k)) return null
        val s = optString(k, "").trim()
        return if (s.isEmpty() || s.equals("null", ignoreCase = true)) null else s
    }

    private fun finish(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrNull()
            throw ApiException(
                when (code) {
                    401 -> "API key ঠিক নয় (401)। Settings-এ আবার দিন"
                    429 -> "Credit শেষ বা অনেক দ্রুত request (429)। OpenAI billing দেখুন"
                    else -> "Server error $code ${msg ?: ""}"
                }
            )
        }
        return text
    }
}

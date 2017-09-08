package utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rethinkdb.net.Cursor
import commands.info.formatter
import main.conn
import main.r
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import org.apache.commons.lang3.exception.ExceptionUtils
import org.json.simple.JSONObject
import translation.Languages
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.management.Attribute
import javax.management.ObjectName

class Quadruple<A, B, C, D>(var first: A, var second: B, var third: C, var fourth: D)
class Pair2(val first1: Any?, val second1: Any?)

var logChannel: TextChannel? = null

val random = Random()
private val gsons = listOf(GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create(),
        GsonBuilder().serializeSpecialFloatingPointValues().create())

fun Throwable.log() {
    logChannel!!.sendMessage("```${ExceptionUtils.getStackTrace(this)}```").queue()
}

fun <E> MutableList<E>.shuffle(): MutableList<E> {
    Collections.shuffle(this)
    return this
}

fun <E> List<E>.get(e: E): Pair<Int, E>? {
    var index = 0
    forEach { if (it == e) return Pair(index, it) else index++ }
    return null
}

/**
 * Append [List] items using a comma to seperate them
 */
fun List<String>.stringify(): String {
    if (size == 0) return "none"
    val builder = StringBuilder()
    forEach { builder.append(it + ", ") }
    return builder.removeSuffix(", ").toString()
}

/**
 * Creates an equivalent string out of the constituent strings
 */
fun List<String>.concat(): String {
    val builder = StringBuilder()
    forEach { builder.append("$it ") }
    return builder.removeSuffix(" ").toString()
}

fun <E> MutableList<E>.addIfNotExists(e: E) {
    if (!contains(e)) add(e)
}

fun Any.insert(table: String) {
    r.table(table).insert(r.json(getGson().toJson(this))).runNoReply(conn)
}

fun <T> asPojo(map: HashMap<*, *>?, tClass: Class<T>): T? {
    return getGson().fromJson(JSONObject.toJSONString(map), tClass)
}

fun <T> Any.queryAsArrayList(t: Class<T>): MutableList<T?> {
    val cursor = this as Cursor<HashMap<*, *>>
    val tS = mutableListOf<T?>()
    cursor.forEach { hashMap -> tS.add(asPojo(hashMap, t)) }
    cursor.close()
    return tS
}

fun Long.readableDate(): String {
    return "${Date.from(Instant.ofEpochMilli(this)).toLocaleString()} EST"
}

fun Int.format(): String {
    return formatter.format(this)
}


fun Double.format(): String {
    return formatter.format(this)
}

fun Long.format(): String {
    return formatter.format(this)
}

fun String.shortenIf(numChars: Int): String {
    return if (length <= numChars) this
    else substring(0, numChars)
}

fun <K> MutableList<K>.forEach(consumer: (MutableIterator<K>, current: K) -> Unit) {
    val iterator = iterator()
    while (iterator.hasNext()) consumer.invoke(iterator, iterator.next())
}

fun <K> MutableMap<K, Int>.incrementValue(key: K): Int {
    val value = putIfAbsent(key, 0) ?: 0
    replace(key, value + 1)
    return value
}

fun PlayerData.update() {
    r.table("playerData").get(id).update(r.json(getGson().toJson(this))).runNoReply(conn)
}

fun GuildData.update() {
    r.table("guilds").get(id).update(r.json(getGson().toJson(this))).runNoReply(conn)
}

fun Long.formatMinSec(): String {
    val seconds = this % 60
    val minutes = (this % 3600) / 60
    return if (minutes.compareTo(0) == 0) "$seconds seconds"
    else "$minutes minutes, $seconds seconds"
}

fun getGson(): Gson {
    return gsons[random.nextInt(gsons.size)]
}

fun String.trReplace(guild: Guild, param: Int, new: String): String {
    val split = split("{$param}")
    when (guild.getLanguage()) {
        Languages.FRENCH.language -> {
            if (split[0].endsWith(" le ", true) || split[0].endsWith(" la ", true)
                    || split[0].endsWith(" le **", true) || split[0].endsWith(" la **", true)
                    || split[0].endsWith(" le *", true) || split[0].endsWith(" la *", true)
                    &&
                    (new.startsWith("a", true) || new.startsWith("e", true))) {
                return split[0].replaceAfterLast(" le ", "l'${if (split[0].endsWith("**")) "**" else if (split[0].endsWith("*")) "*" else ""}$new${split[1]}")
                        .replace(" le l'", " l'").replace(" la l'", " l'")
            }
        }
    }
    return replace("{$param}", new)
}

val waiterExecutor = Executors.newScheduledThreadPool(3)!!
fun after(consumer: () -> Unit, time: Int, unit: TimeUnit = TimeUnit.SECONDS) {
    waiterExecutor.schedule({ consumer.invoke() }, time.toLong(), unit)
}

fun <E> List<E>.limit(int: Int): List<E> {
    return if (size <= int) this
    else subList(0, int - 1)
}

inline fun <E, T> Map<E, T>.forEachIndexed(function: (index: Int, E, T) -> Unit) {
    var current = 0
    forEach {
        function.invoke(current, it.key, it.value)
        current++
    }
}

fun Map<*, *>.sort(descending: Boolean = true): MutableMap<*, *> {
    var list = toList().sortedWith(compareBy { (it.second as Number).toDouble() })
    if (descending) list = list.reversed()
    return list.toMap().toMutableMap()
}

fun Any.toJson(): String {
    return getGson().toJson(this)
}

fun <T> MutableList<T>.without(t: T): MutableList<T> {
    val n = mutableListOf<T>()
    n.addAll(this)
    n.remove(t)
    return n
}

fun List<String>.containsEq(string: String): Boolean {
    forEach { if (string.toLowerCase() == it.toLowerCase()) return true }
    return false
}

/**
 * Full credit goes to http://stackoverflow.com/questions/18489273/how-to-get-percentage-of-cpu-usage-of-os-from-java
 */
fun getProcessCpuLoad(): Double {
    val mbs = ManagementFactory.getPlatformMBeanServer()
    val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
    val list = mbs.getAttributes(name, arrayOf("ProcessCpuLoad"))

    if (list.isEmpty()) return java.lang.Double.NaN

    val att = list[0] as Attribute
    val value = att.value as Double

    // usually takes a couple of seconds before we get real values
    if (value == -1.0) return Double.NaN
    // returns a percentage value with 1 decimal point precision
    return (value * 1000).toInt() / 10.0
}

/**
 * Thanks StackOverflow
 */
fun replaceLast(text: String, regex: String, replacement: String): String {
    return text.replaceFirst(("(?s)(.*)" + regex).toRegex(), "$1$replacement")
}
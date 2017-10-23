package utils.functionality

import com.google.gson.GsonBuilder
import com.rethinkdb.net.Cursor
import commands.info.formatter
import main.conn
import main.r
import main.waiter
import net.dv8tion.jda.core.entities.TextChannel
import org.apache.commons.lang3.exception.ExceptionUtils
import org.json.simple.JSONObject
import java.lang.management.ManagementFactory
import java.net.URLEncoder
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.management.Attribute
import javax.management.ObjectName

class Quadruple<A, B, C, D>(var first: A, var second: B, var third: C, var fourth: D)

var logChannel: TextChannel? = null

val random = Random()
val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()

fun Throwable.log() {
    logChannel?.sendMessage("```${ExceptionUtils.getStackTrace(this)}```")?.queue() ?: printStackTrace()
}

fun Float.format(): String {
    return "%.2f".format(this)
}

fun <E> MutableList<E>.shuffle(): MutableList<E> {
    Collections.shuffle(this)
    return this
}

fun <E> List<E>.getWithIndex(e: E): Pair<Int, E>? {
    var index = 0
    forEach { if (it == e) return Pair(index, it) else index++ }
    return null
}

/**
 * Append a [List] of items using a comma as a separator
 */
fun <T> List<T>.stringify(): String {
    return if (size == 0) "none" else map { it.toString() }.stream().collect(Collectors.joining(", "))
}

/**
 * Creates an equivalent string out of the constituent strings
 */
fun List<String>.concat(): String {
    return stream().collect(Collectors.joining(" "))
}


fun String.encode(): String {
    return URLEncoder.encode(this, "UTF-8")
}

fun <E> MutableList<E>.putIfNotThere(e: E) {
    if (!contains(e)) add(e)
}

fun Any.insert(table: String) {
    r.table(table).insert(r.json(gson.toJson(this))).runNoReply(conn)
}

fun <T> asPojo(map: HashMap<*, *>?, tClass: Class<T>): T? {
    return gson.fromJson(JSONObject.toJSONString(map), tClass)
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

fun <K> MutableMap<K, Int>.increment(key: K): Int {
    val value = putIfAbsent(key, 0) ?: 0
    replace(key, value + 1)
    return value
}

fun Long.toMinutesAndSeconds(): String {
    val seconds = this % 60
    val minutes = (this % 3600) / 60
    return if (minutes.compareTo(0) == 0) "$seconds seconds"
    else "$minutes minutes, $seconds seconds"
}

fun after(consumer: () -> Unit, time: Int, unit: TimeUnit = TimeUnit.SECONDS) {
    waiter.executor.schedule({ consumer.invoke() }, time.toLong(), unit)
}

fun Double.toMinutes(): Int {
    return ((this % 1) * 60).toInt()
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

fun <A, B : Number> Map<A, B>.sort(descending: Boolean = true): MutableMap<A, B> {
    var list = toList().sortedWith(compareBy { (it.second as Number).toDouble() })
    if (descending) list = list.reversed()
    return list.toMap().toMutableMap()
}

fun Any.toJson(): String {
    return gson.toJson(this)
}

fun <T> MutableList<T>.without(index: Int): MutableList<T> {
    removeAt(index)
    return this
}

fun <T> MutableList<T>.without(t: T): MutableList<T> {
    remove(t)
    return this
}

fun List<String>.containsEqualsIgnoreCase(string: String): Boolean {
    forEach { if (string.toLowerCase().equals(it, true)) return true }
    return false
}

/**
 * Full credit goes to http://stackoverflow.com/questions/18489273/how-to-getWithIndex-percentage-of-cpu-usage-of-os-from-java
 */
fun getProcessCpuLoad(): Double {
    val mbs = ManagementFactory.getPlatformMBeanServer()
    val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
    val list = mbs.getAttributes(name, arrayOf("ProcessCpuLoad"))

    if (list.isEmpty()) return java.lang.Double.NaN

    val att = list[0] as Attribute
    val value = att.value as Double

    // usually takes a couple of seconds before we getWithIndex real values
    if (value == -1.0) return Double.NaN
    // returns a percentage value with 1 decimal point precision
    return (value * 1000).toInt() / 10.0
}

fun replaceLast(text: String, regex: String, replacement: String): String {
    return text.replaceFirst(("(?s)(.*)" + regex).toRegex(), "$1$replacement")
}
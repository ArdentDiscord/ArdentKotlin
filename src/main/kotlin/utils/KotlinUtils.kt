package utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rethinkdb.net.Cursor
import commands.info.formatter
import main.conn
import main.r
import net.dv8tion.jda.core.entities.TextChannel
import org.apache.commons.lang3.exception.ExceptionUtils
import org.json.simple.JSONObject
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.management.Attribute
import javax.management.ObjectName
import kotlin.collections.HashMap


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

fun List<String>.stringify(): String {
    if (size == 0) return "none"
    val builder = StringBuilder()
    forEach { builder.append(it + ", ") }
    return builder.removeSuffix(", ").toString()
}

fun List<String>.concat(): String {
    val builder = StringBuilder()
    forEach { builder.append("$it ") }
    return builder.removeSuffix(" ").toString()
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
    if (minutes.compareTo(0) == 0) return "$seconds seconds"
    else return "$minutes minutes, $seconds seconds"
}

fun getGson(): Gson {
    return gsons[random.nextInt(gsons.size)]
}

val waiterExecutor = Executors.newScheduledThreadPool(3)!!
fun after(consumer: () -> Unit, time: Int, unit: TimeUnit = TimeUnit.SECONDS) {
    waiterExecutor.schedule({ consumer.invoke() }, time.toLong(), unit)
}

fun <E> List<E>.limit(int: Int) : List<E> {
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
    return this
}

fun List<String>.containsEq(string: String): Boolean {
    forEach { if (string.toLowerCase() == it.toLowerCase()) return true }
    return false
}

/**
 * Full credit goes to http://stackoverflow.com/questions/18489273/how-to-get-percentage-of-cpu-usage-of-os-from-java
 */
@Throws(Exception::class)
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
 * Credit mfulton26 @ https://stackoverflow.com/questions/34498368/kotlin-convert-large-list-to-sublist-of-set-partition-size
 */
fun <T> List<T>.collate(size: Int): List<List<T>> {
    require(size > 0)
    return if (isEmpty()) {
        emptyList()
    } else {
        (0..lastIndex / size).map {
            val fromIndex = it * size
            val toIndex = Math.min(fromIndex + size, this.size)
            subList(fromIndex, toIndex)
        }
    }
}

fun replaceAll(builder: StringBuilder, from: String, to: String) {
    var cont = true
    while (cont) {
        val temp = builder.indexOf(from)
        if (temp != -1) builder.replace(temp, temp + from.length, to)
        else cont = false
    }
}

fun replaceFirst(text: String, searchString: String, replacement: String): String {
    return org.apache.commons.lang3.StringUtils.replaceOnce(text, searchString, replacement)
}

/**
 * Thanks StackOverflow
 */
fun replaceLast(text: String, regex: String, replacement: String): String {
    return text.replaceFirst(("(?s)(.*)" + regex).toRegex(), "$1$replacement")
}
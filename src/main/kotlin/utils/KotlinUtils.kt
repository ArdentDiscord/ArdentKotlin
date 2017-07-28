package utils

import com.google.gson.Gson
import com.rethinkdb.net.Cursor
import commands.games.CoinflipGame
import main.conn
import main.r
import net.dv8tion.jda.core.entities.User
import org.json.simple.JSONObject
import java.util.*
import kotlin.collections.HashMap
import java.util.LinkedHashMap
import java.util.function.Supplier
import java.util.stream.Collectors



private val random = Random()
private val gsons = listOf(Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson())

fun <E> MutableList<E>.shuffle() : MutableList<E> {
    Collections.shuffle(this)
    return this
}

fun List<String>.stringify() : String {
    if (size == 0) return "none"
    val builder = StringBuilder()
    forEach { builder.append(it + ", ") }
    return builder.removeSuffix(", ").toString()
}

fun List<String>.concat() : String {
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

fun <T> Any.queryAsArrayList(t: Class<T>): ArrayList<T?> {
    val cursor = this as Cursor<HashMap<*, *>>
    val tS = ArrayList<T?>()
    cursor.forEach { hashMap -> tS.add(asPojo(hashMap, t)) }
    return tS
}

fun PlayerData.update() {
    r.table("playerData").get(id).update(r.json(getGson().toJson(this))).runNoReply(conn)
}

fun GuildData.update() {
    r.table("guilds").get(id).update(r.json(getGson().toJson(this))).runNoReply(conn)
}

fun Long.formatMinSec() : String {
    val seconds = this % 60
    val minutes = (this % 3600) / 60
    if (minutes.compareTo(0) == 0) return "$seconds seconds"
    else return "$minutes minutes, $seconds seconds"
}

fun getGson(): Gson {
    return gsons[random.nextInt(gsons.size)]
}

fun MutableList<CoinflipGame.Round>.mapScores() : MutableMap<String, Int> {
    val scores = hashMapOf<String, Int>()
    forEach { round ->
        round.winners.forEach {
            scores.putIfAbsent(it, 0)
            scores.replace(it, scores[it]!! + 1)
        }
        round.losers.forEach {
            scores.putIfAbsent(it, 0)
        }
    }
    return JavaUtils.sortByValue(scores)
}


fun <T> MutableList<T>.without(t: T) : MutableList<T> {
    this.remove(t)
    return this
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
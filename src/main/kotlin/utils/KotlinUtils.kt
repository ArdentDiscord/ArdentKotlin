package utils

import com.google.gson.Gson
import com.rethinkdb.net.Cursor
import main.conn
import main.r
import org.json.simple.JSONObject
import java.util.*

private val random = Random()
private val gsons = listOf(Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson())

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

fun getGson(): Gson {
    return gsons[random.nextInt(gsons.size)]
}
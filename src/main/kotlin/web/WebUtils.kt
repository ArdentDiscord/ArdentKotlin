package web

import com.google.gson.JsonSyntaxException
import main.config
import main.conn
import main.jdas
import main.r
import net.dv8tion.jda.core.entities.User
import org.jsoup.Jsoup
import utils.*
import java.util.*

val dapi = "https://discordapp.com/api"

data class Failure(val failure: String)

class Setting(val route: String, val name: String, val description: String, vararg val optionalParameters: String) {
    override fun toString(): String {
        return getGson().toJson(this)
    }
}

data class Token(val access_token: String, val token_type: String, val expires_in: Int, val refresh_token: String, val scope: String) {
    fun getScopes(): MutableList<Scope> {
        val list = mutableListOf<Scope>()
        val split = scope.split(" ")
        split.forEach { s ->
            when (s) {
                "identify" -> list.add(Scope.IDENTIFY)
                "email" -> list.add(Scope.EMAIL)
                "guilds" -> list.add(Scope.GUILDS)
            }
        }
        return list
    }
}

data class IdentificationObject(val username: String, val verified: Boolean, val mfa_enabled: Boolean, val id: String, val avatar: String,
                                val discriminator: String)

data class Credential(val code: String, val id: String)
enum class Scope(val route: String) {
    CONNECTIONS("/users/@me/connections"),
    EMAIL("/users/@me"),
    IDENTIFY("/users/@me"),
    GUILDS("/users/@me/guilds"),
    BOT_INFORMATION("/oauth2/applications/@me")
    ;

    override fun toString(): String {
        return route
    }
}

fun identityObject(access_token: String): IdentificationObject? {
    val obj = getGson().fromJson(retrieveObject(access_token, Scope.IDENTIFY), IdentificationObject::class.java)
    if (obj.id == null) return null /* This is possible */
    else return obj
}

fun retrieveObject(access_token: String, scope: Scope): String {
    return Jsoup.connect("$dapi$scope").ignoreContentType(true).ignoreHttpErrors(true)
            .header("authorization", "Bearer $access_token")
            .header("cache-control", "no-cache")
            .get()
            .text()
}

fun retrieveToken(code: String): Token? {
    val response = Jsoup.connect("$dapi/oauth2/token").ignoreContentType(true).ignoreHttpErrors(true)
            .header("content-type", "application/x-www-form-urlencoded")
            .header("authorization", "Bearer $code")
            .header("cache-control", "no-cache")
            .data("client_id", jdas[0].selfUser.id)
            .data("client_secret", config.getValue("client_secret"))
            .data("grant_type", "authorization_code")
            .data("redirect_uri", loginRedirect)
            .data("code", code)
            .post()
    try {
        println(response.text())
        val data = getGson().fromJson(response.text(), Token::class.java)
        if (data.access_token == null) return null // this is a possibility due to issues with the kotlin compiler
        else return data /* verified non null object */
    } catch (e: JsonSyntaxException) {
        e.log()
        return null
    }
}

fun getTickets(): List<SupportTicket> {
    return r.table("supportTickets").run<Any>(conn).queryAsArrayList(SupportTicketModel::class.java).map { it!!.toSupportTicket() }
}

fun getOpenTickets(): List<SupportTicket> {
    return r.table("supportTickets").filter(r.hashMap("open", true)).run<Any>(conn).queryAsArrayList(SupportTicketModel::class.java).map { it!!.toSupportTicket() }
}
fun User.getTickets(): List<SupportTicket> {
    return r.table("supportTickets").filter(r.hashMap("user", id)).run<Any>(conn).queryAsArrayList(SupportTicketModel::class.java).map { it!!.toSupportTicket() }
}

data class SupportTicket(val user: User, val title: String, val open: Boolean, val messages: List<SupportMessage>, val adminRe: Boolean, val id: String, val date: String)
data class SupportMessage(val writer: User, val userMessage: Boolean, val content: String, val date: String, val id: String) {
    init {
        println(this)
    }
}

data class SupportTicketModel(val user: String, val title: String, val open: Boolean, val userResponses: MutableList<SupportMessageModel> = mutableListOf(),
                              val administratorResponses: MutableList<SupportMessageModel> = mutableListOf(), val id: String = r.uuid().run<String>(conn), val date: Long = System.currentTimeMillis()) {
    fun toSupportTicket(): SupportTicket {
        val messages = mutableListOf<SupportMessageModel>()
        userResponses.toCollection(messages)
        administratorResponses.toCollection(messages)
        Collections.sort(messages) { o1, o2 ->
            when {
                o1.date < o2.date -> -1
                o1.date > o2.date -> 1
                else -> 0
            }
        }
        return SupportTicket(user.toUser()!!, title, open, messages.map { it.toSupportMessage() }, administratorResponses.size > 0, id, date.readableDate())
    }

    fun add(originalMessage: String): SupportTicketModel {
        userResponses.add(SupportMessageModel(user, true, originalMessage, System.currentTimeMillis()))
        return this
    }
}

data class SupportMessageModel(val writer: String, val userMessage: Boolean, val content: String, val date: Long, val id: String = r.uuid().run(conn)) {
    fun toSupportMessage(): SupportMessage {
        val writerUser = writer.toUser()!!
        return SupportMessage(writerUser, userMessage, content, date.readableDate(), id)
    }
}
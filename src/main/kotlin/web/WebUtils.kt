package web

import com.google.gson.JsonSyntaxException
import main.config
import main.jdas
import org.jsoup.Jsoup
import utils.getGson

val dapi = "https://discordapp.com/api"

data class Failure(val failure: String)

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

fun identityObject(access_token: String) : IdentificationObject? {
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
            .data("redirect_uri", "https://ardentbot.com/onboard")
            .data("code", code)
            .post()
    try {
        val data = getGson().fromJson(response.text(), Token::class.java)
        if (data.access_token == null) return null // this is a possibility due to issues with the kotlin compiler
        else return data /* verified non null object */
    } catch (e: JsonSyntaxException) {
        return null
    }
}
package web

import commands.administrate.staff
import main.conn
import spark.*
import spark.Spark.*
import main.factory
import main.r
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import org.apache.commons.io.IOUtils
import utils.*
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicLong

val webCalls = AtomicLong(0)
val apiCredentials = hashMapOf<String /* Code given to frontend */, String /* User ID */>()

val settings = mutableListOf<Setting>()

class Web {
    init {
        settings.add(Setting("/defaultrole", "Default Role", "Remove or set the role given to all new members. Can fail if Ardent doesn't " +
                "have sufficient permissions to give them the role."))
        settings.add(Setting("/joinmessage", "Join Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/leavemessage", "Leave Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/music/announcenewmusic", "Announce Songs at Start", "Choose whether you want to allow "))

        val credentials = r.table("apiCodes").run<Any>(conn).queryAsArrayList(Credential::class.java)
        credentials.forEach { if (it != null) apiCredentials.put(it.code, it.id) }
        port(443)
        secure("/root/Ardent/keystore.p12", "ardent", null, null)
        internalServerError({ _, _ ->
            "Well, you fucked up. Congrats!".toJson()
        })
        get("/support", { _, response -> response.redirect("https://discord.gg/rfGSxNA") })
        get("/invite", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=bot&client_id=339101087569281045&permissions=269574192") })
        path("/api", {
            before("/*", { _, _ -> webCalls.getAndIncrement() })
            path("/oauth", {
                get("/login", { request, response ->
                    val code = request.params("code")
                    if (code == null) Failure("Invalid URL").toJson()
                    response.redirect("https://ardentbot.com/onboard/$code")
                })
                post("/login", { request, response ->
                    if (request.headers("code") == null) Failure("No code provided.").toJson()
                    else {
                        val code = request.headers("code")
                        val token = retrieveToken(code)
                        if (token == null) Failure("Provided an invalid code.").toJson()
                        else {
                            val identification = identityObject(token.access_token)
                            if (identification == null) Failure("The user didn't allow the Identify scope.").toJson()
                            else {
                                apiCredentials.put(code, identification.id)
                                Credential(code, identification.id).insert("apiCodes")
                                identification.toJson()
                            }
                        }
                    }
                })
            })
            path("/public", {
                get("/status", { _, _ -> Internals().toJson() })
                get("/commands", { _, _ -> factory.commands })
                get("/staff", { _, _ -> staff.toJson() })
                path("/data", {
                    path("/user", {
                        get("/*/*", { request, _ ->
                            val id = request.splat()[0]
                            val guildId = request.splat()[1]
                            val guild: Guild? = getGuildById(guildId)
                            if (guild == null) createUserModel(id).toJson()
                            else {
                                val member = guild.getMemberById(id)
                                if (member == null) Failure("invalid user specified")
                                else {
                                    GuildMemberModel(id, member.effectiveName, member.roles.map { it.name },
                                            member.hasOverride(guild.publicChannel, failQuietly = true)).toJson()
                                }
                            }
                        })
                    })
                })

            })
            path("/internal", {
                path("/settings", {
                    get("/", { request, response ->

                    })
                })

            })
        })
    }
}

fun createUserModel(id: String): Any {
    val user: User = getUserById(id) ?: return Failure("invalid user requested").toJson()
    return UserModel(id, user.name, user.discriminator, user.effectiveAvatarUrl)
}

data class UserModel(val id: String, val username: String, val discrim: String, val avatar: String)

data class GuildMemberModel(val id: String, val effectiveName: String, val roles: List<String>, val hasOverride: Boolean)


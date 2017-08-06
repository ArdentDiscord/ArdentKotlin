package web

import commands.administrate.Staff
import commands.administrate.filterByRole
import commands.administrate.staff
import main.factory
import main.jdas
import main.test
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import spark.ModelAndView
import spark.Service
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine
import utils.*

val settings = mutableListOf<Setting>()

val handlebars = HandlebarsTemplateEngine()

val loginRedirect = "https://ardentbot.com/api/oauth/login"

class Web {
    init {
        if (!test) {
            val httpServer = Service.ignite().port(80)
            httpServer.before { request, response ->
                val url = request.url()
                response.redirect("https://${url.split("http://")[1]}")
            }
            port(443)
        } else port(80)

        settings.add(Setting("/defaultrole", "Default Role", "Remove or set the role given to all new members. Can fail if Ardent doesn't " +
                "have sufficient permissions to give them the role."))
        settings.add(Setting("/joinmessage", "Join Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/leavemessage", "Leave Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/music/announcenewmusic", "Announce Songs at Start", "Choose whether you want to allow "))

        staticFiles.location("/public")
        secure("/root/Ardent/keystore.p12", "ardent", null, null)

        notFound({ request, response ->
            response.redirect("/404")
        })


        get("/", { request, response ->
            val map = hashMapOf<String, Any>()
            val session = request.session()
            val user = session.attribute<User>("user")
            if (user == null) map.put("validSession", false)
            else {
                map.put("validSession", true)
                map.put("user", user)
            }
            map.put("title", "Home")
            ModelAndView(map, "index.hbs")
        }, handlebars)
        get("/welcome", { request, response ->
            val map = hashMapOf<String, Any>()
            val session = request.session()
            val user = session.attribute<User>("user")
            if (user == null) map.put("validSession", false)
            else {
                map.put("validSession", true)
                map.put("user", user)
            }
            map.put("title", "Welcome!")
            ModelAndView(map, "welcome.hbs")
        }, handlebars)
        get("/status", { request, response ->
            val internals = Internals()
            val map = hashMapOf<String, Any>()
            map.put("title", "Status")
            map.put("usedRam", internals.ramUsage.first)
            map.put("totalRam", internals.ramUsage.second)
            map.put("cpuUsage", internals.cpuUsage)
            map.put("apiCalls", internals.apiCalls.format())
            map.put("messagesReceived", internals.messagesReceived)
            map.put("commandsReceived", internals.commandsReceived)
            map.put("guilds", internals.guilds.format())
            map.put("users", internals.users.format())
            map.put("loadedMusicPlayers", internals.loadedMusicPlayers)
            map.put("arePeoplePlayingMusic", internals.loadedMusicPlayers > 0)
            map.put("queueLength", internals.queueLength.format())
            map.put("uptime", internals.uptimeFancy)
            ModelAndView(map, "status.hbs")
        }, handlebars)
        get("/staff", { request, response ->
            val map = hashMapOf<String, Any>()
            map.put("title", "Staff")
            map.put("administrators", staff.filterByRole(Staff.StaffRole.ADMINISTRATOR).map { it.id.toUser() })
            map.put("moderators", staff.filterByRole(Staff.StaffRole.MODERATOR).map { it.id.toUser() })
            map.put("helpers", staff.filterByRole(Staff.StaffRole.HELPER).map { it.id.toUser() })
            ModelAndView(map, "staff.hbs")
        }, handlebars)
        get("/support", { _, response -> response.redirect("https://discord.gg/rfGSxNA") })
        get("/invite", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=bot&client_id=339101087569281045&permissions=269574192&redirect_uri=https://ardentbot.com/welcome&response_type=code") })
        get("/login", { request, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=identify&client_id=${jdas[0].selfUser.id}&response_type=code&redirect_uri=$loginRedirect") })
        get("/patreon", { request, response -> response.redirect("https://patreon.com/ardent") })
        get("/404", { request, response ->
            val map = hashMapOf<String, Any>()
            map.put("title", "404 Not Found")
            ModelAndView(map, "404.hbs")
        }, handlebars)
        path("/api", {
            path("/oauth", {
                get("/login", { request, response ->
                    if (request.queryParams("code") == null) {
                        response.redirect("/fail", 404)
                        null
                    } else {
                        val code = request.queryParams("code")
                        val token = retrieveToken(code)
                        if (token == null) {
                            response.redirect("/fail", 404)
                            null
                        } else {
                            val identification = identityObject(token.access_token)
                            if (identification == null) {
                                response.redirect("/fail", 404)
                                null
                            } else {
                                val session = request.session()
                                session.attribute("user", getUserById(identification.id))
                                response.redirect("/welcome")
                                null
                            }
                        }
                    }
                }, handlebars)
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
        })
    }
}

fun createUserModel(id: String): Any {
    val user: User = getUserById(id) ?: return Failure("invalid user requested").toJson()
    return UserModel(id, user.name, user.discriminator, user.effectiveAvatarUrl)
}

data class UserModel(val id: String, val username: String, val discrim: String, val avatar: String)

data class GuildMemberModel(val id: String, val effectiveName: String, val roles: List<String>, val hasOverride: Boolean)


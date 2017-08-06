package web

import commands.administrate.Staff
import commands.administrate.filterByRole
import commands.administrate.staff
import events.Category
import main.factory
import main.jdas
import main.test
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import spark.ModelAndView
import spark.Request
import spark.Service
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine
import utils.*

val settings = mutableListOf<Setting>()

val handlebars = HandlebarsTemplateEngine()

val loginRedirect = if (test) "http://localhost/api/oauth/login" else "https://ardentbot.com/api/oauth/login"

class Web {
    init {
        if (!test) {
            val httpServer = Service.ignite().port(80)
            httpServer.before { request, response ->
                val url = request.url()
                response.redirect("https://${url.split("http://")[1]}")
            }
            port(443)
            secure("/root/Ardent/keystore.p12", "ardent", null, null)
        } else port(80)

        settings.add(Setting("/defaultrole", "Default Role", "Remove or set the role given to all new members. Can fail if Ardent doesn't " +
                "have sufficient permissions to give them the role."))
        settings.add(Setting("/joinmessage", "Join Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/leavemessage", "Leave Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/music/announcenewmusic", "Announce Songs at Start", "Choose whether you want to allow "))

        staticFiles.location("/public")

        notFound({ request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "404 Not Found")
            handlebars.render(ModelAndView(map, "404.hbs"))
        })
        get("/", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Home")
            ModelAndView(map, "index.hbs")
        }, handlebars)
        get("/welcome", { request, response ->
            val map = hashMapOf<String, Any>()
            map.put("showSnackbar", false)
            handle(request, map)
            map.put("title", "Welcome!")
            ModelAndView(map, "welcome.hbs")
        }, handlebars)
        get("/status", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            val internals = Internals()
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
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Staff")
            map.put("administrators", staff.filterByRole(Staff.StaffRole.ADMINISTRATOR).map { it.id.toUser() })
            map.put("moderators", staff.filterByRole(Staff.StaffRole.MODERATOR).map { it.id.toUser() })
            map.put("helpers", staff.filterByRole(Staff.StaffRole.HELPER).map { it.id.toUser() })
            ModelAndView(map, "staff.hbs")
        }, handlebars)
        get("/commands", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Commands")
            val category: Int? = request.queryParams("c")?.toIntOrNull()
            if (category == null || category !in 1..5) map.put("genTable", false)
            else {
                map.put("genTable", true)
                val commandCategory = when (category) {
                    1 -> Category.MUSIC
                    2 -> Category.BOT_INFO
                    3 -> Category.SERVER_INFO
                    4 -> Category.ADMINISTRATE
                    5 -> Category.FUN
                    else -> Category.GAMES
                }
                map.put("commands", factory.commands.filter { it.category == commandCategory })
                map.put("category", commandCategory)
            }
            ModelAndView(map, "commands.hbs")
        }, handlebars)
        get("/manage", { request, response ->
            val session = request.session()
            val user: User? = session.attribute<User>("user")
            if (user == null) {
                response.redirect("/login")
                null
            } else {
                val map = hashMapOf<String, Any>()
                handle(request, map)
                map.put("showSnackbar", false)
                map.put("title", "Management Center")
                map.put("availableGuilds", getMutualGuildsWith(user).filter { it.getMember(user).hasOverride(it.publicChannel, failQuietly = true) })
                ModelAndView(map, "manage.hbs")
            }
        }, handlebars)
        get("/manage/*", { request, response ->
            val session = request.session()
            val user: User? = session.attribute<User>("user")
            val guild = getGuildById(request.splat()[0])
            if (user == null) {
                response.redirect("/login")
                null
            } else if (guild == null) {
                response.redirect("/manage")
                null
            } else {
                val map = hashMapOf<String, Any>()
                handle(request, map)
                map.put("showSnackbar", false)
                if (guild.getMember(user).hasOverride(guild.publicChannel, failQuietly = true)) {
                    map.put("title", "Management Center")
                    val data = guild.getData()
                    map.put("announceMusic", data.musicSettings.announceNewMusic)
                    map.put("guild", guild)
                    ModelAndView(map, "manageGuild.hbs")
                } else {
                    map.put("showSnackbar", true)
                    map.put("snackbarMessage", "You don't have permission to manage the settings for this server!")
                    ModelAndView(map, "fail.hbs")
                }
            }
        }, handlebars)

        get("/support", { _, response -> response.redirect("https://discord.gg/rfGSxNA") })
        get("/invite", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=bot&client_id=339101087569281045&permissions=269574192&redirect_uri=$loginRedirect&response_type=code") })
        get("/login", { request, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=identify&client_id=${jdas[0].selfUser.id}&response_type=code&redirect_uri=$loginRedirect") })
        get("/patreon", { request, response -> response.redirect("https://patreon.com/ardent") })
        get("/404", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "404 Not Found")
            ModelAndView(map, "404.hbs")
        }, handlebars)
        get("/fail", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "No Permission")
            ModelAndView(map, "fail.hbs")
        }, handlebars)

        path("/api", {
            path("/internal", {
                get("/data/*/*", { request, response ->
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
                    val state: String? = request.queryParams("state")
                    val guild = getGuildById(request.splat()[0])
                    if (user == null) {
                        response.redirect("/login")
                        null
                    } else if (guild == null) {
                        response.redirect("/manage")
                        null
                    } else {
                        val map = hashMapOf<String, Any>()
                        handle(request, map)
                        map.put("showSnackbar", false)
                        if (guild.getMember(user).hasOverride(guild.publicChannel, failQuietly = true)) {
                            val data = guild.getData()
                            when (request.splat()[1]) {
                                "announcemusic" -> {
                                    when (state) {
                                        "on" -> data.musicSettings.announceNewMusic = true
                                        else -> data.musicSettings.announceNewMusic = false
                                    }
                                    data.update()
                                    map.replace("showSnackbar", true)
                                    map.put("snackbarMessage", "Successfully updated the Announce Music setting")
                                }
                            }
                            map.put("announceMusic", data.musicSettings.announceNewMusic)
                            ModelAndView(map, "manageGuild.hbs")
                        } else {
                            map.put("showSnackbar", true)
                            map.put("snackbarMessage", "You don't have permission to manage the settings for this server!")
                            ModelAndView(map, "fail.hbs")
                        }
                    }
                }, handlebars)
            })
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

    private fun  handle(request: Request, map: HashMap<String, Any>) {
        val session = request.session()
        val user = session.attribute<User>("user")
        if (user == null) map.put("validSession", false)
        else {
            map.put("validSession", true)
            map.put("user", user)
        }
    }
}

fun createUserModel(id: String): Any {
    val user: User = getUserById(id) ?: return Failure("invalid user requested").toJson()
    return UserModel(id, user.name, user.discriminator, user.effectiveAvatarUrl)
}

data class UserModel(val id: String, val username: String, val discrim: String, val avatar: String)

data class GuildMemberModel(val id: String, val effectiveName: String, val roles: List<String>, val hasOverride: Boolean)


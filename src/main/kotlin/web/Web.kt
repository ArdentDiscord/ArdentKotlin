package web

import commands.administrate.Staff
import commands.administrate.filterByRole
import commands.administrate.staff
import commands.games.*
import events.Category
import main.*
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.apache.commons.lang3.exception.ExceptionUtils
import spark.*
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
                try {
                    response.redirect("https://${url.split("http://")[1]}")
                } catch (e: Throwable) {
                    response.redirect("https://ardentbot.com")
                }
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
            map.put("messagesReceived", internals.messagesReceived.format())
            map.put("commandsReceived", internals.commandsReceived.format())
            map.put("guilds", internals.guilds.format())
            map.put("users", internals.users.format())
            map.put("loadedMusicPlayers", internals.loadedMusicPlayers.format())
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
        get("/patrons", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Patrons")
            map.put("patrons", r.table("patrons").run<Any>(conn).queryAsArrayList(Patron::class.java).filter { it != null }.map { it!!.id.toUser() })
            ModelAndView(map, "patrons.hbs")
        }, handlebars)
        get("/commands", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Commands")
            val category: Int? = request.queryParams("c")?.toIntOrNull()
            if (category == null || category !in 1..6) map.put("genTable", false)
            else {
                map.put("genTable", true)
                val commandCategory = when (category) {
                    1 -> Category.MUSIC
                    2 -> Category.BOT_INFO
                    3 -> Category.SERVER_INFO
                    4 -> Category.ADMINISTRATE
                    5 -> Category.FUN
                    6 -> Category.GAMES
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
                map.put("availableGuilds", getMutualGuildsWith(user).filter {
                    it.getMember(user) != null &&
                            it.getMember(user).hasOverride(it.textChannels[0], failQuietly = true)
                })
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
                if (guild.getMember(user).hasOverride(guild.textChannels[0], failQuietly = true)) {
                    manage(request, map, guild)
                    ModelAndView(map, "manageGuild.hbs")
                } else {
                    map.put("showSnackbar", true)
                    map.put("snackbarMessage", "You don't have permission to manage the settings for this server!")
                    ModelAndView(map, "fail.hbs")
                }
            }
        }, handlebars)
        get("/logout", { request, response ->
            request.session().invalidate()
            response.redirect("/")
        })
        get("/administrators", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("announcements", getAnnouncements())
            map.put("title", "Administrator Zone")
            val openTickets = getOpenTickets()
            map.put("noOpenTickets", openTickets.isEmpty())
            map.put("openTickets", openTickets)
            if (isAdministrator(request, response)) {
                map.put("showSnackbar", false)
                ModelAndView(map, "administrators.hbs")
            } else null
        }, handlebars)
        path("/translation", {
            get("/languages", { request, response ->
                val map = hashMapOf<String, Any>()
                handle(request, map)
                map.put("title", "Language List")
                map.put("languages_table_one", languages_table_one)
                map.put("languages_table_two", languages_table_two)
                ModelAndView(map, "languages.hbs")
            }, handlebars)
        })
        get("/support", { _, response -> response.redirect("https://discord.gg/rfGSxNA") })
        get("/invite", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=bot&client_id=339101087569281045&permissions=269574192&redirect_uri=$loginRedirect&response_type=code") })
        get("/login", { request, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=identify&client_id=${jdas[0].selfUser.id}&response_type=code&redirect_uri=$loginRedirect") })
        get("/patreon", { request, response -> response.redirect("https://patreon.com/ardent") })
        path("/guides", {
            get("/identifier", { request, response ->
                val map = hashMapOf<String, Any>()
                handle(request, map)
                map.put("title", "How to find Discord IDs")
                ModelAndView(map, "findid.hbs")
            }, handlebars)
        })
        get("/404", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "404 Not Found")
            ModelAndView(map, "404.hbs")
        }, handlebars)
        get("/games/recent", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("title", "Recent Games")
            val games = mutableListOf<Pair<GameType, GameData>>()
            r.table("CoinflipData").run<Any>(conn).queryAsArrayList(GameDataCoinflip::class.java).forEach { games.add(Pair(GameType.COINFLIP, it!!)) }
            r.table("BlackjackData").run<Any>(conn).queryAsArrayList(GameDataBlackjack::class.java).forEach { games.add(Pair(GameType.BLACKJACK, it!!)) }
            r.table("BettingData").run<Any>(conn).queryAsArrayList(GameDataBetting::class.java).forEach { games.add(Pair(GameType.BETTING, it!!)) }
            games.sortByDescending { it.second.endTime }
            games.removeIf { it.second.creator.toUser() == null }
            map.put("recentGames", games.map {
                SanitizedGame(it.second.creator.toUser()!!.withDiscrim(), it.second.endTime.readableDate(), it.first.readable, "https://ardentbot.com/games/${it.first.readable.toLowerCase()}/${it.second.id}")
            }.limit(30))
            ModelAndView(map, "recentgames.hbs")

        }, handlebars)
        get("/fail", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "No Permission")
            ModelAndView(map, "fail.hbs")
        }, handlebars)
        get("/announcements", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Announcements")
            map.put("announcements", getAnnouncements())
            ModelAndView(map, "announcements.hbs")
        }, handlebars)
        get("/games/*/*", { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            when (request.splat()[0]) {
                "blackjack" -> {
                    val id = request.splat()[1].toIntOrNull() ?: 999999999
                    val game = asPojo(r.table("BlackjackData").get(id).run(conn), GameDataBlackjack::class.java)
                    if (game == null) {
                        map.put("showSnackbar", true)
                        map.put("snackbarMessage", "No game with that id was found!")
                        map.put("title", "Gamemode not found")
                        ModelAndView(map, "404.hbs")
                    } else {
                        val user = game.creator.toUser()!!
                        map.put("title", "Blackjack Game #$id")
                        map.put("game", game)
                        map.put("user", user)
                        map.put("date", game.startTime.readableDate())
                        map.put("data", user.getData())
                        ModelAndView(map, "blackjack.hbs")
                    }
                }
                "trivia" -> {
                    val id = request.splat()[1].toIntOrNull() ?: 999999999
                    val game = asPojo(r.table("TriviaData").get(id).run(conn), GameDataTrivia::class.java)
                    if (game == null) {
                        map.put("showSnackbar", true)
                        map.put("snackbarMessage", "No game with that id was found!")
                        map.put("title", "Gamemode not found")
                        ModelAndView(map, "404.hbs")
                    } else {
                        val user = game.creator.toUser()!!
                        map.put("title", "Trivia Game #$id")
                        map.put("game", game.sanitize())
                        map.put("user", user)
                        map.put("date", game.startTime.readableDate())
                        map.put("data", user.getData())
                        ModelAndView(map, "trivia.hbs")
                    }

                }
                "coinflip" -> {
                    val id = request.splat()[1].toIntOrNull() ?: 999999999
                    val game = asPojo(r.table("CoinflipData").get(id).run(conn), GameDataCoinflip::class.java)
                    if (game == null) {
                        map.put("showSnackbar", true)
                        map.put("snackbarMessage", "No game with that id was found!")
                        map.put("title", "Gamemode not found")
                        ModelAndView(map, "404.hbs")
                    } else {
                        val creator = game.creator.toUser()!!
                        map.put("title", "Coinflip Game #$id")
                        map.put("game", game)
                        map.put("user", creator)
                        map.put("winner", game.winner.toUser()!!.withDiscrim())
                        map.put("losers", game.losers.toUsers())
                        map.put("date", game.startTime.readableDate())
                        ModelAndView(map, "coinflip.hbs")
                    }
                }
                "betting" -> {
                    val id = request.splat()[1].toIntOrNull() ?: 999999999
                    val game = asPojo(r.table("BettingData").get(id).run(conn), GameDataBetting::class.java)
                    if (game == null) {
                        map.put("showSnackbar", true)
                        map.put("snackbarMessage", "No game with that id was found!")
                        map.put("title", "Gamemode not found")
                        ModelAndView(map, "404.hbs")
                    } else {
                        val creator = game.creator.toUser()!!
                        map.put("title", "Betting Game #$id")
                        map.put("game", game)
                        map.put("user", creator)
                        map.put("date", game.startTime.readableDate())
                        ModelAndView(map, "betting.hbs")
                    }
                }
                else -> {
                    map.put("showSnackbar", true)
                    map.put("snackbarMessage", "No Gamemode with that title was found!")
                    map.put("title", "Gamemode not found")
                    ModelAndView(map, "404.hbs")
                }
            }
        }, handlebars)
        get("/tickets", { request, response ->
            response.redirect("/tickets/")
        })
        get("/tickets/*", { request, response ->
            val session = request.session()
            val user: User? = session.attribute<User>("user")
            val role: Staff? = session.attribute<Staff>("role")
            if (user == null) {
                response.redirect("/login")
                null
            } else {
                val map = hashMapOf<String, Any>()
                map.put("title", "Tickets")
                handle(request, map)
                val splats = request.splat()
                if (splats.isEmpty()) {
                    val tickets = user.getTickets()
                    val openTickets = tickets.filter { it.open }
                    val closedTickets = tickets.filter { !it.open }
                    map.put("hasOpenTickets", openTickets.isNotEmpty())
                    map.put("hasClosedTickets", closedTickets.isNotEmpty())
                    map.put("openTickets", openTickets)
                    map.put("closedTickets", closedTickets)
                    ModelAndView(map, "ticketsHome.hbs")
                } else {
                    when (splats[0]) {
                        "new" -> ModelAndView(map, "newTicket.hbs")
                        "api" -> {
                            val action = request.queryParams("action")
                            if (action == null) {
                                response.redirect("/404")
                                null
                            } else {
                                when (action) {
                                    "create" -> {
                                        val title = request.queryParams("title")
                                        val description = request.queryParams("description")
                                        if (title == null || description == null) {
                                            response.redirect("/tickets")
                                        } else {
                                            val ticket = SupportTicketModel(user.id, title, true).add(description)
                                            ticket.insert("supportTickets")
                                            response.redirect("/tickets/${ticket.id}")
                                            "346818849032896513".toChannel()!!.sendMessage("**${user.withDiscrim()}** has created a support ticket at " +
                                                    "https://ardentbot.com/tickets/${ticket.id}").queue()
                                        }
                                        null
                                    }
                                    "addMessage" -> {
                                        val message = request.queryParams("message")
                                        val ticket = asPojo(r.table("supportTickets").get(request.queryParamOrDefault("id", "4")).run(conn),
                                                SupportTicketModel::class.java)
                                        if (message != null && ticket != null && ticket.user == user.id || map["isAdmin"] as Boolean) {
                                            if (ticket!!.user == user.id) {
                                                ticket.userResponses.add(SupportMessageModel(user.id, true, message, System.currentTimeMillis()))
                                                "346818849032896513".toChannel()!!.sendMessage("**${user.withDiscrim()}** has **replied** to their support ticket @ " +
                                                        "https://ardentbot.com/tickets/${ticket.id}").queue()
                                            } else {
                                                ticket.administratorResponses.add(SupportMessageModel(user.id, false, message, System.currentTimeMillis()))
                                                "346818849032896513".toChannel()!!.sendMessage("**${user.withDiscrim()}**, an **administrator**, has replied to the " +
                                                        "support ticket @ https://ardentbot.com/tickets/${ticket.id}").queue()
                                                ticket.user.toUser()!!.openPrivateChannel().queue({ privateChannel ->
                                                    privateChannel.sendMessage("**Ardent Support**: __${user.withDiscrim()}__ has replied to your ticket @ https://ardentbot.com/tickets/${ticket.id}").queue()
                                                })
                                            }
                                            r.table("supportTickets").get(ticket.id).update(r.json(ticket.toJson())).runNoReply(conn)
                                            response.redirect("/tickets/${ticket.id}")

                                        } else {
                                            response.redirect("/fail")
                                        }
                                        null
                                    }
                                    "close" -> {
                                        val ticket = asPojo(r.table("supportTickets").get(request.queryParamOrDefault("id", "4")).run(conn),
                                                SupportTicketModel::class.java)
                                        if (ticket != null && (ticket.user == user.id || map["isAdmin"] as Boolean)) {
                                            r.table("supportTickets").get(ticket.id).update(r.hashMap("open", false)).runNoReply(conn)
                                            response.redirect("/tickets/${ticket.id}?announce=Successfully+closed+your+ticket")
                                            "346818849032896513".toChannel()!!.sendMessage("**${user.withDiscrim()}** has **closed** a support ticket at " +
                                                    "https://ardentbot.com/tickets/${ticket.id}. No further action is necessary").queue()
                                        } else {
                                            response.redirect("/fail")
                                        }
                                        null
                                    }
                                    "reopen" -> {
                                        val ticket = asPojo(r.table("supportTickets").get(request.queryParamOrDefault("id", "4")).run(conn),
                                                SupportTicketModel::class.java)
                                        if (ticket != null && (ticket.user == user.id || map["isAdmin"] as Boolean)) {
                                            r.table("supportTickets").get(ticket.id).update(r.hashMap("open", true)).runNoReply(conn)
                                            "346818849032896513".toChannel()!!.sendMessage("**${user.withDiscrim()}** has **re-opened** a support ticket at " +
                                                    "https://ardentbot.com/tickets/${ticket.id}").queue()
                                            response.redirect("/tickets/${ticket.id}?announce=Successfully+reopened+your+ticket")
                                        } else {
                                            response.redirect("/fail")
                                        }
                                        null
                                    }
                                    else -> {
                                        response.redirect("/404")
                                        null
                                    }
                                }
                            }
                        }
                        else -> {
                            val ticketId = splats[0]
                            if (ticketId == null) {
                                response.redirect("/404")
                                null
                            } else {
                                val ticketModel = asPojo(r.table("supportTickets").get(ticketId).run(conn), SupportTicketModel::class.java)
                                if (ticketModel == null) {
                                    response.redirect("/404")
                                    null
                                } else {
                                    val ticket = ticketModel.toSupportTicket()
                                    if (ticket.user.id == user.id || map["isAdmin"] as Boolean) {
                                        map.put("noResponses", ticket.messages.isEmpty())
                                        map.put("oneMessage", ticket.messages.size == 1)
                                        map.put("ticket", ticket)
                                        map.put("messages", ticket.messages)
                                        if (request.queryParams("announce") != null) {
                                            map.replace("showSnackbar", true)
                                            map.put("snackbarMessage", request.queryParams("announce"))
                                        }
                                        ModelAndView(map, "viewTicket.hbs")
                                    } else {
                                        response.redirect("/fail")
                                        null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, handlebars)
        path("/api", {
            path("/internal", {
                get("/useraction/*", { request, response ->
                    val map = hashMapOf<String, Any>()
                    handle(request, map)
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
                    if (user == null) {
                        response.redirect("/login")
                        null
                    } else {
                        val actionUser = getUserById(request.queryParams("id"))
                        if (actionUser == null || request.queryParams("action") == null) {
                            map.put("showSnackbar", true)
                            map.put("snackbarMessage", "No user with that ID or action was found!")
                            ModelAndView(map, "404.hbs")
                        } else {
                            when (request.queryParams("action")) {
                                "addWhitelisted" -> {
                                    val role = session.attribute<Staff>("role")
                                    if (role == null) ModelAndView(map, "fail.hbs")
                                    else {
                                        if (role.role != Staff.StaffRole.ADMINISTRATOR) {
                                            map.put("showSnackbar", true)
                                            map.put("snackbarMessage", "You don't have permission to do this!")
                                            ModelAndView(map, "fail.hbs")
                                        } else {
                                            if (user.whitelisted().size >= 3) {
                                                map.put("showSnackbar", true)
                                                map.put("snackbarMessage", "You can't have more than 3 whitelists at a time!")
                                                ModelAndView(map, "fail.hbs")
                                            } else {
                                                SpecialPerson(actionUser.id, user.id).insert("specialPeople")
                                                val redirect = request.queryParams("redirect")
                                                if (redirect == null) response.redirect("/")
                                                else response.redirect("/$redirect")
                                                null
                                            }
                                        }
                                    }
                                }
                                "removeWhitelisted" -> {
                                    val role = session.attribute<Staff>("role")
                                    if (role == null) ModelAndView(map, "fail.hbs")
                                    else {
                                        if (role.role != Staff.StaffRole.ADMINISTRATOR) {
                                            map.put("showSnackbar", true)
                                            map.put("snackbarMessage", "You don't have permission to do this!")
                                            ModelAndView(map, "fail.hbs")
                                        } else {
                                            val whitelists = user.whitelisted().map { it!!.id }
                                            val isWhitelisted = whitelists.contains(actionUser.id)
                                            if (!isWhitelisted) {
                                                map.put("showSnackbar", true)
                                                map.put("snackbarMessage", "You haven't whitelisted this user!")
                                                ModelAndView(map, "fail.hbs")
                                            } else {
                                                r.table("specialPeople").get(actionUser.id).delete().runNoReply(conn)
                                                val redirect = request.queryParams("redirect")
                                                if (redirect == null) response.redirect("/")
                                                else response.redirect("/$redirect")
                                                null
                                            }
                                        }
                                    }
                                }
                                "advancedPermissions" -> {
                                    if (request.splat().isEmpty() || getGuildById(request.splat()[0]) == null) {
                                        map.put("showSnackbar", true)
                                        map.put("snackbarMessage", "No server was provided!")
                                        ModelAndView(map, "fail.hbs")
                                    } else {
                                        val guild = getGuildById(request.splat()[0])!!
                                        if (guild.getMember(user).hasOverride(guild.publicChannel, failQuietly = true)) {
                                            when (request.queryParams("type")) {
                                                "add" -> {
                                                    val data = guild.getData()
                                                    if (!data.advancedPermissions.contains(actionUser.id)) {
                                                        data.advancedPermissions.add(actionUser.id)
                                                        data.update()
                                                    }
                                                    response.redirect("/manage/${guild.id}")
                                                    null
                                                }
                                                "remove" -> {
                                                    val data = guild.getData()
                                                    if (data.advancedPermissions.contains(actionUser.id)) {
                                                        data.advancedPermissions.remove(actionUser.id)
                                                        data.update()
                                                    }
                                                    response.redirect("/manage/${guild.id}")
                                                    null
                                                }
                                                else -> {
                                                    map.put("showSnackbar", true)
                                                    map.put("snackbarMessage", "No registered action was found!")
                                                    ModelAndView(map, "404.hbs")
                                                }
                                            }
                                        } else {
                                            map.put("showSnackbar", true)
                                            map.put("snackbarMessage", "You don't have permission to perform this action!")
                                            ModelAndView(map, "fail.hbs")
                                        }
                                    }
                                }
                                else -> {
                                    map.put("showSnackbar", true)
                                    map.put("snackbarMessage", "No registered action was found!")
                                    ModelAndView(map, "404.hbs")
                                }
                            }
                        }
                    }
                }, handlebars)
                path("/administrators", {
                    get("/announcements/*", { request, response ->
                        val map = hashMapOf<String, Any>()
                        handle(request, map)
                        map.put("title", "Administrator Zone")
                        if (isAdministrator(request, response)) {
                            if (request.splat().size == 1) {
                                val content = request.queryParams("content")
                                if (content == null) response.redirect("/404")
                                else {
                                    when (request.splat()[0]) {
                                        "add" -> {
                                            val user = request.session().attribute<User>("user")
                                            val announcement = AnnouncementModel(System.currentTimeMillis(), user.id, content)
                                            announcement.insert("announcements")
                                            "272411413031419904".toChannel()!!.sendMessage("**New Announcement** from __${user.withDiscrim()}__\n" +
                                                    "${announcement.content}\n" +
                                                    "*View Announcements @ https://ardentbot.com/announcements*").queue()
                                            response.redirect("/administrators")
                                        }
                                        "remove" -> {
                                            val dateLong = content.toLongOrNull() ?: 0
                                            r.table("announcements").get(dateLong).delete().runNoReply(conn)
                                            response.redirect("/administrators")
                                        }
                                        else -> response.redirect("/fail")
                                    }
                                }
                            } else response.redirect("/fail")
                        }
                    })
                    get("/remove", { request, response ->
                        val map = hashMapOf<String, Any>()
                        handle(request, map)
                        map.put("title", "Administrator Zone")
                        val session = request.session()
                        val user: User? = session.attribute<User>("user")
                        val role: Staff? = session.attribute<Staff>("role")
                        if (user == null) {
                            response.redirect("/login")
                            null
                        } else if (role == null || role.role != Staff.StaffRole.ADMINISTRATOR) {
                            map.put("showSnackbar", true)
                            map.put("snackbarMessage", "You need to be an administrator to access this page!")
                            ModelAndView(map, "fail.hbs")

                        } else {
                            val id = request.queryParamOrDefault("id", "3")
                            val whitelisted = user.whitelisted().map { it!!.id }
                            if (whitelisted.contains(id)) r.table("specialPeople").get(id).delete().runNoReply(conn)
                            response.redirect("/administrators")
                            null
                        }
                    })
                })
                get("/data/*/*", { request, response ->
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
                    val guild = getGuildById(request.splat()[0] ?: "1")
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
                        if (guild.getMember(user).hasOverride(guild.textChannels[0], failQuietly = true)) {
                            map.put("title", "Management Center")
                            val data = guild.getData()
                            when (request.splat()[1] ?: "1") {
                                "addautorole" -> {
                                    map.replace("showSnackbar", true)
                                    val title = request.queryParams("name")
                                    val role = guild.getRoleById(request.queryParams("role") ?: "1")
                                    if (title == null || role == null) {
                                        map.put("snackbarMessage", "One of the given parameters was null")
                                    } else {
                                        if (data.iamList.firstOrNull({ it.roleId == role.id }) != null) {
                                            map.put("snackbarMessage", "This role already has an autorole set to it!")
                                        } else {
                                            data.iamList.add(Iam(title, role.id))
                                            map.put("snackbarMessage", "Successfully added autorole!")
                                        }
                                    }
                                }
                                "removeautorole" -> {
                                    map.replace("showSnackbar", true)
                                    val role = request.queryParams("role")
                                    if (role == null) map.put("snackbarMessage", "The role provided wasn't found")
                                    else {
                                        val iterator = data.iamList.iterator()
                                        while (iterator.hasNext()) {
                                            if (iterator.next().roleId == role) iterator.remove()
                                        }
                                        map.put("snackbarMessage", "Removed autorole if it was found in our database")
                                    }
                                }
                                "autoplaymusic" -> {
                                    val state: String? = request.queryParams("state")
                                    val snackbarKey: String
                                    when (state) {
                                        "on" -> {
                                            data.musicSettings.autoQueueSongs = true
                                            snackbarKey = "enabled"
                                        }
                                        else -> {
                                            data.musicSettings.autoQueueSongs = false
                                            snackbarKey = "disabled"
                                        }
                                    }
                                    map.replace("showSnackbar", true)
                                    map.put("snackbarMessage", "Successfully $snackbarKey Ardent's autoplay feature")

                                }
                                "announcemusic" -> {
                                    val state: String? = request.queryParams("state")
                                    when (state) {
                                        "on" -> data.musicSettings.announceNewMusic = true
                                        else -> data.musicSettings.announceNewMusic = false
                                    }
                                    map.replace("showSnackbar", true)
                                    map.put("snackbarMessage", "Successfully updated the Announce Music setting")
                                }
                                "trusteveryone" -> {
                                    val state: String? = request.queryParams("state")
                                    when (state) {
                                        "on" -> data.allowGlobalOverride = true
                                        else -> data.allowGlobalOverride = false
                                    }
                                    map.replace("showSnackbar", true)
                                    map.put("snackbarMessage", "Successfully updated the \"Music Trust Level\" setting")
                                }
                                "receiverchannel" -> {
                                    val ch: String? = request.queryParams("channelid")
                                    if (ch.equals("none", true)) {
                                        map.put("snackbarMessage", "Removed the Receiver Channel")
                                        if (data.joinMessage == null) data.joinMessage = Pair(null, null)
                                        else data.joinMessage = Pair(data.joinMessage!!.first, null)
                                        if (data.leaveMessage == null) data.leaveMessage = Pair(null, null)
                                        else data.leaveMessage = Pair(data.leaveMessage!!.first, null)
                                    } else {
                                        val channel: TextChannel? = ch?.toChannel()
                                        map.replace("showSnackbar", true)
                                        if (channel == null) {
                                            map.put("snackbarMessage", "Unable to find a Text Channel with that ID... Please retry")
                                        } else {
                                            if (data.joinMessage == null) data.joinMessage = Pair(null, channel.id)
                                            else data.joinMessage = Pair(data.joinMessage!!.first, channel.id)
                                            if (data.leaveMessage == null) data.leaveMessage = Pair(null, channel.id)
                                            else data.leaveMessage = Pair(data.leaveMessage!!.first, channel.id)
                                            map.put("snackbarMessage", "Set the Receiver Channel")
                                        }
                                    }
                                }
                                "removemessages" -> {
                                    val resetChannel = request.queryParams("resetChannel") ?: ""
                                    val resetJoinMessage = request.queryParams("resetJoinMessage") ?: ""
                                    val resetLeaveMessage = request.queryParams("resetLeaveMessage") ?: ""
                                    if (resetChannel == "on") {
                                        if (data.joinMessage != null) data.joinMessage = Pair(data.joinMessage!!.first, null)
                                        if (data.leaveMessage != null) data.leaveMessage = Pair(data.leaveMessage!!.first, null)
                                    }
                                    if (resetJoinMessage == "on") {
                                        if (data.joinMessage != null) data.joinMessage = Pair(null, data.joinMessage!!.second)
                                    }
                                    if (resetLeaveMessage == "on") {
                                        if (data.leaveMessage != null) data.leaveMessage = Pair(null, data.leaveMessage!!.second)
                                    }
                                    map.replace("showSnackbar", true)
                                    map.put("snackbarMessage", "Your request for removal completed successfully")
                                }
                                "joinmessage" -> {
                                    var joinMessage = request.queryParams("joinMessage")
                                    if (joinMessage != null) {
                                        val built = mutableListOf<String>()
                                        val arguments = joinMessage.split(" ")
                                        arguments.forEach { it ->
                                            if (it.startsWith("#")) {
                                                val lookup = it.removePrefix("#")
                                                val results = guild.getTextChannelsByName(lookup, true)
                                                if (results.size > 0) built.add(results[0].asMention)
                                                else built.add(it)
                                            } else built.add(it)
                                        }
                                        joinMessage = built.concat()
                                        if (data.joinMessage == null) data.joinMessage = Pair(joinMessage, null)
                                        else data.joinMessage = Pair(joinMessage, data.joinMessage!!.second)
                                        map.replace("showSnackbar", true)
                                        map.put("snackbarMessage", "Successfully set the Join Message")
                                    }
                                }
                                "leavemessage" -> {
                                    var leaveMessage = request.queryParams("leaveMessage")
                                    if (leaveMessage != null) {
                                        val built = mutableListOf<String>()
                                        val arguments = leaveMessage.split(" ")
                                        arguments.forEach { it ->
                                            if (it.startsWith("#")) {
                                                val lookup = it.removePrefix("#")
                                                val results = guild.getTextChannelsByName(lookup, true)
                                                if (results.size > 0) built.add(results[0].asMention)
                                                else built.add(it)
                                            } else built.add(it)
                                        }
                                        leaveMessage = built.concat()
                                        if (data.leaveMessage == null) data.leaveMessage = Pair(leaveMessage, null)
                                        else data.leaveMessage = Pair(leaveMessage, data.leaveMessage!!.second)
                                        map.replace("showSnackbar", true)
                                        map.put("snackbarMessage", "Successfully set the Leave Message")
                                    }
                                }
                                "defaultrole" -> {
                                    map.replace("showSnackbar", true)
                                    val roleId = request.queryParams("defaultRole")
                                    if (!roleId.equals("none", true)) {
                                        data.defaultRole = roleId
                                        map.put("snackbarMessage", "Successfully set the Default Role")
                                    } else {
                                        data.defaultRole = ""
                                        map.put("snackbarMessage", "Successfully removed the Default Role")
                                    }
                                }
                            }
                            data.update()
                            manage(request, map, guild)
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
                        response.redirect("/welcome")
                        null
                    } else {
                        val code = request.queryParams("code")
                        val token = retrieveToken(code)
                        if (token == null) {
                            response.redirect("/welcome")
                            null
                        } else {
                            val identification = identityObject(token.access_token)
                            if (identification != null) {
                                val session = request.session()
                                val role = asPojo(r.table("staff").get(identification.id).run(conn), Staff::class.java)
                                if (role != null) session.attribute("role", role)
                                session.attribute("user", getUserById(identification.id))
                            }
                            response.redirect("/welcome")
                            null
                        }
                    }
                }, handlebars)
                post("/login", { request, response ->
                    response.redirect("/welcome")
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
        })
        internalServerError { request, response ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Internal Error")
            handlebars.render(ModelAndView(map, "error.hbs"))
        }
    }
}

fun isAdministrator(request: Request, response: Response): Boolean {
    val session = request.session()
    val user: User? = session.attribute<User>("user")
    val role: Staff? = session.attribute<Staff>("role")
    if (user == null) {
        response.redirect("/login")
        return false
    } else if (role == null || role.role != Staff.StaffRole.ADMINISTRATOR) {
        response.redirect("/fail")
        return false
    }
    return true
}

fun manage(request: Request, map: HashMap<String, Any>, guild: Guild) {
    map.put("title", "Management Center")
    val data = guild.getData()
    map.put("announceMusic", data.musicSettings.announceNewMusic)
    map.put("trustEveryone", data.allowGlobalOverride)
    map.put("guild", guild)
    val receiverChannel = data.joinMessage?.second?.toChannel()
    var channels = guild.textChannels
    if (receiverChannel != null) {
        channels = channels.toMutableList()
        channels.removeIf { it.id == receiverChannel.id }
        map.put("hasReceiverChannel", true)
        map.put("receiverChannel", receiverChannel)
    } else map.put("hasReceiverChannel", false)
    val defaultRole = data.defaultRole?.toRole(guild)
    if (defaultRole == null) map.put("hasDefaultRole", false)
    else {
        map.put("hasDefaultRole", true)
        map.put("defaultRole", defaultRole)
    }
    map.put("hasIams", data.iamList.size > 0)
    val iams = mutableListOf<Pair<String, Role>>()
    data.iamList.forEach {
        val role = guild.getRoleById(it.roleId)
        if (role != null) iams.add(Pair(it.name, role))
    }
    map.put("iams", iams)
    map.put("roles", guild.roles.toMutableList().without(guild.publicRole))
    map.put("advancedPermissions", data.advancedPermissions.map { it.toUser()!! })
    if (data.joinMessage?.first == null) map.put("joinMessage", "")
    else map.put("joinMessage", data.joinMessage!!.first!!)
    if (data.leaveMessage?.first == null) map.put("leaveMessage", "")
    else map.put("leaveMessage", data.leaveMessage!!.first!!)
    map.put("channels", channels)
    map.put("autoplayMusic", data.musicSettings.autoQueueSongs)
    map.put("data", data)
}

fun handle(request: Request, map: HashMap<String, Any>) {
    val session = request.session()
    val user = session.attribute<User>("user")
    if (user == null) {
        map.put("validSession", false)
        map.put("user", "")
    } else {
        map.put("validSession", true)
        map.put("user", user)
    }
    val role = session.attribute<Staff>("role")
    if (role == null) {
        map.put("isStaff", false)
        map.put("isAdmin", false)
        map.put("hasWhitelists", false)
    } else {
        val whitelisted = user.whitelisted()
        map.put("hasWhitelists", whitelisted.isNotEmpty())
        map.put("whitelisted", whitelisted.filter { it != null }.map { it!!.id.toUser()!! })
        map.put("isAdmin", role.role == Staff.StaffRole.ADMINISTRATOR)
        map.put("isStaff", true)
        map.put("role", role.role)
    }
}


fun createUserModel(id: String): Any {
    val user: User = getUserById(id) ?: return Failure("invalid user requested").toJson()
    return UserModel(id, user.name, user.discriminator, user.effectiveAvatarUrl)
}

data class LangModel(val name: String, val code: String)

data class UserModel(val id: String, val username: String, val discrim: String, val avatar: String)

data class GuildMemberModel(val id: String, val effectiveName: String, val roles: List<String>, val hasOverride: Boolean)


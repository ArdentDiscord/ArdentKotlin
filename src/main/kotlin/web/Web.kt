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
import org.apache.commons.lang3.text.WordUtils
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Service
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine
import translation.ArdentPhraseTranslation
import translation.Languages
import translation.toLanguage
import translation.translationData
import utils.*
import java.net.URLEncoder

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
            secure("/root/Ardent/ssl/keystore.p12", "mortimer5", null, null)
        } else port(80)

        settings.add(Setting("/defaultrole", "Default Role", "Remove or set the role given to all new members. Can fail if Ardent doesn't " +
                "have sufficient permissions to give them the role."))
        settings.add(Setting("/joinmessage", "Join Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/leavemessage", "Leave Message", "Set or remove the message displayed when a member joins your server"))
        settings.add(Setting("/music/announcenewmusic", "Announce Songs at Start", "Choose whether you want to allow "))
        staticFiles.location("/public")

        notFound({ request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "404 Not Found")
            handlebars.render(ModelAndView(map, "404.hbs"))
        })
        get("/robots.txt", { request, response ->
            "User-Agent: *\n" +
                    "Disallow: \n" +
                    "Disallow: /manage\n" +
                    "Disallow: /administrators"
        })
        get("/", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Home")
            ModelAndView(map, "index.hbs")
        }, handlebars)
        get("/welcome", { request, _ ->
            val map = hashMapOf<String, Any>()
            map.put("showSnackbar", false)
            handle(request, map)
            map.put("title", "Welcome to Ardent!")
            ModelAndView(map, "welcome.hbs")
        }, handlebars)
        get("/status", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
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
        get("/staff", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Staff")
            map.put("administrators", staff.filterByRole(Staff.StaffRole.ADMINISTRATOR).map { it.id.toUser() })
            map.put("moderators", staff.filterByRole(Staff.StaffRole.MODERATOR).map { it.id.toUser() })
            map.put("helpers", staff.filterByRole(Staff.StaffRole.HELPER).map { it.id.toUser() })
            ModelAndView(map, "staff.hbs")
        }, handlebars)
        get("/patrons", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "Patrons")
            map.put("patrons", r.table("patrons").run<Any>(conn).queryAsArrayList(Patron::class.java).filter { it != null }.map { it!!.id.toUser() })
            ModelAndView(map, "patrons.hbs")
        }, handlebars)
        get("/commands", { request, _ ->
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
                    manage(map, guild)
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
            map.put("commands", factory.commands.sortedBy { it.name })
            map.put("phrases", translationData.phrases.map { it.value })
            map.put("staffMembers", staff.filterByRole(Staff.StaffRole.MODERATOR).map { it.id.toUser() })
            if (isAdministrator(request, response)) {
                map.put("showSnackbar", false)
                ModelAndView(map, "administrators.hbs")
            } else null
        }, handlebars)
        path("/translation", {
            get("/languages", { request, _ ->
                val map = hashMapOf<String, Any>()
                handle(request, map)
                map.put("title", "Language List")
                map.put("languages_table_one", languages_table_one)
                map.put("languages_table_two", languages_table_two)
                ModelAndView(map, "languages.hbs")
            }, handlebars)
            path("/ar", {
                get("/*/*/*", { request, response ->
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
                    when {
                        user == null -> {
                            response.redirect("/login")
                            null
                        }
                        session.attribute<Staff>("role") == null -> {
                            response.redirect("/fail")
                            null
                        }
                        else -> {
                            val map = hashMapOf<String, Any>()
                            handle(request, map)
                            val language = request.splat().getOrNull(0)?.toLanguage()
                            if (language == null) {
                                response.redirect("/fail")
                                println(request.splat().getOrNull(0))
                                null
                            } else {
                                if (request.splat().getOrNull(1) == "hi" && request.splat().getOrNull(2) == "guys") {
                                    val nullTranslations = language.getNullTranslations()
                                    val completedTranslations = language.getNonNullTranslations()
                                    map.put("nullTranslations", nullTranslations)
                                    map.put("completedTranslations", completedTranslations)
                                    map.put("undoneTotal", nullTranslations.size)
                                    map.put("completedTotal", completedTranslations.size)
                                    map.put("language", language)
                                    map.put("percent", "%.2f".format(internals.languageStatuses[language]?.toFloat()) ?: "NaN")
                                    map.put("title", "${language.readable} Translations")
                                    ModelAndView(map, "languageHome.hbs")
                                } else if (request.splat().getOrNull(1) == "proofread" && request.splat().getOrNull(2) == "all") {
                                    val translations = language.getTranslations()
                                    val proofed = mutableListOf<ProofreadPhrase>()
                                    translations.forEach { translation ->
                                        proofed.add(ProofreadPhrase(translation, translation.translate(language) ?: "Doesn't exist", false))
                                    }
                                    map.put("proofed", proofed)
                                    map.put("language", language)
                                    map.put("title", "Proofreading | ${language.readable}")
                                    ModelAndView(map, "proofread.hbs")
                                } else {
                                    map.put("title", "${language.readable} Translation")
                                    val phrase = translationData.getByEncoded(URLEncoder.encode(request.splat().getOrNull(2)))
                                    if (phrase == null) {
                                        ModelAndView(map, "404.hbs")
                                    } else {
                                        when (request.splat().getOrNull(1)) {
                                            "view" -> {
                                                val langTranslation = phrase.translations[language.code] ?: ""
                                                map.put("english", phrase.translations["en"] ?: phrase.english)
                                                map.put("langTranslation", langTranslation)
                                                map.put("language", language)
                                                map.put("encoded", phrase.encoded ?: "")
                                                map.put("original", phrase.english)
                                                ModelAndView(map, "translationView.hbs")
                                            }
                                            "update" -> {
                                                val new = request.queryParams("n")
                                                if (new == null) {
                                                    response.redirect("/translation/ar/${language.code}/hi/guys")
                                                    null
                                                } else {
                                                    phrase.translations.putIfAbsent(language.code, new)
                                                    if (language == Languages.ENGLISH.language) {
                                                        phrase.translations.replace("en", new)
                                                        val temp = phrase.english
                                                        phrase.english = new
                                                        r.table("phrases").filter(r.hashMap("english", temp)).update(r.json(phrase.toJson())).runNoReply(conn)
                                                        phrase.encoded = URLEncoder.encode(new)
                                                    } else {
                                                        phrase.translations.replace(language.code, new)
                                                        r.table("phrases").filter(r.hashMap("english", phrase.english)).update(r.json(phrase.toJson())).runNoReply(conn)
                                                    }
                                                    "355817985052508160".toChannel()?.send("${user.asMention} just updated a **${language.readable}** translation!")
                                                    response.redirect("/translation/ar/${language.code}/hi/guys")
                                                    null
                                                }
                                            }
                                            else -> {
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
            })
            get("/", { request, response ->
                val session = request.session()
                val user: User? = session.attribute<User>("user")
                when {
                    user == null -> {
                        response.redirect("/login")
                        null
                    }
                    session.attribute<Staff>("role") == null -> {
                        response.redirect("/fail")
                        null
                    }
                    else -> {
                        val map = hashMapOf<String, Any>()
                        handle(request, map)
                        map.put("showSnackbar", false)
                        map.put("title", "Translation Home")
                        map.put("languages", Languages.values())
                        ModelAndView(map, "translationHome.hbs")
                    }
                }
            }, handlebars)
        })
        get("/invite/extensions", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?client_id=354400275412418560&scope=bot&permissions=67193857") })
        get("/support", { _, response -> response.redirect("https://discord.gg/VebBB5z") })
        get("/invite", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=bot&client_id=339101087569281045&permissions=269574192&redirect_uri=$loginRedirect&response_type=code") })
        get("/login", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=identify&client_id=${jdas[0].selfUser.id}&response_type=code&redirect_uri=$loginRedirect") })
        get("/patreon", { _, response -> response.redirect("https://patreon.com/ardent") })
        path("/guides", {
            get("/identifier", { request, _ ->
                val map = hashMapOf<String, Any>()
                handle(request, map)
                map.put("title", "How to find Discord IDs")
                ModelAndView(map, "findid.hbs")
            }, handlebars)
        })
        get("/404", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "404 Not Found")
            ModelAndView(map, "404.hbs")
        }, handlebars)
        get("/games/recent", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("title", "Recent Games")
            val games = mutableListOf<Pair<GameType, GameData>>()
            r.table("BlackjackData").run<Any>(conn).queryAsArrayList(GameDataBlackjack::class.java).forEach { games.add(Pair(GameType.BLACKJACK, it!!)) }
            r.table("BettingData").run<Any>(conn).queryAsArrayList(GameDataBetting::class.java).forEach { games.add(Pair(GameType.BETTING, it!!)) }
            r.table("TriviaData").run<Any>(conn).queryAsArrayList(GameDataTrivia::class.java).forEach { games.add(Pair(GameType.TRIVIA, it!!)) }
            r.table("Connect_4Data").run<Any>(conn).queryAsArrayList(GameDataConnect4::class.java).forEach { games.add(Pair(GameType.CONNECT_4, it!!)) }
            r.table("SlotsData").run<Any>(conn).queryAsArrayList(GameDataSlots::class.java).forEach { games.add(Pair(GameType.SLOTS, it!!)) }
            r.table("Tic_Tac_ToeData").run<Any>(conn).queryAsArrayList(GameDataTicTacToe::class.java).forEach { games.add(Pair(GameType.TIC_TAC_TOE, it!!)) }
            games.sortByDescending { it.second.endTime }
            map.put("total", games.size)
            games.removeIf { it.second.creator.toUser() == null }
            map.put("recentGames", games.map {
                SanitizedGame(it.second.creator.toUser()!!.withDiscrim(), it.second.endTime.readableDate(), it.first.readable, "https://ardentbot.com/games/${it.first.readable.toLowerCase()}/${it.second.id}")
            }.limit(30))
            ModelAndView(map, "recentgames.hbs")

        }, handlebars)
        get("/fail", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            map.put("showSnackbar", false)
            map.put("title", "No Permission")
            ModelAndView(map, "fail.hbs")
        }, handlebars)
        get("/games/*/*", { request, _ ->
            val map = hashMapOf<String, Any>()
            handle(request, map)
            when (request.splat()[0]) {
                "guess_the_number" -> {
                    map.put("title", "Guess The Number")
                    ModelAndView(map, "guessthenumber.hbs")
                }
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
                "slots" -> {
                    val id = request.splat()[1].toIntOrNull() ?: 999999999
                    val game = asPojo(r.table("SlotsData").get(id).run(conn), GameDataSlots::class.java)
                    if (game == null) {
                        map.put("showSnackbar", true)
                        map.put("snackbarMessage", "No game with that id was found!")
                        map.put("title", "Gamemode not found")
                        ModelAndView(map, "404.hbs")
                    } else {
                        val user = game.creator.toUser()!!
                        map.put("title", "Slots Game #$id")
                        map.put("game", game)
                        map.put("user", user)
                        map.put("date", game.startTime.readableDate())
                        map.put("data", user.getData())
                        ModelAndView(map, "slots.hbs")
                    }
                }
                "connect_4" -> {
                    val id = request.splat()[1].toIntOrNull() ?: 999999999
                    val game = asPojo(r.table("Connect_4Data").get(id).run(conn), GameDataConnect4::class.java)
                    if (game == null) {
                        map.put("showSnackbar", true)
                        map.put("snackbarMessage", "No game with that id was found!")
                        map.put("title", "Gamemode not found")
                        ModelAndView(map, "404.hbs")
                    } else {
                        val user = game.creator.toUser()!!
                        map.put("title", "Connect 4 Game #$id")
                        map.put("game", game)
                        map.put("board", game.game.replace("\n", "<br />").replace("⚪", "◯"))
                        map.put("winner", game.winner.toUser()!!)
                        map.put("loser", game.loser.toUser()!!)
                        map.put("user", user)
                        map.put("date", game.startTime.readableDate())
                        map.put("data", user.getData())
                        ModelAndView(map, "connect_4.hbs")
                    }
                }
                "tic_tac_toe" -> {
                    val id = request.splat()[1].toIntOrNull() ?: 999999999
                    val game = asPojo(r.table("Tic_Tac_ToeData").get(id).run(conn), GameDataTicTacToe::class.java)
                    if (game == null) {
                        map.put("showSnackbar", true)
                        map.put("snackbarMessage", "No game with that id was found!")
                        map.put("title", "Gamemode not found")
                        ModelAndView(map, "404.hbs")
                    } else {
                        map.put("title", "Tic Tac Toe Game #$id")
                        map.put("game", game)
                        map.put("user", game.creator.toUser()!!)
                        map.put("board", game.game.replace("\n", "<br />"))
                        if (game.winner == null) {
                            map.put("hasWinner", false)
                            map.put("player1", game.playerOne.toUser()!!)
                            map.put("player2", game.playerTwo.toUser()!!)
                        } else {
                            map.put("hasWinner", true)
                            map.put("winner", game.winner.toUser()!!)
                            map.put("loser", (if (game.winner != game.playerOne) game.playerOne else game.playerTwo).toUser()!!)
                        }
                        map.put("date", game.startTime.readableDate())
                        ModelAndView(map, "tic_tac_toe.hbs")
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
        get("/tickets",
                { _, response ->
                    response.redirect("/tickets/")
                })
        get("/tickets/*",
                { request, response ->
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
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
                                                    "351377320927428609".toChannel()!!.sendMessage("**${user.withDiscrim()}** has created a support ticket at " +
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
                                                        "351377320927428609".toChannel()!!.sendMessage("**${user.withDiscrim()}** has **replied** to their support ticket @ " +
                                                                "https://ardentbot.com/tickets/${ticket.id}").queue()
                                                    } else {
                                                        ticket.administratorResponses.add(SupportMessageModel(user.id, false, message, System.currentTimeMillis()))
                                                        "351377320927428609".toChannel()!!.sendMessage("**${user.withDiscrim()}**, an **administrator**, has replied to the " +
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
                                                    "351377320927428609".toChannel()!!.sendMessage("**${user.withDiscrim()}** has **closed** a support ticket at " +
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
                                                    "351377320927428609".toChannel()!!.sendMessage("**${user.withDiscrim()}** has **re-opened** a support ticket at " +
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
        path("/api",
                {
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
                                        "addStaffMember" -> {
                                            val role = session.attribute<Staff>("role")
                                            if (role == null) ModelAndView(map, "fail.hbs")
                                            else {
                                                if (role.role != Staff.StaffRole.ADMINISTRATOR) {
                                                    map.put("showSnackbar", true)
                                                    map.put("snackbarMessage", "You don't have permission to do this!")
                                                    ModelAndView(map, "fail.hbs")
                                                } else {
                                                    if (actionUser.isStaff()) {
                                                        map.put("showSnackbar", true)
                                                        map.put("snackbarMessage", "This person is already a staff member!")
                                                        ModelAndView(map, "fail.hbs")
                                                    } else {
                                                        val newStaff = Staff(actionUser.id, Staff.StaffRole.MODERATOR)
                                                        staff.add(newStaff)
                                                        newStaff.insert("staff")
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
                                                if (guild.getMember(user).hasOverride(guild.defaultChannel ?: guild.textChannels[0], failQuietly = true)) {
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
                            get("/translations/*", { request, response ->
                                val map = hashMapOf<String, Any>()
                                handle(request, map)
                                map.put("title", "Administrator Zone")
                                if (isAdministrator(request, response)) {
                                    when (request.splat().getOrNull(0)) {
                                        "add" -> {
                                            val command = request.queryParams("command")
                                            val content = request.queryParams("content")
                                            if (command == null || content == null || content.isEmpty()) response.redirect("/administrators")
                                            else {
                                                val phrase = ArdentPhraseTranslation(content, WordUtils.capitalize(command)).instantiate(content)
                                                phrase.insert("phrases")
                                                translationData.phrases.put(content, phrase)
                                                response.redirect("/administrators")
                                                "355817985052508160".toChannel()?.send("${"355817985052508160".toChannel()?.guild?.getRolesByName("Translator", true)?.get(0)?.asMention}, a new phrase was added by ${request.session().attribute<User>("user").asMention} at https://ardentbot.com/translation/")
                                            }
                                        }
                                        "remove" -> {
                                            val content = request.queryParams("content")?.split("||")
                                            if (content?.size == 2) {
                                                val phrase = translationData.getByEncoded(content[0])
                                                if (phrase != null) {
                                                    translationData.phrases.forEach { t, u -> if (phrase == u) translationData.phrases.remove(t) }
                                                    "355817985052508160".toChannel()?.send("${request.session().attribute<User>("user").asMention} just removed a phrase :(")
                                                    r.table("phrases").filter(r.hashMap("english", phrase.english)).delete().runNoReply(conn)
                                                }
                                            }
                                            response.redirect("/administrators")
                                        }
                                        else -> response.redirect("/administrators")
                                    }
                                    null
                                } else {
                                    response.redirect("/administrators")
                                    null
                                }
                            }, handlebars)
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
                                                    "351369307147468800".toChannel()!!.sendMessage("**New Announcement** from __${user.withDiscrim()}__\n" +
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
                            get("/removeStaff", { request, response ->
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
                                    r.table("staff").get(id).delete().runNoReply(conn)
                                    staff.removeIf { it.id == id }
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
                                    manage(map, guild)
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
                        post("/login", { _, response -> response.redirect("/welcome") })
                    })
                    path("/public", {
                        get("/status", { _, _ -> internals.toJson() })
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
                                        else GuildMemberModel(id, member.effectiveName, member.roles.map { it.name }, member.hasOverride(guild.publicChannel, failQuietly = true)).toJson()

                                    }
                                })
                            })
                        })
                    })
                })
        internalServerError { request: Request, _: Response ->
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

fun manage(map: HashMap<String, Any>, guild: Guild) {
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
        val role = session.attribute<Staff>("role")
        if (role == null) {
            map.put("isStaff", false)
            map.put("isAdmin", false)
            map.put("hasWhitelists", false)
        } else {
            val whitelisted = user.whitelisted()
            map.put("hasWhitelists", whitelisted.isNotEmpty())
            map.put("whitelisted", whitelisted.filter { it != null }.map { it?.id?.toUser() })
            map.put("isAdmin", role.role == Staff.StaffRole.ADMINISTRATOR)
            map.put("isStaff", true)
            map.put("role", role.role)
        }
    }
}


fun createUserModel(id: String): Any {
    val user: User = getUserById(id) ?: return Failure("invalid user requested").toJson()
    return UserModel(id, user.name, user.discriminator, user.effectiveAvatarUrl)
}

data class LangModel(val name: String, val code: String)

data class UserModel(val id: String, val username: String, val discrim: String, val avatar: String)

data class GuildMemberModel(val id: String, val effectiveName: String, val roles: List<String>, val hasOverride: Boolean)


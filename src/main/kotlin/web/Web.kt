import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Options
import commands.music.getAudioManager
import commands.music.toTrackDisplay
import events.Category
import main.factory
import main.jdas
import main.spotifyApi
import main.test
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import obj.SimpleArtist
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine
import translation.Language
import translation.toLanguage
import utils.discord.*
import utils.functionality.*
import utils.music.*
import web.CommandWrapper
import web.identityObject
import web.retrieveToken

val handlebars = HandlebarsTemplateEngine()

val loginRedirect = if (test) "http://localhost/api/oauth/login" else "https://ardentbot.com/api/oauth/login"

val redirects = hashMapOf<String, String>()

class Web {
    private fun webRedirect(request: Request, response: Response, redirect_to: String, redirect_back: String? = null) {
        if (redirect_back != null) redirects.put(request.session().id(), redirect_back)
        response.redirect(redirect_to)
    }

    private fun redirectIfExists(request: Request, response: Response, redirect_failure_path: String) {
        if (redirects.containsKey(request.session().id())) {
            response.redirect(redirects[request.session().id()])
            redirects.remove(request.session().id())
        } else response.redirect(redirect_failure_path)
    }

    init {
        startup()
        registerHandlebarHelpers()

        get("/api/oauth/login", { request, response ->
            if (request.queryParams("code") == null) {
                webRedirect(request, response, "/login")
                null
            } else {
                val code = request.queryParams("code")
                val token = retrieveToken(code)
                if (token == null) {
                    webRedirect(request, response, "/login")
                    null
                } else {
                    val identification = identityObject(token.access_token)
                    if (identification != null) {
                        val session = request.session()
                        val user = getUserById(identification.id)
                        if (user == null) webRedirect(request, response, "/login")
                        else {
                            session.attribute("user", user)
                            redirectIfExists(request, response, "/profile")
                        }
                    }
                    null
                }
            }
        })
        get("/login", { _, response -> response.redirect("https://discordapp.com/oauth2/authorize?scope=identify&client_id=${jdas[0].selfUser.id}&response_type=code&redirect_uri=$loginRedirect") })
        get("/", { request, response ->
            val map = request.getDefaultMap(response, "Home")
            ModelAndView(map, "index.hbs")
        }, handlebars)
        get("/status", { request, response ->
            val map = request.getDefaultMap(response, "Status")
            map.put("internals", internals)
            map.put("arePeoplePlayingMusic", internals.loadedMusicPlayers > 0)
            ModelAndView(map, "status.hbs")
        }, handlebars)

        // Commands
        path("/commands", {
            get("", { request, response -> webRedirect(request, response, "/commands/") })
            get("/*", { request, response ->
                val map = request.getDefaultMap(response, "Commands")
                when {
                    request.splat().isEmpty() -> {
                        val commandWrappers = mutableListOf<CommandWrapper>()
                        Category.values().forEach { category ->
                            commandWrappers.add(CommandWrapper(category.webName, category.description, factory.commands.filter { it.category == category }))
                        }
                        commandWrappers.sortByDescending { it.category }
                        map.put("wrappers", commandWrappers)
                        handlebars.render(ModelAndView(map, "commands.hbs"))
                    }
                    request.splat()[0] == "translate" -> handlebars.render(ModelAndView(map, "languages.hbs"))
                    else -> null
                }
            })
        })


        // Profiles
        path("/profile", {
            get("", { _, response -> response.redirect("/profile/") })
            get("/*", { request, response ->
                val map = request.getDefaultMap(response, "Profiles")
                if (request.splat().isEmpty() && map["user"] == null) webRedirect(request, response, "/login", "/profile")
                else {
                    val user = (if (request.splat().size == 1) {
                        val requestedUser = getUserById(request.splat()[0])
                        if (requestedUser == null) map.put("found", false)
                        else {
                            map.put("found", true)
                            requestedUser
                        }
                    } else if (map["user"] == null) webRedirect(request, response, "/login", "/profile") else map["user"]!!) as? User ?: return@get webRedirect(request, response, "/login", "/profile")
                    map.put("isUser", map["user"] == user)
                    map.put("user", user)
                    map.putIfAbsent("user", user)
                    val data = user.getData()
                    map.put("languages", data.languagesSpoken.stringify())
                    map.put("gender", data.gender.display)
                    map.put("musicLibrary", getMusicLibrary(user.id))
                    map.put("hasLocalMusic", (map["musicLibrary"]!! as DatabaseMusicLibrary).tracks.isNotEmpty())
                    map.put("playlists", getPlaylists(user.id))
                    map.put("hasPlaylists", (map["playlists"]!! as List<DatabaseMusicPlaylist>).isNotEmpty())
                    map.put("description", data.selfDescription)
                    map.put("gold", data.gold)
                    handlebars.render(ModelAndView(map, "profile.hbs"))
                }
            })
        })

        // Music
        path("/music", {
            // Server queues
            get("/queue/*", { request, response ->
                val map = request.getDefaultMap(response, "Playlists")
                if (request.splat().isEmpty()) webRedirect(request, response, "/")
                else {
                    val guild = getGuildById(request.splat()[0])
                    if (guild == null) webRedirect(request, response, "/")
                    else {
                        val manager = guild.getAudioManager(null).manager
                        if (manager.current?.track != null) {
                            map.put("isPlaying", true)
                            map.put("current", TrackDisplay(manager.current!!.track!!.info.title, manager.current!!.track!!.info.author))
                        }
                        map.put("queue", manager.queue.toList().toTrackDisplay())
                        map.put("hasQueue", manager.queue.size > 0)
                        map.put("guild", guild)
                        map.put("info", "<b>${manager.queue.map { it.track?.duration ?: 0 }.sum().toMinutesAndSeconds()}</b> | <b>${manager.queue.size}</b> tracks")
                        handlebars.render(ModelAndView(map, "queue.hbs"))
                    }
                }
            })
            // Playlists
            path("/playlist", {
                get("/*", { request, response ->
                    val map = request.getDefaultMap(response, "Playlists")
                    if (request.splat().isEmpty()) webRedirect(request, response, "/")
                    else {
                        val playlist = getPlaylistById(request.splat()[0])
                        if (playlist == null) webRedirect(request, response, "/")
                        else {
                            map.put("isUser", playlist.owner == (map["user"] as User?)?.id)
                            map.put("playlist", playlist)
                            map.put("ownerUser", getUserById(playlist.owner))
                            when {
                                playlist.spotifyAlbumId != null -> {
                                    val album = spotifyApi.albums.getAlbum(playlist.spotifyAlbumId!!)
                                    if (album != null) {
                                        map.put("albumTitle", album.name)
                                        map.put("albumTracks", album.tracks.items)
                                        map.put("albumArtists", album.artists.map { it.name }.stringify())
                                        map.put("albumLink", "https://open.spotify.com/album/${album.id}")
                                        map.put("albumInfo", "<b>${album.tracks.items.map { it.duration_ms }.sum().toLong().toMinutesAndSeconds()}</b> | <b>${album.tracks.total}</b> tracks")
                                    }
                                }
                                playlist.spotifyPlaylistId != null -> {
                                    val split = playlist.spotifyPlaylistId.split("||")
                                    val foundPlaylist = spotifyApi.playlists.getPlaylist(split[0], split[1])
                                    if (foundPlaylist != null) {
                                        map.put("playlistLink", "https://open.spotify.com/user/${foundPlaylist.owner.id}/playlist/${foundPlaylist.id}")
                                        map.put("playlistTitle", foundPlaylist.name)
                                        map.put("playlistOwner", foundPlaylist.owner)
                                        map.put("playlistDescription", foundPlaylist.description)
                                        map.put("playlistTracks", foundPlaylist.tracks.items.map { it.track })
                                        map.put("playlistInfo", "<b>${foundPlaylist.tracks.items.map { it.track.duration_ms }.sum().toLong().toMinutesAndSeconds()}</b> | <b>${foundPlaylist.tracks.total}</b> tracks")
                                    }
                                }
                            }
                            map.put("dbTracks", playlist.tracks)
                            map.put("trackInfo", "<b>${playlist.tracks.size}</b> tracks")
                            map.put("hasDbTracks", playlist.tracks.isNotEmpty())
                            handlebars.render(ModelAndView(map, "playlist.hbs"))
                        }
                    }
                })

            })
        })


        path("/manage", {
            get("", { request, response -> webRedirect(request, response, "/manage/") })
            before("/*", { request, response ->
                val map = request.getDefaultMap(response, "Manage Settings")
                val user = map["user"] as User?
                if (user == null) {
                    webRedirect(request, response, "/login", "/manage/${request.splat().getOrNull(0) ?: ""}")
                }
            })
            get("/*", { request, response ->
                val map = request.getDefaultMap(response, "Manage Settings")
                val user = map["user"] as User
                val params = request.splat()
                if (params.isEmpty()) {
                    map.put("guilds", user.mutualGuilds.filter { it.getMember(user).hasPermission(Permission.MANAGE_SERVER) })
                    ModelAndView(map, "manage.hbs")
                } else {
                    val guild = getGuildById(params[0])
                    if (guild == null) ModelAndView(map, "manage.hbs")
                    else {
                        val data = guild.getData()
                        map.put("guild", guild)
                        map.put("guildData", data)
                        map.put("langs", Language.values())
                        map.put("defaultRole", guild.getRoleById(data.roleSettings.defaultRole ?: "6"))
                        val ch = getTextChannelById(data.messageSettings.joinMessage?.channel)
                        if (ch != null) {
                            map.put("hasReceiverChannel", true)
                            map.put("receiverChannel", ch)
                        }
                        map.put("hasIams", data.roleSettings.autoroles.size > 0)
                        ModelAndView(map, "manageGuild.hbs")
                    }
                }
            }, handlebars)
        })



        get("/accept", { request, response ->
            val map = request.getDefaultMap(response, "Submitting GET")
            val name = request.queryParams("name")
            var redirectUrl = request.queryParams("webRedirect")
            if (name != null) {
                if (map["user"] != null) {
                    val user = map["user"] as User
                    when (name) {
                        "removeautorole" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val autorole = request.queryParams("autorolename")
                                if (autorole != null) {
                                    val data = guild.getData()
                                    data.roleSettings.autoroles.removeIf { it.name == autorole }
                                    data.update(true)
                                }
                            }
                        }
                        "addautorole" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val autorole = request.queryParams("autorolename")
                                val role = request.queryParams("autorolerole")
                                if (autorole != null && role != null) {
                                    val data = guild.getData()
                                    data.roleSettings.autoroles.add(Autorole(autorole, role, user.id))
                                    data.update(true)
                                }
                            }
                        }
                        "defaultrole" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val defaultRole = request.queryParams("defaultRole")
                                if (defaultRole != null) {
                                    val data = guild.getData()
                                    if (defaultRole == "none") data.roleSettings.defaultRole = null
                                    else data.roleSettings.defaultRole = defaultRole
                                    data.update(true)
                                }
                            }
                        }
                        "changemessage" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val uneditedMessage = if (request.queryParams("type") == "join") request.queryParams("joinmessage") else request.queryParams("leavemessage")
                                val editedList = mutableListOf<String>()
                                uneditedMessage.split(" ").forEach { word ->
                                    if (word.startsWith("#")) {
                                        val lookup = word.removePrefix("#")
                                        val results = guild.getTextChannelsByName(lookup, true)
                                        if (results.size > 0) editedList.add(results[0].asMention)
                                        else editedList.add(word)
                                    } else if (word.startsWith("@")) {
                                        val lookup = word.removePrefix("@")
                                        val found = getUserById(lookup)
                                        if (found != null) editedList.add(found.asMention)
                                        else editedList.add(word)
                                    } else editedList.add(word)
                                }
                                val editedMessage = editedList.concat()
                                val isJoin = request.queryParams("type") == "join"
                                val data = guild.getData()
                                if (isJoin) {
                                    if (data.messageSettings.joinMessage == null) data.messageSettings.joinMessage = JoinMessage(editedMessage, user.id, System.currentTimeMillis(), null)
                                    else {
                                        data.messageSettings.joinMessage!!.message = editedMessage
                                        data.messageSettings.joinMessage!!.lastEditedAt = System.currentTimeMillis()
                                        data.messageSettings.joinMessage!!.lastEditedBy = user.id
                                    }
                                } else {
                                    if (data.messageSettings.leaveMessage == null) data.messageSettings.leaveMessage = LeaveMessage(editedMessage, user.id, System.currentTimeMillis(), null)
                                    else {
                                        data.messageSettings.leaveMessage!!.message = editedMessage
                                        data.messageSettings.leaveMessage!!.lastEditedAt = System.currentTimeMillis()
                                        data.messageSettings.leaveMessage!!.lastEditedBy = user.id
                                    }
                                }
                                data.update(true)
                            }
                        }
                        "automessagechannel" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val channelId = request.queryParams("messagechannelid")
                                if (channelId != null) {
                                    val data = guild.getData()
                                    if (data.messageSettings.joinMessage == null) {
                                        data.messageSettings.joinMessage = JoinMessage(null, user.id, System.currentTimeMillis(), channelId, true)
                                    } else data.messageSettings.joinMessage!!.channel = channelId

                                    if (data.messageSettings.leaveMessage == null) {
                                        data.messageSettings.leaveMessage = LeaveMessage(null, user.id, System.currentTimeMillis(), channelId, true)
                                    } else data.messageSettings.leaveMessage!!.channel = channelId

                                    data.update(true)
                                }
                            }
                        }
                        "disablejoin" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val enable = when (request.queryParams("state")) {
                                    "on" -> true
                                    "off" -> false
                                    else -> null
                                }
                                println(enable)
                                if (enable != null) {
                                    val data = guild.getData()
                                    if (data.messageSettings.joinMessage == null) data.messageSettings.joinMessage =
                                            JoinMessage(null, user.id, System.currentTimeMillis(), null, enable)
                                    else {
                                        data.messageSettings.joinMessage!!.enabled = enable
                                        data.messageSettings.joinMessage!!.lastEditedAt = System.currentTimeMillis()
                                        data.messageSettings.joinMessage!!.lastEditedBy = user.id
                                    }
                                    data.update(true)
                                }
                            }
                        }
                        "disableleave" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val enable = when (request.queryParams("state")) {
                                    "on" -> true
                                    "off" -> false
                                    else -> null
                                }
                                if (enable != null) {
                                    val data = guild.getData()
                                    if (data.messageSettings.leaveMessage == null) data.messageSettings.leaveMessage =
                                            LeaveMessage(null, user.id, System.currentTimeMillis(), null, enable)
                                    else {
                                        data.messageSettings.leaveMessage!!.enabled = enable
                                        data.messageSettings.leaveMessage!!.lastEditedAt = System.currentTimeMillis()
                                        data.messageSettings.leaveMessage!!.lastEditedBy = user.id
                                    }
                                    data.update(true)
                                }
                            }
                        }
                        "stayinvoice" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val enable = when (request.queryParams("state")) {
                                    "on" -> true
                                    "off" -> false
                                    else -> null
                                }
                                if (enable != null) {
                                    val data = guild.getData()
                                    data.musicSettings.stayInChannel = enable
                                    data.update(true)
                                }
                            }
                        }
                        "changemusicadmin" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val enable = when (request.queryParams("state")) {
                                    "on" -> true
                                    "off" -> false
                                    else -> null
                                }
                                if (enable != null) {
                                    val data = guild.getData()
                                    data.musicSettings.canEveryoneUseAdminCommands = enable
                                    data.update(true)
                                }
                            }
                        }
                        "changeautoplay" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val enable = when (request.queryParams("state")) {
                                    "on" -> true
                                    "off" -> false
                                    else -> null
                                }
                                if (enable != null) {
                                    val data = guild.getData()
                                    data.musicSettings.autoplay = enable
                                    data.update(true)
                                }
                            }
                        }
                        "changelang" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val newLang = request.queryParams("lang")
                                if (newLang != null) {
                                    val data = guild.getData()
                                    data.languageSettings.language = newLang
                                    data.update(true)
                                }
                            }
                        }
                        "changeprefix" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val newPrefix = request.queryParams("newprefix")
                                if (newPrefix != null) {
                                    val data = guild.getData()
                                    data.prefixSettings.prefix = newPrefix
                                    data.update(true)
                                }
                            }
                        }
                        "defaultprefix" -> {
                            val guild = getGuildById(request.queryParams("guild") ?: "")
                            if (guild != null && guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                                val disable = when (request.queryParams("state")) {
                                    "on" -> true
                                    "off" -> false
                                    else -> null
                                }
                                if (disable != null) {
                                    val data = guild.getData()
                                    data.prefixSettings.disabledDefaultPrefix = disable
                                    data.update(true)
                                }
                            }
                        }
                        "addsong", "add-song" -> {
                            val url = request.queryParams("song")
                            val playlistId = request.queryParams("playlistId")
                            if (playlistId == null) {
                                val library = getMusicLibrary(user.id)
                                url.loadExternally { audioTrack, _ ->
                                    library.lastModified = System.currentTimeMillis()
                                    library.tracks.add(DatabaseTrackObj(user.id, System.currentTimeMillis(), null, audioTrack.info.title,
                                            audioTrack.info.author, if (url.startsWith("https")) url else audioTrack.info.uri))
                                    library.update("musicLibraries", user.id)
                                    Thread.sleep(5)
                                }
                            } else {
                                val playlist = getPlaylistById(playlistId)
                                if (playlist?.owner?.equals(user.id) == true) {
                                    url.loadExternally { audioTrack, _ ->
                                        playlist.lastModified = System.currentTimeMillis()
                                        playlist.tracks.add(DatabaseTrackObj(user.id, System.currentTimeMillis(), playlist.id, audioTrack.info.title,
                                                audioTrack.info.author, if (url.startsWith("https")) url else audioTrack.info.uri))
                                        playlist.update("musicPlaylists", playlist.id)
                                    }
                                }
                            }
                        }
                        "cloneplaylist" -> {
                            val playlistId = request.queryParams("playlistId")
                            if (playlistId != null) {
                                val playlist = getPlaylistById(playlistId)
                                if (playlist != null && playlist.owner != user.id) {
                                    val id = genId(6, "musicPlaylists")
                                    playlist.copy(id = id, owner = user.id).insert("musicPlaylists")
                                    redirectUrl = "/music/playlist/$id"
                                }
                            }
                        }
                        "removesong" -> {
                            val url = request.queryParams("song")
                            val library = getMusicLibrary(user.id)
                            library.tracks.removeIf { it.title == url || it.url == url }
                            library.lastModified = System.currentTimeMillis()
                            library.update("musicLibraries", user.id)
                        }
                        "removefromplaylist" -> {
                            val url = request.queryParams("song")
                            val playlist = getPlaylistById(request.queryParams("playlist") ?: "")
                            if (url != null && playlist?.tracks != null && playlist.owner == user.id) {
                                playlist.tracks.removeIf { it.title == url || it.url == url }
                                playlist.update("musicPlaylists", playlist.id)
                            }
                        }
                    }
                }
            }
            webRedirect(request, response, redirectUrl ?: "/")
        })

    }
}

private fun startup() {
    if (!test) {
        port(443)
        secure("/root/Ardent/ssl/keystore.p12", "mortimer5", null, null)
    } else port(80)
    staticFiles.location("/public")
}

fun registerHandlebarHelpers() {
    val field = handlebars::class.java.getDeclaredField("handlebars")
    field.isAccessible = true
    val handle = field.get(handlebars) as Handlebars
    handle.registerHelper("date", { date: Long, _: Options -> date.readableDate() })
    handle.registerHelper("artistConcat", { artist: List<SimpleArtist>, _: Options -> artist.map { it.name }.stringify() })
    handle.registerHelper("langname", { lang: String?, _: Options -> lang?.toLanguage()?.readable })
    handle.registerHelper("getfancyuser", { string: String?, _: Options -> getUserById(string)?.toFancyString() })
    handle.registerHelper("rolename", { role: String?, _: Options -> getRoleById(role)?.name })
}


fun Request.getDefaultMap(response: Response, title: String): HashMap<Any, Any?> {
    val map = hashMapOf<Any, Any?>()
    val session = session()
    map.put("user", session.attribute("user"))
    map.put("validSession", map["user"] != null)
    map.put("title", title)
    if (cookie("error") != null) {
        map.put("hasError", true)
        map.put("error", cookie("error"))
        response.removeCookie("error")
    }
    return map
}
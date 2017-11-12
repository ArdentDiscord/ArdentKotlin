import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Options
import main.jdas
import main.test
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine
import utils.discord.getData
import utils.discord.getUserById
import utils.discord.internals
import utils.functionality.loadExternally
import utils.functionality.readableDate
import utils.functionality.stringify
import utils.functionality.update
import utils.music.DatabaseMusicLibrary
import utils.music.DatabaseMusicPlaylist
import utils.music.DatabaseTrackObj
import utils.music.getMusicLibrary
import web.identityObject
import web.retrieveToken

val handlebars = HandlebarsTemplateEngine()

val loginRedirect = if (test) "http://localhost/api/oauth/login" else "https://ardentbot.com/api/oauth/login"

class Web {
    private fun redirect(response: Response, redirect_to: String, redirect_back: String? = null) {
        response.removeCookie("redirect_back")
        if (redirect_back != null) response.cookie("redirect_back", redirect_back)
        response.redirect(redirect_to)
    }

    private fun redirectIfExists(request: Request, response: Response, redirect_failure_path: String) {
        if (request.cookie("redirect_back") == null) response.redirect(redirect_failure_path);
        else response.redirect(request.cookie("redirect_back"))
    }

    init {
        startup()
        registerHandlebarHelpers()

        get("/api/oauth/login", { request, response ->
            if (request.queryParams("code") == null) {
                redirect(response, "/login")
                null
            } else {
                val code = request.queryParams("code")
                val token = retrieveToken(code)
                if (token == null) {
                    redirect(response, "/login")
                    null
                } else {
                    val identification = identityObject(token.access_token)
                    if (identification != null) {
                        val session = request.session()
                        val user = getUserById(identification.id)
                        if (user == null) redirect(response, "/login")
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


        // Profiles
        path("/profile", {
           get("", {request, response -> response.redirect("/profile/") })
            get("/*", { request, response ->
                val map = request.getDefaultMap(response, "Profiles")
                if (request.splat().isEmpty() && map["user"] == null) redirect(response, "/login", "/profile")
                else {
                    val user = (if (request.splat().size == 1) {
                        val requestedUser = getUserById(request.splat()[0])
                        if (requestedUser == null) map.put("found", false)
                        else {
                            map.put("found", true)
                            requestedUser
                        }
                    } else if (map["user"] == null) redirect(response, "/login", "/profile") else map["user"]!!) as? User ?: redirect(response, "/login", "/profile")
                    user as User
                    map.put("isUser", map["user"] == user)
                    map.putIfAbsent("user", user)
                    val data = user.getData()
                    map.put("languages", data.languagesSpoken.stringify())
                    map.put("gender", data.gender.display)
                    map.put("musicLibrary", data.getMusicLibrary())
                    map.put("hasLocalMusic", (map["musicLibrary"]!! as DatabaseMusicLibrary).tracks.isNotEmpty())
                    map.put("linkedPlaylists", data.getLinkedPlaylists())
                    map.put("playlists", data.getPlaylists())
                    map.put("hasPlaylists", (map["playlists"]!! as List<DatabaseMusicPlaylist>).isNotEmpty())
                    map.put("hasLinkedPlaylists", (map["linkedPlaylists"]!! as List<DatabaseMusicPlaylist>).isNotEmpty())
                    map.put("description", data.selfDescription)
                    map.put("gold", data.gold)
                    handlebars.render(ModelAndView(map, "profile.hbs"))
                }
            })
        })

        // Music
        path("/music", {
            // Playlists
            path("/playlist", {
                get("/new", { request, response ->

                })
            })
        })


        path("/manage", {
            before("/*", { request, response ->
                val map = request.getDefaultMap(response, "Manage Settings")
                val user = map["user"] as User?
                if (request.splat().isEmpty() || user == null) {
                    this.redirect(response, "/login", "/manage/${if (request.splat().isEmpty()) "" else request.splat()[0]}")
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
                    ModelAndView(map, "manage.hbs")
                }
            }, handlebars)
        })



        get("/accept", { request, response ->
            val map = request.getDefaultMap(response, "POST")
            val name = request.queryParams("name")
            val redirectUrl = request.queryParams("redirect")
            if (name != null) {
                if (map["user"] != null) {
                    val user = map["user"] as User
                    when (name) {
                        "addsong" -> {
                            val url = request.queryParams("song")
                            val library = getMusicLibrary(user.id)
                            url.loadExternally { audioTrack, _ ->
                                library.lastModified = System.currentTimeMillis()
                                library.tracks.add(DatabaseTrackObj(user.id, System.currentTimeMillis(), null, audioTrack.info.title,
                                        audioTrack.info.author, if (url.startsWith("https")) url else audioTrack.info.uri))
                                library.update("musicLibraries", user.id)
                            }
                        }
                        "removesong" -> {
                            val url = request.queryParams("song")
                            val library = getMusicLibrary(user.id)
                            library.tracks.removeIf { it.url == url }
                            library.lastModified = System.currentTimeMillis()
                            library.update("musicLibraries", user.id)
                        }
                    }
                }
            }
            redirect(response, redirectUrl ?: "/")
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
    handle.registerHelper("date", { date: Long, options: Options -> date.readableDate() })
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
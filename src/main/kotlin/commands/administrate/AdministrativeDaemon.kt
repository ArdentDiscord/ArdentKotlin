package commands.administrate

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.wrapper.spotify.models.ClientCredentials
import main.config
import main.conn
import main.r
import main.spotifyApi
import org.jsoup.Jsoup
import utils.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


val administrativeExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(6)

class AdministrativeDaemon : Runnable {
    override fun run() {
        val activePunishments = r.table("punishments").run<Any>(conn).queryAsArrayList(Punishment::class.java)
        val time = System.currentTimeMillis()
        activePunishments.forEach { punishment ->
            if (punishment != null) {
                val guild = getGuildById(punishment.guildId)
                val user = getUserById(punishment.userId)
                if (user == null || guild == null) {
                    r.table("punishments").get(punishment.id).delete().runNoReply(conn)
                } else {
                    if (time > punishment.expiration) {
                        r.table("punishments").get(punishment.id).delete().runNoReply(conn)
                        when (punishment.type) {
                            Punishment.Type.TEMPBAN -> {
                                guild.controller.unban(user.id).queue({
                                    user.openPrivateChannel().queue { privateChannel -> privateChannel.send("You were unbanned from **{0}**".tr(guild, guild.name)) }

                                }, {
                                    user.openPrivateChannel().queue { privateChannel ->
                                        privateChannel.send("Your punishment expired but I do not have the permissions to unban you. Please contact {0}".tr(guild, guild.owner.asMention))
                                    }

                                })
                            }
                            Punishment.Type.MUTE -> {
                                user.openPrivateChannel().queue { privateChannel -> privateChannel.send("You were unmuted in **{0}**".tr(guild, guild.name)) }
                            }
                        }
                    }
                }
            }
        }
        val stats = internals
        try {
            println(Jsoup.connect("https://discordbots.org/api/bots/339101087569281045/stats")
                    .header("Authorization", config.getValue("discordbotsorg"))
                    .data("server_count", stats.guilds.toString())
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .post().body().text())
            println(Jsoup.connect("https://bots.discord.pw/api/bots/339101087569281045/stats")
                    .header("Authorization", config.getValue("botsdiscordpw"))
                    .data("server_count", stats.guilds.toString())
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .post().body().text())
        } catch (e: Exception) {
            e.log()
        }
    }
}

fun startAdministrativeDaemon() {
    val refresh = {
        val request = spotifyApi.clientCredentialsGrant().build()
        val responseFuture = request.async
        Futures.addCallback(responseFuture, object : FutureCallback<ClientCredentials> {
            override fun onSuccess(result: ClientCredentials?) {
                spotifyApi.setAccessToken(result!!.accessToken)
            }

            override fun onFailure(throwable: Throwable) {}
        })
    }
    refresh.invoke()
    administrativeExecutor.scheduleAtFixedRate(refresh, 9, 9, TimeUnit.MINUTES)
    val administrativeDaemon = AdministrativeDaemon()
    administrativeExecutor.scheduleAtFixedRate(administrativeDaemon, 6, 30, TimeUnit.SECONDS)
    val ranksDaemon = RanksDaemon()
    administrativeExecutor.scheduleAtFixedRate(ranksDaemon, 6, 30, TimeUnit.SECONDS)
}

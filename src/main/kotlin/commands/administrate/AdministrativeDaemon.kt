package commands.administrate

import main.*
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
                                guild.controller.unban(user.id).queue( {
                                    user.openPrivateChannel().queue { privateChannel -> privateChannel.send(user, "You were unbanned from **${guild.name}**") }

                                }, {
                                    user.openPrivateChannel().queue { privateChannel -> privateChannel.send(user, "Your punishment expired but I do not have " +
                                            "the permissions to unban you. Please contact ${guild.owner.asMention}") }

                                })
                            }
                            Punishment.Type.MUTE -> {
                                guild.controller.removeRolesFromMember(guild.getMember(user), guild.getRolesByName("muted", true))
                                        .reason("Automatic Unmute").queue({
                                    user.openPrivateChannel().queue { privateChannel -> privateChannel.send(user, "You were unmuted in **${guild.name}**") }
                                }, {
                                    user.openPrivateChannel().queue { privateChannel -> privateChannel.send(user, "I was unable to unmute you in **${guild.name}**." +
                                            " Please contact a server administrator to resolve your mute.") }
                                })
                            }
                        }
                    }
                }
            }
        }
        val stats = Internals()
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
        }
        catch (e: Exception) {
            e.log()
        }
    }
}

fun startAdministrativeDaemon() {
    val administrativeDaemon = AdministrativeDaemon()
    administrativeExecutor.scheduleAtFixedRate(administrativeDaemon, 6, 30, TimeUnit.SECONDS)
    val ranksDaemon = RanksDaemon()
    administrativeExecutor.scheduleAtFixedRate(ranksDaemon, 6, 30, TimeUnit.SECONDS)
}

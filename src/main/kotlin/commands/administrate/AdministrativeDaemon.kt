package commands.administrate

import main.*
import org.jsoup.Jsoup
import utils.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val administrativeExecutor = Executors.newScheduledThreadPool(6)

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
                                guild.controller.unban(user.id).queue()
                                user.openPrivateChannel().queue { privateChannel -> privateChannel.send(user, "You were unbanned from **${guild.name}**") }
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
        Jsoup.connect("https://discordbots.org/api/bots/339101087569281045/stats")
                .header("Authorization", config.getValue("discordbotsorg"))
                .data("shard_count", jdas.size.toString())
                .data("server_count", stats.guilds.toString())
                .post().body().text()
        Jsoup.connect("https://bots.discord.pw/api/bots/339101087569281045/stats")
                .header("Authorization", config.getValue("botsdiscordpw"))
                .data("shard_count", jdas.size.toString())
                .data("server_count", stats.guilds.toString())
                .post().body().text()
    }
}

fun startAdministrativeDaemon() {
    val administrativeDaemon = AdministrativeDaemon()
    administrativeExecutor.scheduleWithFixedDelay(administrativeDaemon, 1, 60, TimeUnit.SECONDS)
    val ranksDaemon = RanksDaemon()
    administrativeExecutor.scheduleWithFixedDelay(ranksDaemon, 1, 60, TimeUnit.SECONDS)
}

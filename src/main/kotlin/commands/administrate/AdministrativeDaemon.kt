package commands.administrate

import main.conn
import main.factory
import main.r
import utils.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val administrativeExecutor = Executors.newScheduledThreadPool(2)

class AdministrativeDaemon : Runnable {
    override fun run() {
        val activePunishments = r.table("punishments").run<Any>(conn).queryAsArrayList(Punishment::class.java)
        val time = System.currentTimeMillis()
        activePunishments.forEach { punishment ->
            if (punishment != null) {
                val guild = getGuildById(punishment.guildId)
                val user = getUserById(punishment.userId)
                if (user == null || guild == null) {
                    r.table("punishments").get(punishment.uuid).delete().runNoReply(conn)
                } else {
                    if (time > punishment.expiration) {
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
                        r.table("punishments").get(punishment.uuid).delete().runNoReply(conn)
                    }
                }
            }
        }
    }
}

fun startAdministrativeDaemon() {
    val administrativeDaemon = AdministrativeDaemon()
    administrativeExecutor.scheduleWithFixedDelay(administrativeDaemon, 1, 45, TimeUnit.SECONDS)
    val ranksDaemon = RanksDaemon()
    administrativeExecutor.scheduleWithFixedDelay(ranksDaemon, 1, 60, TimeUnit.SECONDS)
}

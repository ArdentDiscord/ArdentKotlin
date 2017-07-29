package commands.administrate

import main.conn
import main.jda
import main.r
import utils.Punishment
import utils.id
import utils.queryAsArrayList
import utils.send
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val administrativeExecutor = Executors.newSingleThreadScheduledExecutor()

class AdministrativeDaemon : Runnable {
    override fun run() {
        val activePunishments = r.table("punishments").run<Any>(conn).queryAsArrayList(Punishment::class.java)
        val time = System.currentTimeMillis()
        activePunishments.forEach { punishment ->
            if (punishment != null) {
                val guild = jda!!.getGuildById(punishment.guildId)
                val user = jda!!.getUserById(punishment.userId)
                if (user == null) {
                    r.table("punishments").get(punishment.uuid).delete().runNoReply(conn)
                } else {
                    if (time > punishment.expiration) {
                        guild.controller.unban(user.id).queue()
                        user.openPrivateChannel().queue { privateChannel ->
                            privateChannel.send(user,
                                    when (punishment.type) {
                                        Punishment.Type.TEMPBAN -> "You were unbanned from **${guild.name}**"
                                        Punishment.Type.MUTE -> "You were unmuted from **${guild.name}**"
                                    })
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
    administrativeExecutor.scheduleAtFixedRate(administrativeDaemon, 5, 20, TimeUnit.SECONDS)
}

package commands.manage

import main.conn
import main.jda
import main.r
import utils.Punishment
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
                val member = guild?.getMemberById(punishment.userId)
                if (member == null) {
                    r.table("punishments").get(punishment.uuid).delete().runNoReply(conn)
                } else {
                    if (punishment.expiration > time) {
                        member.user.openPrivateChannel().queue { privateChannel ->
                            privateChannel.send(member,
                                    when (punishment.type) {
                                        Punishment.Type.TEMPBAN -> "You were unbanned from **${guild.name}**"
                                        Punishment.Type.MUTE -> "You were unmuted from **${guild.name}**"
                                    })
                        }
                    }
                }
            }
        }
    }
}

fun startAdministrativeDaemon() {
    val administrativeDaemon = AdministrativeDaemon()
    administrativeExecutor.scheduleAtFixedRate(administrativeDaemon, 0, 1, TimeUnit.MINUTES)
}

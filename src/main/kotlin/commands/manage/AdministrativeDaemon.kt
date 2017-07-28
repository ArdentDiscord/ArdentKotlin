package commands.manage

import main.conn
import main.r
import utils.Punishment
import utils.queryAsArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val administrativeExecutor = Executors.newSingleThreadScheduledExecutor()
class AdministrativeDaemon : Runnable {
    override fun run() {
        val activePunishments = r.table("punishments").run<Any>(conn).queryAsArrayList(Punishment::class.java)
        activePunishments.forEach { punishment ->
            if ()
        }
    }
}

fun startAdministrativeDaemon() {
    val administrativeDaemon = AdministrativeDaemon()
    administrativeExecutor.scheduleAtFixedRate(administrativeDaemon, 0, 1, TimeUnit.MINUTES)
}

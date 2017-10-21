package commands.administrate

import main.config
import main.managers
import main.test
import org.jsoup.Jsoup
import utils.discord.getGuildById
import utils.discord.internals
import utils.functionality.log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


val administrativeExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(6)

class AdministrativeDaemon : Runnable {
    override fun run() {
        val iterator = managers.iterator()
        while (iterator.hasNext()) if (getGuildById(iterator.next().key.toString())?.audioManager?.isConnected != true) iterator.remove()
        if (test) return
        val stats = internals
        try {
            Jsoup.connect("https://discordbots.org/api/bots/339101087569281045/stats")
                    .header("Authorization", config.getValue("discordbotsorg"))
                    .data("server_count", stats.guilds.toString())
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .post().body().text()
            Jsoup.connect("https://bots.discord.pw/api/bots/339101087569281045/stats")
                    .header("Authorization", config.getValue("botsdiscordpw"))
                    .header("Content-Type", "application/json")
                    .requestBody("{\"server_count\": ${stats.guilds}}")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .post().body().text()
        } catch (e: Exception) {
            e.log()
        }
    }
}
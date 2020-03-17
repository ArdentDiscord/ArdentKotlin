package commands.administrate

import com.patreon.PatreonAPI
import main.*
import org.jsoup.Jsoup
import utils.discord.*
import utils.functionality.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


val administrativeExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(6)
val api = PatreonAPI(config.getValue("patreon"))
val patronRole = hangout!!.getRoleById("351370393485049876")
val supporterRole = hangout!!.getRoleById("366528603116273664")
val premiumRole = hangout!!.getRoleById("371361751498883072")
val sponsorRole = hangout!!.getRoleById("371361925717688321")

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

        val patrons = hashMapOf<String, PatronLevel>()
        val pledgeResponse = api.getPledges("758508", 20, null)
        pledgeResponse.pledges.forEach { pledge ->
            pledgeResponse.patrons.forEach { patron ->
                val discordId = patron.attributes.discordId ?: patron.attributes.socialConnections.discord.userId
                if (discordId != null && patron.id == pledge.simplePatron.data.id) {
                    if (pledge.attributes.declinedSince == null) {
                        patrons.put(discordId, when (pledge.attributes.amountCents) {
                            in 100..299 -> PatronLevel.SUPPORTER
                            in 300..499 -> PatronLevel.PREMIUM
                            else -> PatronLevel.SPONSOR
                        })
                    } else {
                        val user = getUserById(discordId)
                        if (user != null) getTextChannelById("365385845798207490")!!.send("${user.asMention} **has declined payment** - go ask them and tell them they will lose their permissions")
                    }
                }
            }
        }

        val dbPatrons = r.table("patrons").run<Any>(conn).queryAsArrayList(Patron::class.java)
        dbPatrons.forEach { dbPatron ->
            if (dbPatron != null) {
                if (!patrons.map { it.key }.contains(dbPatron.id)) {
                    r.table("patrons").filter(r.hashMap("id", dbPatron.id)).delete().runNoReply(conn)
                    getUserById(dbPatron.id)?.openPrivateChannel()?.queue {
                        it.send("We detected that you either charged back or otherwise didn't pay your Patron dues. You can still keep your perks if you pay at <https://patreon.com/ardent> !")
                    }
                }
            }
        }
        patrons.forEach { id, level ->
            if (asPojo(r.table("patrons").filter(r.hashMap("id", id)).run(conn), Patron::class.java) == null) {
                Patron(id, level).insert("patrons")
                val member = hangout!!.getMemberById(id)
                if (member == null) {
                    getUserById(id)!!.openPrivateChannel().queue {
                        it.send("Thanks for supporting Ardent, we appreciate it! Remember to join " +
                                "our hangout @ <$hostname/server> to give us feedback, suggestions, or just to say hi! ${Emoji.WAVING_HANDS.symbol}")
                    }
                } else {
                    member.user.openPrivateChannel().queue { it.send("Registered your patronage ${Emoji.THUMBS_UP.symbol} - thanks for supporting us!") }
                    val roleToGive = when (level) {
                        PatronLevel.SUPPORTER -> supporterRole
                        PatronLevel.PREMIUM -> premiumRole
                        PatronLevel.SPONSOR -> sponsorRole
                    }!!
                    if (!member.roles.contains(roleToGive)) hangout!!.addRoleToMember(member, roleToGive).queue()
                    if (!member.roles.contains(patronRole)) hangout!!.addRoleToMember(member, patronRole!!).queue()
                }
            }
        }
    }
}
package commands.administrate

import com.patreon.API
import main.config
import main.conn
import main.r
import org.json.JSONObject
import utils.*
import java.util.concurrent.CopyOnWriteArrayList

val staff = CopyOnWriteArrayList<Staff>()

class RanksDaemon : Runnable {
    val api = API(config.getValue("patreon"))
    override fun run() {
        try {
            val staffMembers = r.table("staff").run<Any>(conn).queryAsArrayList(Staff::class.java)
            staffMembers.forEach { member ->
                if (member != null) {
                    staff.forEach { if (member.id == it.id) staff.remove(it) }
                    staff.add(member)
                }
            }
            val arrayedCampaignData = api.fetchCampaignAndPatrons().getJSONArray("included")
            val mappedPatreonDiscordIds = hashMapOf<String, String>()
            val successfulSearches = mutableListOf<String>()
            arrayedCampaignData.forEach {
                val current = it as JSONObject
                val patreonId = current.getString("id")
                if (current.getString("type") == "user" && patreonId != "5140688") {
                    try {
                        val discordId = current.getJSONObject("attributes").getJSONObject("social_connections").toString().split("{\"user_id\":\"")[1].removeSuffix("\"},\"twitch\":null,\"facebook\":null,\"spotify\":null}")
                        mappedPatreonDiscordIds.put(patreonId, discordId)
                    } catch(ignored: Throwable) {
                    }
                }
            }
            arrayedCampaignData.forEach {
                val current = it as JSONObject
                if (current.getString("type") == "pledge") {
                    val patreonId = current.getJSONObject("relationships").getJSONObject("patron")
                            .getJSONObject("data").getString("id")
                    val amount = current.getJSONObject("attributes").getInt("amount_cents")
                    val level = when (amount) {
                        in 100..299 -> DonationLevel.SUPPORTER
                        in 300..499 -> DonationLevel.BASIC
                        in 500..999 -> DonationLevel.INTERMEDIATE
                        else -> DonationLevel.EXTREME
                    }
                    val discordId = mappedPatreonDiscordIds[patreonId]
                    if (discordId != null) {
                        successfulSearches.add(discordId)
                        val info = asPojo(r.table("patrons").get(discordId).run(conn), Patron::class.java)
                        if (info == null) Patron(discordId, level).insert("patrons")
                        else if (info.donationLevel != level) r.table("patrons").get(discordId).update(r.hashMap("donationLevel", level.name)).runNoReply(conn)
                    }
                }
            }
            r.table("patrons").run<Any>(conn).queryAsArrayList(Patron::class.java).forEach { patron ->
                if (patron != null) {
                    if (!successfulSearches.contains(patron.id)) r.table("patrons").get(patron.id).delete().runNoReply(conn)
                }
            }
            r.table("playerData").run<Any>(conn).queryAsArrayList(PlayerData::class.java).forEach {
                if (it != null) {
                    if (it.id.toUser() == null) r.table("playerData").get(it.id).delete().runNoReply(conn)
                }
            }
        } catch (e: Exception) {
            e.log()
        }
    }
}

data class Staff(val id: String, val role: StaffRole) {
    enum class StaffRole {
        HELPER, MODERATOR, ADMINISTRATOR
    }
}

fun List<Staff>.filterByRole(role: Staff.StaffRole): MutableList<Staff> {
    val members = mutableListOf<Staff>()
    forEach { if (it.role == role) members.add(it) }
    return members
}
package commands.administrate

import com.patreon.PatreonAPI
import main.config
import main.conn
import main.hangout
import main.r
import utils.*
import utils.functionality.insert
import utils.functionality.logChannel
import utils.functionality.queryAsArrayList
import java.util.concurrent.CopyOnWriteArrayList

val staff = CopyOnWriteArrayList<Staff>()

class RanksDaemon : Runnable {
    val api = PatreonAPI(config.getValue("patreon"))
    override fun run() {
        try {
            val staffMembers = r.table("staff").run<Any>(conn).queryAsArrayList(Staff::class.java)
            staffMembers.forEach { member ->
                if (member != null) {
                    staff.forEach { if (member.id == it.id) staff.remove(it) }
                    staff.add(member)
                }
            }
            val patrons = hashMapOf<String, DonationLevel>()
            val pledgeResponse = api.getPledges("758508", 20, null)
            pledgeResponse.pledges.forEach { pledge ->
                pledgeResponse.patrons.forEach { patron ->
                    val discordId = patron.attributes.discordId ?: patron.attributes.socialConnections.discord.userId
                    if (discordId != null && patron.id == pledge.simplePatron.data.id) {
                        if (pledge.attributes.declinedSince == null) {
                            patrons.put(discordId, when (pledge.attributes.amountCents) {
                                in 100..299 -> DonationLevel.SUPPORTER
                                in 300..499 -> DonationLevel.BASIC
                                in 500..999 -> DonationLevel.INTERMEDIATE
                                else -> DonationLevel.EXTREME
                            })
                        } else {
                            val user = getUserById(discordId)
                            if (user != null) "365385845798207490".toChannel()!!.send("${user.asMention} **has declined payment** - go ask them and tell them they will lose their permissions")
                        }
                    }

                }
            }
            r.table("patrons").delete().runNoReply(conn)
            val patronRole = hangout!!.getRolesByName("Patron", true)[0]
            patrons.forEach { id, level ->
                Patron(id, level).insert("patrons")
                val member = hangout!!.getMemberById(id)
                if (member != null && !member.roles.contains(patronRole)) {
                    hangout!!.controller.addRolesToMember(member, patronRole).reason("Automated Patron Check").queue {
                        member.user.openPrivateChannel().queue { it.send("Gave you the Patron role in **Ardent Hangout**. Thanks for supporting us!") }
                        logChannel!!.send("Added Patron role to ${member.asMention}")
                    }
                }
            }
            System.out.println("Updated ${patrons.size} patron records")
            r.table("playerData").run<Any>(conn).queryAsArrayList(PlayerData::class.java).forEach {
                if (it != null) {
                    if (it.id.toUser() == null) r.table("playerData").get(it.id).delete().runNoReply(conn)
                }
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
    }
}

data class Staff(val id: String, val role: StaffRole) {
    enum class StaffRole {
        HELPER, MODERATOR, ADMINISTRATOR
    }
}

fun filterByRole(role: Staff.StaffRole): MutableList<Staff> {
    return staff.filter { it.role == role }.toMutableList()
}
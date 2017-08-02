package commands.administrate

import main.conn
import main.r
import utils.DonationLevel
import utils.getGson
import utils.queryAsArrayList

val staff = mutableListOf<Staff>()

class RanksDaemon : Runnable {
    override fun run() {
        val staffMembers = r.table("staff").run<Any>(conn).queryAsArrayList(Staff::class.java)
        staffMembers.forEach { member ->
            if (member != null) {
                staff.forEach { if (member.id == it.id) return }
                staff.add(member)
            }
        }
    }
}

data class Staff(val id: String, val role: StaffRole) {
    enum class StaffRole {
        HELPER, MODERATOR, ADMINISTRATOR
    }
}
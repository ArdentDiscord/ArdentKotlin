package utils

import main.conn
import main.r
import net.dv8tion.jda.core.entities.User
import org.apache.commons.lang3.text.WordUtils

data class TriviaCategory(val id: Int, val title: String, val created_at: String, val updated_at: String, val clues_count: Int) {
    fun getCategoryName(): String {
        return title.split(" ").map { WordUtils.capitalize(it) }.concat()
    }
}
data class TriviaQuestion(val id: String, val answer: String, val question: String, val value: Int, val airdate: String,
                          val created_at: String, val updated_at: String, val category_id: Int, val game_id: Int?,
                          val invalid_count: Int?, val category: TriviaCategory) {
    fun test(guess: String): Boolean {
        return guess.equals(answer, true)
    }
}
data class Marriage(var userOne: String, var userTwo: String, val id: String = r.uuid().run(conn))
data class Patron(var id: String, var donationLevel: DonationLevel)

data class SpecialPerson(var id: String, var backer: String)
data class Announcement(val date: String, val dateLong: Long, val writer: User, val content: String)
data class AnnouncementModel(var date: Long, var writer: String, var content: String) {
    fun toAnnouncement(): Announcement {
        return Announcement(date.readableDate(), date, writer.toUser()!!, content)
    }
}

class GuildData(val id: String, var prefix: String, var musicSettings: MusicSettings, var advancedPermissions: MutableList<String>, var iamList: MutableList<Iam> = mutableListOf(), var joinMessage: Pair<String?, String? /* Message then Channel ID */>? = null, var leaveMessage: Pair<String?, String?>? = null, var defaultRole: String? = null, var allowGlobalOverride: Boolean = false)
data class Iam(var name: String, var roleId: String)
class MusicSettings(var announceNewMusic: Boolean = false, var singleSongInQueueForMembers: Boolean = false, var membersCanMoveBot: Boolean = true,
                    var membersCanSkipSongs: Boolean = false, var autoQueueSongs: Boolean = false)

data class UDSearch(val tags: List<String>, val result_type: String, val list: List<UDResult>, val sounds: List<String>)

data class UDResult(val definition: String, val permalink: String, val thumbs_up: Int, val author: String, val word: String,
                    val defid: String, val current_vote: String, val example: String, val thumbs_down: Int)

data class EightBallResult(val magic: Magic)
data class Magic /* The name was not my choice...... */(val question: String, val answer: String, val type: String)

class Punishment(val userId: String, val punisherId: String, val guildId: String, val type: Type, val expiration: Long, val start: Long = System.currentTimeMillis(), val id: String = r.uuid().run(conn)) {
    enum class Type {
        TEMPBAN, MUTE
        ;

        override fun toString(): String {
            if (this == TEMPBAN) return "temp-banned"
            else return "muted"
        }
    }
}
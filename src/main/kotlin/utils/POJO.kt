package utils

import com.rethinkdb.gen.ast.Uuid
import main.conn
import main.r

data class Patron(var id: String, var donationLevel: DonationLevel)
data class SpecialPerson(var id: String, var backer: String)

class GuildData(val id: String, var prefix: String, var musicSettings: MusicSettings, var advancedPermissions: MutableList<String>, var joinMessage: Pair<String?, String? /* Message then Channel ID */>? = null, var leaveMessage: Pair<String?, String?>? = null, var defaultRole : String? = null, var allowGlobalOverride: Boolean = false)

class MusicSettings(var announceNewMusic: Boolean = false, var singleSongInQueueForMembers: Boolean = false, var membersCanMoveBot: Boolean = true,
                    var membersCanSkipSongs: Boolean = false, var autoQueueSongs: Boolean = false)

data class UDSearch(val tags: List<String>, val result_type: String, val list: List<UDResult>, val sounds: List<String>)

data class UDResult(val definition: String, val permalink: String, val thumbs_up: Int, val author: String, val word: String,
                    val defid: String, val current_vote: String, val example: String, val thumbs_down: Int)

data class EightBallResult(val magic: Magic)
data class Magic /* The name was not my choice...... */(val question: String, val answer: String, val type: String)

class Punishment(val userId: String, val punisherId: String, val guildId: String, val type: Type, val expiration: Long, val start: Long = System.currentTimeMillis(), val id : String = r.uuid().run(conn)) {
    enum class Type {
        TEMPBAN, MUTE
        ;

        override fun toString(): String {
            if (this == TEMPBAN) return "temp-banned"
            else return "muted"
        }
    }
}
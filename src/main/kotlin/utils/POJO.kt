package utils

class GuildData(val id : String, var prefix : String, var musicSettings : MusicSettings, var advancedPermissions : MutableList<String>)

class MusicSettings(var announceNewMusic : Boolean = false, var singleSongInQueueForMembers : Boolean = false, var membersCanMoveBot : Boolean = true,
                    var membersCanSkipSongs : Boolean = false)

data class UDSearch(val tags: List<String>, val result_type: String, val list: List<UDResult>, val sounds: List<String>)

data class UDResult(val definition: String, val permalink: String, val thumbs_up: Int, val author: String, val word: String,
                    val defid: String, val current_vote: String, val example: String, val thumbs_down: Int)

data class EightBallResult(val magic: Magic)
data class Magic /* The name was not my choice...... */ (val question: String, val answer: String, val type: String)
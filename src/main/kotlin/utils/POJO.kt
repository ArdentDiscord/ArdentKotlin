package utils

import main.jda

class GuildData(val id : String, var prefix : String, var musicSettings : MusicSettings, var advancedPermissions : MutableList<String>)

class MusicSettings(var announceNewMusic : Boolean = false, var singleSongInQueueForMembers : Boolean = false, var membersCanMoveBot : Boolean = true, var membersCanSkipSongs : Boolean = false)
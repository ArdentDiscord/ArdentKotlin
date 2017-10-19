package utils.discord

import main.conn
import main.r
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import translation.Language
import translation.LanguageData
import translation.toLanguage
import utils.functionality.asPojo
import utils.functionality.gson
import utils.functionality.insert
import utils.music.LocalTrackObj

fun Guild.isPatronGuild(): Boolean {
    return members.size > 300 || owner.user.getData().donationLevel != DonationLevel.NONE
}

fun Member.donationLevel(): DonationLevel {
    return if (guild.isPatronGuild()) DonationLevel.EXTREME else user.getData().donationLevel
}

data class SavedQueue(val guildId: String, val voiceChannelId: String, val tracks: List<LocalTrackObj>)

fun Guild.getData(): GuildData {
    var data = asPojo(r.table("guilds").get(id).run(conn), GuildData::class.java)
    if (data != null) return data
    data = GuildData(id, mutableListOf(), LanguageSettings(Language.ENGLISH.data.code), MusicSettings(), MessageSettings(),
            BlacklistSettings(mutableListOf(), mutableListOf(), mutableListOf()), RoleSettings())
    data.insert("guilds")
    return data
}

class GuildData(val id: String, val prefixes: MutableList<String>, val languageSettings: LanguageSettings,
                val musicSettings: MusicSettings, val messageSettings: MessageSettings,
                val blacklistSettings: BlacklistSettings, val roleSettings: RoleSettings) {
    fun update(blocking: Boolean = false) {
        if (!blocking) r.table("guilds").get(id).update(r.json(gson.toJson(this))).runNoReply(conn)
        else r.table("guilds").get(id).update(r.json(gson.toJson(this))).run<Any>(conn)
    }

}

data class LanguageSettings(var language: String, var enabled: Boolean = true) {
    fun getLanguage(): LanguageData {
        var lang = language.toLanguage()
        if (lang != null) return lang
        lang = Language.ENGLISH.data
        language = lang.code
        return lang
    }
}

data class MusicSettings(var autoplay: Boolean = false, var stayInChannel: Boolean = false, var whitelistedRoles: MutableList<String>? = null,
                         var canEveryoneUseAdminCommands: Boolean = false, var whitelistedRolesForAdminCommands: MutableList<String>? = null)

data class MessageSettings(var joinMessage: JoinMessage? = null, var leaveMessage: LeaveMessage? = null,
                           var messageUsersOnJoin: Boolean = false)

data class JoinMessage(var message: String, var lastEditedBy: String, var lastEditedAt: Long,
                       var creator: String, var channel: String, var enabled: Boolean = true)

data class LeaveMessage(var message: String, var lastEditedBy: String, var lastEditedAt: Long,
                        var creator: String, var channel: String, var enabled: Boolean = true)

data class BlacklistSettings(var blacklistedChannels: MutableList<String>, var blacklistedUsers: MutableList<String>,
                             val blacklistedVoiceChannels: MutableList<String>)

data class RoleSettings(var defaultRole: String? = null, var autoroles: MutableList<Autorole> = mutableListOf())
data class Autorole(var name: String, var role: String, val creator: String)
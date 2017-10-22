package utils.discord

import main.conn
import main.r
import main.shards
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import translation.Language
import translation.LanguageData
import translation.toLanguage
import utils.functionality.asPojo
import utils.functionality.gson
import utils.functionality.insert

fun Guild.getShard(): Int {
    return ((id.toLong() shr 22) % shards).toInt()
}

fun Guild.isPatronGuild(): Boolean {
    return members.size > 300 || getPatronLevel(owner.user.id)?.level?.compareTo(1) ?: -1 > 0
}

fun Member.donationLevel(): PatronLevel? {
    return if (guild.isPatronGuild()) PatronLevel.PREMIUM else getPatronLevel(user.id)
}

fun Guild.getData(): GuildData {
    var data = asPojo(r.table("guilds").get(id).run(conn), GuildData::class.java)
    if (data != null) return data
    data = GuildData(id, PrefixSettings(), LanguageSettings(Language.ENGLISH.data.code), MusicSettings(), MessageSettings(), BlacklistSettings(), RoleSettings())
    data.insert("guilds")
    return data
}

class GuildData(val id: String, val prefixSettings: PrefixSettings, val languageSettings: LanguageSettings,
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

data class PrefixSettings(val prefixes: MutableList<String> = mutableListOf(), var disabledDefaultPrefix: Boolean = false)

data class MusicSettings(var autoplay: Boolean = false, var stayInChannel: Boolean = false, var whitelistedRoles: MutableList<String>? = null,
                         var canEveryoneUseAdminCommands: Boolean = false, var whitelistedRolesForAdminCommands: MutableList<String>? = null)

data class MessageSettings(var joinMessage: JoinMessage? = null, var leaveMessage: LeaveMessage? = null,
                           var messageUsersOnJoin: Boolean = false)

data class JoinMessage(var message: String, var lastEditedBy: String, var lastEditedAt: Long,
                       var creator: String, var channel: String, var enabled: Boolean = true)

data class LeaveMessage(var message: String, var lastEditedBy: String, var lastEditedAt: Long,
                        var creator: String, var channel: String, var enabled: Boolean = true)

data class BlacklistSettings(val blacklistedChannels: MutableList<String> = mutableListOf(), val blacklistedUsers: MutableList<String> = mutableListOf(),
                             val blacklistedVoiceChannels: MutableList<String> = mutableListOf(), val blacklistedRoles: MutableList<String> = mutableListOf() )

data class RoleSettings(var defaultRole: String? = null, var autoroles: MutableList<Autorole> = mutableListOf())

/**
 * When [whitelistedRoles] is empty, all will be able to use this autorole
 */
data class Autorole(var name: String, var role: String, val creator: String, val whitelistedRoles: MutableList<String> = mutableListOf())
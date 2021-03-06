package utils.discord

import main.conn
import main.jdas
import main.r
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.VoiceChannel
import utils.functionality.concat
import utils.functionality.queryAsArrayList
import utils.functionality.stringify

// Guilds
fun getGuildById(id: String): Guild? {
    jdas.forEach { jda ->
        try {
            val guild = jda.getGuildById(id)
            if (guild != null) return guild
        } catch (ignored: Exception) {
        }
    }
    return null
}

fun getGuildsByName(name: String, ignoreCase: Boolean = true): MutableList<Guild> {
    val guilds = mutableListOf<Guild>()
    jdas.forEach { guilds.addAll(it.getGuildsByName(name, ignoreCase)) }
    return guilds
}

// Text Channels
fun getTextChannelById(id: String?): TextChannel? {
    if (id == null) return null
    jdas.forEach { jda ->
        val channel = jda.getTextChannelById(id)
        if (channel != null) return channel
    }
    return null
}

// Voice Channels
fun getVoiceChannelById(id: String): VoiceChannel? {
    jdas.forEach { jda ->
        try {
            val voice = jda.getVoiceChannelById(id)
            if (voice != null) return voice
        } catch (ignored: Exception) {
        }
    }
    return null
}

// Users

fun getUserById(id: String?): User? {
    if (id == null) return null
    jdas.forEach { jda ->
        try {
            val user = jda.getUserById(id)
            if (user != null) return user
        } catch (ignored: Exception) {
        }
    }
    return null
}

fun String.toUser() = getUserById(this)

fun List<String>.toUsers(): String {
    return map { getUserById(it)?.toFancyString() ?: "Unknown" }.stringify()
}

// Roles
fun Message.getFirstRole(arguments: List<String>): Role? {
    if (arguments.isEmpty()) return null
    if (mentionedRoles.size > 0) return mentionedRoles[0]
    if (guild != null) {
        val search = guild.getRolesByName(arguments.concat(), true)
        if (search.size > 0) return search[0]
    }
    return null
}

fun getDerogatoryTerms(): MutableList<String> {
    return r.table("derogatoryTerms").run<Any>(conn).queryAsArrayList(DerogatoryTerm::class.java).map { it!!.term }.toMutableList()
}

data class DerogatoryTerm(var term: String)
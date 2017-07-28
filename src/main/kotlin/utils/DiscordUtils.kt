package utils

import net.dv8tion.jda.core.exceptions.PermissionException
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.vdurmont.emoji.EmojiParser
import commands.games.CoinflipPlayerData
import commands.games.GameDataCoinflip
import commands.music.getGuildAudioPlayer
import commands.games.TriviaPlayerData
import main.conn
import main.jda
import main.r
import main.version
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import java.util.HashMap

fun String.toChannel(): TextChannel? {
    return jda?.getTextChannelById(this)
}

fun AudioPlayer.currentlyPlaying(channel: TextChannel): Boolean {
    if (playingTrack != null && channel.guild.getGuildAudioPlayer(channel).scheduler.manager.current != null) return true
    channel.send(channel.guild.selfMember, "${Emoji.HEAVY_MULTIPLICATION_X} There isn't a currently playing track!")
    return false
}

fun Member.voiceChannel() : VoiceChannel? {
    return voiceState.channel
}

fun Member.hasOverride(channel: TextChannel, ifAloneInVoice : Boolean): Boolean {
    if (hasOverride() || (ifAloneInVoice && voiceChannel() != null && voiceChannel()!!.members.size == 1 && voiceChannel()!!.members[0] == this)) return true
    channel.send(this, "${Emoji.NEGATIVE_SQUARED_CROSSMARK} You need to be given advanced permissions or the `Manage Server` permission to use this!")
    return false
}

fun Member.hasOverride(): Boolean {
    return hasPermission(Permission.MANAGE_CHANNEL) || guild.getData().advancedPermissions.contains(user.id)
}

fun String.getChannel(): TextChannel? {
    return jda!!.getTextChannelById(this)
}

fun Member.withDiscrim(): String {
    return this.user.withDiscrim()
}

fun User.withDiscrim(): String {
    return "$name#$discriminator"
}


fun embed(title: String, member: Member, color: Color = Color.MAGENTA): EmbedBuilder {
    return EmbedBuilder().setAuthor(title, "https://ardentbot.com", member.guild.iconUrl)
            .setColor(color)
            .setFooter("Served by Ardent ${Emoji.COPYRIGHT_SIGN} Adam#9261", member.user.avatarUrl)
}

fun String.toUser(): User? {
    return jda?.getUserById(this)
}

fun List<String>.toUsers() : String {
    return map { it.toUser()!!.withDiscrim() }.stringify()
}

fun Guild.getData(): GuildData {
    val guildData: GuildData? = asPojo(r.table("guilds").get(this.id).run(conn), GuildData::class.java)
    if (guildData != null) return guildData
    val data = GuildData(id, "/", MusicSettings(true, true), mutableListOf<String>())
    data.insert("guilds")
    return data
}

fun Message.getFirstRole(arguments: List<String>) : Role? {
    if (mentionedRoles.size > 0) return mentionedRoles[0]
    if (guild != null) {
        val search = guild.getRolesByName(arguments.concat(), true)
        if (search.size > 0) return search[0]
    }
    return null
}

fun MessageChannel.sendReceive(member: Member, embed: EmbedBuilder): Message? {
    try {
        return this.sendMessage(embed.build()).complete()
    } catch (ex: PermissionException) {
        sendFailed(member.user, false)
    }
    return null
}

fun MessageChannel.sendReceive(member: Member, message: String): Message? {
    try {
        return this.sendMessage(message).complete()
    } catch (ex: PermissionException) {
        sendFailed(member.user, false)
    }
    return null
}

fun TextChannel.send(member: Member, embedBuilder: EmbedBuilder) {
    send(member.user, embedBuilder)
}

fun TextChannel.send(user: User, embedBuilder: EmbedBuilder) {
    try {
        sendMessage(embedBuilder.build()).queue()
    } catch (e: Exception) {
        sendFailed(user, true)
    }
}

fun MessageChannel.send(member: Member, message: String) {
    this.send(member.user, message)
}

fun MessageChannel.send(user: User, message: String) {
    try {
        if (message.length <= 2000) {
            this.sendMessage(message).queue()
            return
        }
        var i = 0
        while (i < message.length) {
            if (i + 2000 <= message.length) {
                this.sendMessage(message.substring(i, i + 2000)).queue()
            } else {
                this.sendMessage(message.substring(i, message.length - 1)).queue()
            }
            i += 2000
        }
    } catch (ex: PermissionException) {
        sendFailed(user, false)
    }
}

fun sendEmbed(embedBuilder: EmbedBuilder, channel: TextChannel, user: User, vararg reactions: String): Message {
    try {
        val message = channel.sendMessage(embedBuilder.build()).complete()
        for (reaction in reactions) {
            message.addReaction(EmojiParser.parseToUnicode(reaction)).queue()
        }
        return message
    } catch (ex: PermissionException) {
        sendFailed(user, true)
    }
    return MessageBuilder().build()
}

fun sendFailed(user: User, embed: Boolean) {
    user.openPrivateChannel().queue { privateChannel ->
        try {
            if (!embed) {
                privateChannel.sendMessage("I don't have permission to type in this channel!").queue()
            } else {
                privateChannel.sendMessage("I don't have permission to send embeds in this channel!").queue()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun Guild.getPrefix(): String {
    return "/"
}

enum class DonationLevel(val readable: String, val level: Int) {
    NONE("None", 1), PATRON("Patron", 2), PATRON_PLUS("Patron+", 3), OG("OG", 4);

    override fun toString(): String {
        return readable
    }
}

fun Guild.isPatronGuild(): Boolean {
    return donationLevel() != DonationLevel.NONE
}

fun Guild.donationLevel(): DonationLevel {
    return owner.user.donationLevel()
}

fun User.getData(): PlayerData {
    var data: Any? = r.table("playerData").get(id).run(conn)
    if (data != null) return asPojo(data as HashMap<*, *>?, PlayerData::class.java)!!
    data = PlayerData(id, DonationLevel.NONE)
    data.insert("playerData")
    return data
}

fun Member.id() : String {
    return user.id
}

fun Member.isPatron(): Boolean {
   return true
   // TODO() return user.donationLevel() != DonationLevel.NONE
}

fun User.donationLevel(): DonationLevel {
    return DonationLevel.OG
    // TODO() return getData().donationLevel
}

fun Member.hasDonationLevel(channel: TextChannel, donationLevel: DonationLevel): Boolean {
    if (user.donationLevel().level >= donationLevel.level || guild.donationLevel().level >= donationLevel.level) return true
    channel.requires(this, donationLevel)
    return false
}

fun TextChannel.requires(member: Member, requiredLevel: DonationLevel) {
    send(member, "${Emoji.CROSS_MARK} This command requires that you or this server have a donation level of **${requiredLevel.readable}** to be able to use it")
}

class PlayerData(val id: String, var donationLevel: DonationLevel, var gold: Double = 0.0) {
    fun coinflipData() : CoinflipPlayerData {
        val data = CoinflipPlayerData()
        val coinflipGames = r.table("CoinflipData").run<Any>(conn).queryAsArrayList(GameDataCoinflip::class.java)
        coinflipGames.forEach { game : GameDataCoinflip? ->
            if (game != null) {
                if (game.contains(id)) {
                    if (game.winner == id) data.wins++
                    else data.losses++
                    game.rounds.forEach { round ->
                        if (round.winners.contains(id)) data.roundsWon++
                        else data.roundsLost++
                    }
                }
            }
        }
        return data
    }
}

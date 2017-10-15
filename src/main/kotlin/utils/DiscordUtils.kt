package utils

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import commands.administrate.Staff
import commands.administrate.staff
import commands.games.questions
import commands.music.getGuildAudioPlayer
import main.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.*
import java.awt.Color
import java.util.*

var internals = Internals()

fun AudioPlayer.currentlyPlaying(channel: TextChannel): Boolean {
    if (playingTrack != null && channel.guild.getGuildAudioPlayer(channel).scheduler.manager.current != null) return true
    channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "There isn't a currently playing track!".tr(channel.guild))
    return false
}

fun Member.data(): PlayerData {
    return user.getData()
}

fun Guild.playerDatas(): MutableList<PlayerData> {
    val data = mutableListOf<PlayerData>()
    val ids = members.map { it.id() }
    r.table("playerData").run<Any>(conn).queryAsArrayList(PlayerData::class.java).forEach {
        if (it != null) {
            if (ids.contains(it.id)) data.add(it)
        }
    }
    return data
}

fun Member.voiceChannel(): VoiceChannel? {
    return voiceState.channel
}

fun Member.hasRole(vararg searchRoles: String): Boolean {
    roles.forEach { memberRole -> if (searchRoles.contains(memberRole.id)) return true }
    return false
}

fun String.toRole(guild: Guild): Role? {
    return try {
        guild.getRoleById(this)
    } catch (e: Exception) {
        null
    }
}

fun Member.isStaff(): Boolean {
    return user.isStaff()
}

fun User.isStaff(): Boolean {
    return staff.map { it.id }.contains(id)
}

fun getAnnouncements(): MutableList<Announcement> {
    return r.table("announcements").run<Any>(conn).queryAsArrayList(AnnouncementModel::class.java).map { it!!.toAnnouncement() }.toMutableList()
}

fun Member.hasOverride(channel: TextChannel, ifAloneInVoice: Boolean = false, failQuietly: Boolean = false, djCommand: Boolean = false): Boolean {
    val data = guild.getData()
    if (staff.map { it.id }.contains(id()) || data.advancedPermissions.contains(id()) || hasOverride() || (ifAloneInVoice && voiceChannel() != null && voiceChannel()!!.members.size == 2 && voiceChannel()!!.members.contains(this)) || (djCommand && data.allowGlobalOverride)) return true
    if (djCommand) {
        val track = guild.getGuildAudioPlayer(channel).scheduler.manager.current
        if (track != null && track.author == id()) return true
    }
    if (!failQuietly) channel.send("${Emoji.NEGATIVE_SQUARED_CROSSMARK} " + "You need to be given advanced permissions or the `Manage Server` permission to use this!".tr(guild))
    return false
}

private fun Member.hasOverride(): Boolean {
    return isOwner || hasPermission(Permission.ADMINISTRATOR) || hasPermission(Permission.MANAGE_CHANNEL)
}

fun User.withDiscrim(): String {
    return "$name#$discriminator"
}

fun Member.embed(title: String, color: Color = Color.DARK_GRAY): EmbedBuilder {
    return EmbedBuilder().setAuthor(title, "https://ardentbot.com", guild.iconUrl)
            .setColor(if (color != Color.DARK_GRAY) color else
                when (random.nextBoolean()) {
                    true -> Color.BLUE; else -> Color.DARK_GRAY;
                })
            .setFooter("Served by Ardent {0} | By {1} and {2}".tr(guild, Emoji.COPYRIGHT_SIGN.symbol, "Adam#9261", "Kotlin", user.avatarUrl))
}


fun guilds(): ArrayList<Guild> {
    val guilds = arrayListOf<Guild>()
    jdas.forEach { guilds.addAll(it.guilds) }
    return guilds
}

fun users(): MutableList<User> {
    val users = hashSetOf<User>()
    jdas.forEach { users.addAll(it.users) }
    return users.toMutableList()
}

fun Guild.getShard(): Int {
    return ((id.toLong() shr 22) % shards).toInt()
}

fun Guild.getData(): GuildData {
    try {
        val guildData = asPojo(r.table("guilds").get(this.id).run(conn), GuildData::class.java)
        if (guildData != null) {
            if (guildData.blacklistedUsers == null || guildData.blacklistedRoles == null || guildData.blacklistedChannels == null) {
                guildData.blacklistedUsers = if (guildData.blacklistedUsers == null) mutableListOf() else guildData.blacklistedUsers
                guildData.blacklistedRoles = if (guildData.blacklistedRoles == null) mutableListOf() else guildData.blacklistedRoles
                guildData.blacklistedChannels = if (guildData.blacklistedChannels == null) mutableListOf() else guildData.blacklistedChannels
                guildData.update()
            }
            if (guildData.musicSettings.stayInChannel == null) {
                guildData.musicSettings.stayInChannel = false
                guildData.update()
            }
            return guildData
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val data = GuildData(id, "/", MusicSettings(false, false), mutableListOf(), languageData = "en".toLanguage()!!,
            blacklistedUsers = mutableListOf(), blacklistedRoles = mutableListOf(), blacklistedChannels = mutableListOf())
    data.insert("guilds")
    return data
}


fun Guild?.punishments(): MutableList<Punishment?> {
    if (this == null) return mutableListOf()
    return r.table("punishments").filter(r.hashMap("guildId", id)).run<Any>(conn).queryAsArrayList(Punishment::class.java)
}

fun Member.punishments(): MutableList<Punishment?> {
    val punishments = r.table("punishments").filter(r.hashMap("guildId", guild.id).with("userId", user.id))
            .run<Any>(conn).queryAsArrayList(Punishment::class.java)
    val iterator = punishments.iterator()
    while (iterator.hasNext()) {
        if (iterator.next() == null) iterator.remove()
    }
    return punishments
}

fun Guild.getDefaultWritingChannel(): TextChannel? {
    textChannels.forEach { if (it.canTalk()) return it }
    return null
}

fun Guild.getPrefix(): String {
    return getData().prefix ?: "/"
}

fun Guild.getLanguage(): LanguageData {
    val data = getData()
    val language = data.languageData
    return if (language != null) language else {
        data.languageData = Language.ENGLISH.data
        data.update()
        Language.ENGLISH.data
    }
}

fun Guild.botSize(): Int {
    var counter = 0
    members.forEach { if (it.user.isBot) counter++ }
    return counter
}

fun Guild.isPatronGuild(): Boolean {
    return members.size > 300 || donationLevel() != DonationLevel.NONE
}

fun Guild.donationLevel(): DonationLevel {
    return owner.user.donationLevel()
}

fun User.getData(): PlayerData {
    val data = r.table("playerData").get(id).run<HashMap<*, *>>(conn)
    if (data != null) {
        val pd = asPojo(data, PlayerData::class.java)
        if (pd != null) {
            if (isStaff()) pd.donationLevel = DonationLevel.EXTREME
            return pd
        }
    }
    val player = PlayerData(id, DonationLevel.NONE)
    player.insert("playerData")
    return player
}

fun Member.id(): String {
    return user.id
}

fun User.isPatron(): Boolean {
    return donationLevel() != DonationLevel.NONE
}

fun Member.isPatron(): Boolean {
    return user.isPatron()
}

fun User.donationLevel(): DonationLevel {
    staff.forEach { if (it.id == id) return DonationLevel.EXTREME }
    val special = asPojo(r.table("specialPeople").get(id).run(conn), SpecialPerson::class.java)
    if (special != null) return DonationLevel.EXTREME
    return asPojo(r.table("patrons").get(id).run(conn), Patron::class.java)?.donationLevel ?: DonationLevel.NONE
}

fun Member.hasDonationLevel(channel: TextChannel, donationLevel: DonationLevel, failQuietly: Boolean = false): Boolean {
    if (usageBonus() || guild.members.size > 300 || user.donationLevel().level >= donationLevel.level || (guild.donationLevel().level >= donationLevel.level && hasOverride(channel, true, true, false))) return true
    return if (!failQuietly) channel.requires(this, donationLevel) else true
}

fun Int.getTrivia(): List<TriviaQuestion> {
    val list = mutableListOf<TriviaQuestion>()
    val random = Random()
    while (list.size < this) {
        val q = questions[random.nextInt(questions.size)]
        if (!list.contains(q)) list.add(q)
    }
    return list
}

fun Member.isWhitelisted(): Boolean {
    return asPojo(r.table("specialPeople").get(id()).run(conn), SpecialPerson::class.java) != null
}

fun usageBonus(): Boolean {
    return false // for implementation at a later date
}

fun MessageChannel.requires(member: Member, requiredLevel: DonationLevel): Boolean {
    if (usageBonus() || member.isPatron() || member.isStaff() || member.isWhitelisted() || member.guild.isPatronGuild()) return true
    else send("${Emoji.CROSS_MARK} " + "This command requires that you or this server have a donation level of **{0}** to be able to use it".tr(member.guild, requiredLevel.readable.tr(member.guild)))
    return false
}

fun MessageReceivedEvent.isAdministrator(complain: Boolean): Boolean {
    return author.isAdministrator(textChannel, complain)
}

fun User.isAdministrator(channel: TextChannel, complain: Boolean = false): Boolean {
    staff.forEach { if (it.id == id && it.role == Staff.StaffRole.ADMINISTRATOR) return true }
    if (complain) channel.send("You need to be an **Ardent Administrator** to use this command")
    return false
}

fun getMutualGuildsWith(user: User): MutableList<Guild> {
    val servers = mutableListOf<Guild>()
    jdas.forEach { servers.addAll(it.getMutualGuilds(user)) }
    return servers
}

fun String.tr(languageData: LanguageData, vararg new: Any): String {
    return languageData.translate(this)?.trReplace(languageData, *new) ?: translationDoesntExist(languageData, *new)
}

fun String.translationDoesntExist(languageData: LanguageData, vararg new: Any): String {
    if (!test) {
        val phrase = ArdentPhraseTranslation(this, "Unknown")
        translationData.phrases.put(this, phrase)
        if (r.table("phrases").filter(r.hashMap("english", this)).count().run<Long>(conn) == 0.toLong()) {
            phrase.insert("phrases")
            logChannel!!.send("```Translation for the following doesn't exist and was automatically inserted into the database: $this```")
            "355817985052508160".toChannel()?.send("A new phrase was automatically detected and added at <https://ardentbot.com/translation/>")
        }
    }
    return this.trReplace(languageData, *new)
}

fun String.tr(messageReceivedEvent: MessageReceivedEvent, vararg new: Any): String {
    return tr(messageReceivedEvent.guild, *new)
}

fun String.tr(textChannel: TextChannel, vararg new: Any): String {
    return tr(textChannel.guild.getLanguage(), new)
}

fun String.tr(guild: Guild, vararg new: Any): String {
    return tr(guild.getLanguage(), *new)
}

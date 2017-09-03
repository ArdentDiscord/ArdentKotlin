package utils

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.vdurmont.emoji.EmojiParser
import commands.administrate.staff
import commands.games.*
import commands.music.getGuildAudioPlayer
import main.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.exceptions.PermissionException
import java.awt.Color
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.TimeUnit

var internals = Internals()

data class SanitizedTriviaRound(val hasWinner: Boolean, val winner: User?, val losers: List<User?>, val question: TriviaQuestion)
data class SanitizedTrivia(val creator: User?, val id: Long?, val winner: User?, val losers: List<User?>, val scores: List<Pair<String, Int>>, val rounds: List<SanitizedTriviaRound>)

data class SanitizedGame(val user: String, val endTime: String, val type: String, val url: String)

fun String.toChannel(): TextChannel? {
    jdas.forEach { jda ->
        val channel = jda.getTextChannelById(this)
        if (channel != null) return channel
    }
    return null
}

fun AudioPlayer.currentlyPlaying(channel: TextChannel): Boolean {
    if (playingTrack != null && channel.guild.getGuildAudioPlayer(channel).scheduler.manager.current != null) return true
    channel.send("${Emoji.HEAVY_MULTIPLICATION_X} There isn't a currently playing track!")
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

fun String.toRole(guild: Guild): Role? {
    try {
        return guild.getRoleById(this)
    } catch (e: Exception) {
        return null
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
    if (!failQuietly) channel.send("${Emoji.NEGATIVE_SQUARED_CROSSMARK} You need to be given advanced permissions or the `Manage Server` permission to use this!")
    return false
}

private fun Member.hasOverride(): Boolean {
    return isOwner || hasPermission(Permission.ADMINISTRATOR) || hasPermission(Permission.MANAGE_CHANNEL)
}

fun User.getMarriage(): User? {
    return getMarriageModeled()?.second
}

fun User.getMarriageModeled(): Pair<Marriage, User?>? {
    r.table("marriages").run<Any>(conn).queryAsArrayList(Marriage::class.java).forEach { marriage ->
        if (marriage != null) {
            if (marriage.userOne == id || marriage.userTwo == id) {
                val spouse: User? = if (marriage.userOne == id) marriage.userTwo.toUser() else marriage.userOne.toUser()
                if (spouse == null) r.table("marriages").get(marriage.id).delete().runNoReply(conn)
                return Pair(marriage, spouse)
            }
        }
    }
    return null
}

fun Member.withDiscrim(): String {
    return this.user.withDiscrim()
}

fun User.withDiscrim(): String {
    return "$name#$discriminator"
}


fun Member.embed(title: String, color: Color = Color.DARK_GRAY): EmbedBuilder {
    return EmbedBuilder().setAuthor(title, "https://ardentbot.com", guild.iconUrl)
            .setColor(color)
            .setFooter("Served by Ardent ${Emoji.COPYRIGHT_SIGN} Adam#9261", user.avatarUrl)
}

fun String.toUser(): User? {
    jdas.forEach { jda ->
        try {
            val user = jda.getUserById(this)
            if (user != null) return user
        } catch (ignored: Exception) {
        }
    }
    return null
}

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

fun getUserById(id: String?): User? {
    return id?.toUser()
}

fun guilds(): ArrayList<Guild> {
    val guilds = arrayListOf<Guild>()
    jdas.forEach { guilds.addAll(it.guilds) }
    return guilds
}

fun users(): ArrayList<User> {
    val users = arrayListOf<User>()
    jdas.forEach { users.addAll(it.users) }
    return users
}

fun List<String>.toUsers(): String {
    return map { it.toUser()!!.withDiscrim() }.stringify()
}

fun Guild.getData(): GuildData {
    val guildData: GuildData? = asPojo(r.table("guilds").get(this.id).run(conn), GuildData::class.java)
    return if (guildData != null) guildData
    else {
        val data = GuildData(id, "/", MusicSettings(false, false), mutableListOf<String>())
        data.insert("guilds")
        data
    }
}

fun Message.getFirstRole(arguments: List<String>): Role? {
    if (mentionedRoles.size > 0) return mentionedRoles[0]
    if (guild != null) {
        val search = guild.getRolesByName(arguments.concat(), true)
        if (search.size > 0) return search[0]
    }
    return null
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

@Deprecated("Inefficient - Use consumer rather than blocking logic")
fun MessageChannel.sendReceive(embed: EmbedBuilder): Message? {
    try {
        return this.sendMessage(embed.build()).complete()
    } catch (ex: Throwable) {
    }
    return null
}

@Deprecated("Inefficient - Use consumer rather than blocking logic")
fun MessageChannel.sendReceive(message: String): Message? {
    try {
        return this.sendMessage(message).complete()
    } catch (ex: PermissionException) {
    }
    return null
}

fun User.whitelisted(): List<SpecialPerson?> {
    return r.table("specialPeople").run<Any>(conn).queryAsArrayList(SpecialPerson::class.java).filter { it != null && it.backer == id }
}

fun MessageChannel.send(embedBuilder: EmbedBuilder) {
    sendEmbed(embedBuilder)
}

fun MessageChannel.send(message: String) {
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
    }
}

fun MessageChannel.sendEmbed(embedBuilder: EmbedBuilder, vararg reactions: String): Message? {
    try {
        val message = sendMessage(embedBuilder.build()).complete()
        for (reaction in reactions) {
            message.addReaction(EmojiParser.parseToUnicode(reaction)).queue()
        }
        return message
    } catch (ex: PermissionException) {
    }
    return null
}

fun Guild.getPrefix(): String {
    return getData().prefix ?: "/"
}

enum class DonationLevel(val readable: String, val level: Int) {
    NONE("None", 1), SUPPORTER("Supporter", 2), BASIC("Basic", 3), INTERMEDIATE("Intermediate", 4), EXTREME("Extreme", 5);

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
    if (data != null) {
        val pd = asPojo(data as HashMap<*, *>?, PlayerData::class.java)!!
        if (isStaff()) pd.donationLevel = DonationLevel.EXTREME
        return pd
    }
    data = PlayerData(id, DonationLevel.NONE)
    data.insert("playerData")
    return data
}

fun Member.id(): String {
    return user.id
}

fun Member.isPatron(): Boolean {
    return user.donationLevel() != DonationLevel.NONE
}

fun User.donationLevel(): DonationLevel {
    staff.forEach { if (it.id == id) return DonationLevel.EXTREME }
    val special = asPojo(r.table("specialPeople").get(id).run(conn), SpecialPerson::class.java)
    if (special != null) return DonationLevel.EXTREME
    return asPojo(r.table("patrons").get(id).run(conn), Patron::class.java)?.donationLevel ?: DonationLevel.NONE
}

fun Member.hasDonationLevel(channel: MessageChannel, donationLevel: DonationLevel, failQuietly: Boolean = false): Boolean {
    if (user.donationLevel().level >= donationLevel.level || guild.donationLevel().level >= donationLevel.level) return true
    if (!failQuietly) channel.requires(this, donationLevel)
    return false
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

fun MessageChannel.requires(member: Member, requiredLevel: DonationLevel): Boolean {
    if (member.isPatron() || member.isStaff() || member.isWhitelisted() || member.guild.isPatronGuild()) return true
    else send("${Emoji.CROSS_MARK} This command requires that you or this server have a donation level of **${requiredLevel.readable}** to be able to use it")
    return false
}

fun getMutualGuildsWith(user: User): MutableList<Guild> {
    val servers = mutableListOf<Guild>()
    jdas.forEach { servers.addAll(it.getMutualGuilds(user)) }
    return servers
}

data class LoggedCommand(val commandId: String, val userId: String, val executionTime: Long, val readableExecutionTime: String, val id: String = r.uuid().run(conn))

class PlayerData(val id: String, var donationLevel: DonationLevel, var gold: Double = 50.0, var collected: Long = 0, val reminders: MutableList<Reminder> = mutableListOf()) {
    fun canCollect(): Boolean {
        return ((System.currentTimeMillis() - collected) / 1000 / 60 / 24) >= 1
    }

    fun collectionTime(): String {
        return (collected + TimeUnit.DAYS.toMillis(1)).readableDate()
    }

    fun collect(): Int {
        val amount = random.nextInt(500) + 1
        gold += amount
        collected = System.currentTimeMillis() + (1000 * 60 * 24)
        update()
        return amount
    }

    fun blackjackData(): BlackjackPlayerData {
        val data = BlackjackPlayerData()
        r.table("BlackjackData").run<Any>(conn).queryAsArrayList(GameDataBlackjack::class.java).forEach { game ->
            if (game != null && game.creator == id) {
                game.rounds.forEach { round ->
                    when (round.won) {
                        BlackjackGame.Result.TIED -> data.ties++
                        BlackjackGame.Result.WON -> data.wins++
                        BlackjackGame.Result.LOST -> data.losses++
                    }
                }
            }
        }
        return data
    }

    fun bettingData(): BettingPlayerData {
        val data = BettingPlayerData()
        r.table("BettingData").run<Any>(conn).queryAsArrayList(GameDataBetting::class.java).forEach { game ->
            if (game != null && game.creator == id) {
                game.rounds.forEach { round ->
                    if (round.won) {
                        data.wins++
                        data.netWinnings += round.betAmount
                    } else {
                        data.losses++
                        data.netWinnings -= round.betAmount
                    }
                }
            }
        }
        return data
    }

    fun triviaData(): TriviaPlayerData {
        val correctByCategory = hashMapOf<String, Pair<Int, Int>>()
        val data = TriviaPlayerData()
        r.table("TriviaData").run<Any>(conn).queryAsArrayList(GameDataTrivia::class.java).forEach { game ->
            if (game != null && (game.winner == id || game.losers.contains(id))) {
                if (game.winner == id) data.wins++
                else data.losses++
                game.rounds.forEach { round ->
                    val currentQuestion = round.question
                    if (!correctByCategory.containsKey(currentQuestion.category)) correctByCategory.put(currentQuestion.category, Pair(0, 0))
                    if (round.winners.contains(id)) {
                        data.questionsCorrect++
                        correctByCategory.replace(currentQuestion.category, Pair(correctByCategory[currentQuestion.category]!!.first + 1, correctByCategory[currentQuestion.category]!!.second))
                    } else {
                        data.questionsWrong++
                        correctByCategory.replace(currentQuestion.category, Pair(correctByCategory[currentQuestion.category]!!.first, correctByCategory[currentQuestion.category]!!.second + 1))
                    }
                }
            }
        }
        correctByCategory.forEach { category, (first, second) -> data.percentageCorrect.put(category, first.toDouble() / (first + second).toDouble() * 100) }
        data.overallCorrectPercent = (data.questionsCorrect.toDouble() / (data.questionsCorrect + data.questionsWrong).toDouble()) * 100.0
        return data
    }
}

class Internals {
    var messagesReceived: Long = 0
    var commandsReceived: Long = 0
    var commandCount: Int = 0
    var commandDistribution: HashMap<String, Int> = hashMapOf()
    var guilds: Int = 0
    var users: Int = 0
    var cpuUsage: Double = 0.0
    var ramUsage: Pair<Long /* Used RAM in MB */, Long /* Available RAM in MB */> = Pair(0, 0)
    var roleCount: Long = 0
    var channelCount: Long = 0
    var voiceCount: Long = 0
    var loadedMusicPlayers: Int = 0
    var queueLength: Int = 0
    var uptime: Long = 0
    var uptimeFancy: String = ""
    var apiCalls: Long = 0

    init {
        waiterExecutor.scheduleAtFixedRate({
            loadedMusicPlayers = 0
            queueLength = 0
            messagesReceived = factory.messagesReceived.get()
            commandsReceived = factory.commandsReceived().toLong()
            commandCount = factory.commands.size
            commandDistribution = factory.commandsById
            guilds = utils.guilds().size
            users = users().size
            cpuUsage = getProcessCpuLoad()
            val totalRam = Runtime.getRuntime().totalMemory() / 1024 / 1024
            ramUsage = Pair(totalRam - Runtime.getRuntime().freeMemory() / 1024 / 1024, totalRam)
            guilds().forEach { guild ->
                roleCount += guild.roles.size
                channelCount += guild.textChannels.size
                voiceCount += guild.voiceChannels.size
            }
            managers.forEach { _, u ->
                queueLength += u.scheduler.manager.queue.size
                if (u.player.playingTrack != null) {
                    queueLength++
                    loadedMusicPlayers++
                }
            }
            jdas.forEach { apiCalls += it.responseTotal }
            uptime = ManagementFactory.getRuntimeMXBean().uptime
            val seconds = (uptime / 1000) % 60
            val minutes = (uptime / (1000 * 60)) % 60
            val hours = (uptime / (1000 * 60 * 60)) % 24
            val days = (uptime / (1000 * 60 * 60 * 24))
            val builder = StringBuilder()
            if (days == 1.toLong()) builder.append("$days day, ")
            else if (days > 1.toLong()) builder.append("$days days, ")

            if (hours == 1.toLong()) builder.append("$hours hour, ")
            else if (hours > 1.toLong()) builder.append("$hours hours, ")

            if (minutes == 1.toLong()) builder.append("$minutes minute, ")
            else if (minutes > 1.toLong()) builder.append("$minutes minutes, ")

            if (seconds == 1.toLong()) builder.append("$minutes second")
            else builder.append("$seconds seconds")
            uptimeFancy = builder.toString()
        }, 0, 5, TimeUnit.SECONDS)
    }
}
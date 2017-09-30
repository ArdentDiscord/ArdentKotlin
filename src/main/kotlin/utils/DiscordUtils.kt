package utils

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.vdurmont.emoji.EmojiParser
import commands.administrate.Staff
import commands.administrate.staff
import commands.games.*
import commands.music.getGuildAudioPlayer
import main.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.*
import java.awt.Color
import java.lang.management.ManagementFactory
import java.math.BigInteger
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
            .setFooter("Served by Ardent {0} | By {1} and {2}".tr(guild, Emoji.COPYRIGHT_SIGN.symbol, getUserById("169904324980244480")?.withDiscrim() ?: "Unknown", getUserById("188505107057475585")!!.withDiscrim()), user.avatarUrl)
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

fun users(): MutableList<User> {
    val users = hashSetOf<User>()
    jdas.forEach { users.addAll(it.users) }
    return users.toMutableList()
}

fun List<String>.toUsers(): String {
    return map { it.toUser()?.withDiscrim() ?: "Unknown" }.stringify()
}

fun Guild.getData(): GuildData {
    val guildData: GuildData? = asPojo(r.table("guilds").get(this.id).run(conn), GuildData::class.java)
    return if (guildData != null) guildData
    else {
        val data = GuildData(id, "/", MusicSettings(false, false), mutableListOf<String>(), language = "en".toLanguage()!!)
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
    } catch (ex: Exception) {
    }
}

fun MessageChannel.sendEmbed(embedBuilder: EmbedBuilder, vararg reactions: String): Message? {
    try {
        if (embedBuilder.descriptionBuilder.length < 2048) {
            val message = sendMessage(embedBuilder.build()).complete()
            for (reaction in reactions) {
                message.addReaction(EmojiParser.parseToUnicode(reaction)).queue()
            }
            return message
        } else {
            val description = embedBuilder.descriptionBuilder.toString()
            embedBuilder.setDescription("")
            val builtEmbed = embedBuilder.build()
            for (i in 0..Math.ceil(description.length / 2048.toDouble()).toInt()) {
                sendEmbed(EmbedBuilder().setColor(builtEmbed.color).setAuthor(builtEmbed.author.name, builtEmbed.author.url, builtEmbed.author.iconUrl)
                        .setDescription(description.substring(2048 * i, if ((2048 * i + 2048 > description.length)) 2048 * i else (2048 * i) + 2048)))
            }
        }
    } catch (ex: Exception) {
    }
    return null
}

fun Guild.getPrefix(): String {
    return getData().prefix ?: "/"
}

fun Guild.getLanguage(): ArdentLanguage {
    val data = getData()
    val language = data.language
    return if (language != null) language else {
        data.language = Languages.ENGLISH.language
        data.update()
        Languages.ENGLISH.language
    }
}

enum class DonationLevel(val readable: String, val level: Int) {
    NONE("None", 1), SUPPORTER("Supporter", 2), BASIC("Basic", 3), INTERMEDIATE("Intermediate", 4), EXTREME("Extreme", 5);

    override fun toString(): String {
        return readable
    }
}

fun Guild.isPatronGuild(): Boolean {
    return members.size > 300 || donationLevel() != DonationLevel.NONE
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

fun String.tr(language: ArdentLanguage, vararg new: Any): String {
    return language.translate(this)?.trReplace(language, *new) ?: translationDoesntExist(language, *new)
}

fun String.translationDoesntExist(language: ArdentLanguage, vararg new: Any): String {
    val phrase = ArdentPhraseTranslation(this, "Unknown").instantiate(this)
    translationData.phrases.put(this, phrase)
    if (r.table("phrases").filter(r.hashMap("english", this)).count().run<Long>(conn) == 0.toLong()) {
        phrase.insert("phrases")
        logChannel!!.send("```Translation for the following doesn't exist and was automatically inserted into the database: $this```")
        "355817985052508160".toChannel()?.send("A new phrase was automatically detected and added at <https://ardentbot.com/translation/>")
    }
    return this.trReplace(language, *new)
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

data class LoggedCommand(val commandId: String, val userId: String, val executionTime: Long, val readableExecutionTime: String, val id: String = r.uuid().run(conn))

class PlayerData(val id: String, var donationLevel: DonationLevel, var gold: Double = 50.0, var collected: Long = 0, val reminders: MutableList<Reminder> = mutableListOf()) {
    fun canCollect(): Boolean {
        return ((System.currentTimeMillis() - collected) / 1000) > 86399;
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

    fun connect4Data(): Connect4PlayerData {
        val data = Connect4PlayerData()
        r.table("Connect_4Data").run<Any>(conn).queryAsArrayList(GameDataConnect4::class.java).forEach { game ->
            if (game != null && (game.loser == id || game.winner == id)) {
                if (game.winner == id) data.wins++
                else data.losses++
            }
        }
        return data
    }

    fun ticTacToeData(): TicTacToePlayerData {
        val data = TicTacToePlayerData()
        r.table("Tic_Tac_ToeData").run<Any>(conn).queryAsArrayList(GameDataTicTacToe::class.java).forEach { game ->
            if (game != null && (game.playerOne == id || game.playerTwo == id)) {
                if (game.winner == null) data.ties++
                else {
                    if (game.winner == id) data.wins++
                    else data.losses++
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

    fun slotsData(): SlotsPlayerData {
        val data = SlotsPlayerData()
        r.table("SlotsData").run<Any>(conn).queryAsArrayList(GameDataSlots::class.java).forEach { game ->
            if (game != null && game.creator == id) {
                game.rounds.forEach { round ->
                    if (round.won) {
                        data.wins++
                        data.netWinnings += round.bet
                    } else {
                        data.losses++
                        data.netWinnings -= round.bet
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
    var musicPlayed: Double = 0.0
    var tracksPlayed: Long = 0
    val languageStatuses = hashMapOf<ArdentLanguage, Double>()

    init {
        waiterExecutor.scheduleWithFixedDelay({
            loadedMusicPlayers = 0
            queueLength = 0
            apiCalls = 0
            roleCount = 0
            channelCount = 0
            voiceCount = 0
            languageStatuses.clear()
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

            val totalPhrases = translationData.phrases.size
            val tempCount = hashMapOf<ArdentLanguage, Int>()
            Languages.values().forEach { tempCount.put(it.language, 0) }
            translationData.phrases.forEach { _, phrase ->
                phrase.translations.forEach { key, value ->
                    val lang = key.toLanguage()
                    if (lang != null) tempCount.incrementValue(lang)
                }
            }

            tempCount.forEach { lang, phraseCount -> languageStatuses.put(lang, 100 * phraseCount / totalPhrases.toDouble()) }

            musicPlayed = 0.0
            tracksPlayed = 0
            val query = r.table("musicPlayed").run<Any>(conn).queryAsArrayList(PlayedMusic::class.java)
            tracksPlayed = query.size.toLong()
            query.forEach { if (it != null) musicPlayed += it.position }
        }, 0, 10, TimeUnit.SECONDS)
    }
}
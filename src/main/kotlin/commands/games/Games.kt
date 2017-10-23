package commands.games

import events.Category
import events.Command
import main.test
import main.waiter
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import utils.functionality.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


val invites = ConcurrentHashMap<String, Game>()
val questions = mutableListOf<TriviaQuestion>()

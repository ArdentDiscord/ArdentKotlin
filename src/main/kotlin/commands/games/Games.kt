package commands.games

import events.Category
import events.Command
import main.test
import main.waiter
import utils.*
import utils.functionality.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


//val invites = ConcurrentHashMap<String, Game>()
val questions = mutableListOf<TriviaQuestion>()

package utils.functionality
/*
import commands.games.questions
import java.util.*

data class TriviaQuestion(val question: String, val answers: List<String>, val category: String, val value: Int)

fun getTriviaQuestions(number: Int): List<TriviaQuestion> {
    val list = mutableListOf<TriviaQuestion>()
    val random = Random()
    (1..number).forEach {
        val q = questions[random.nextInt(questions.size)]
        if (!list.contains(q)) list.add(q)
    }
    return list
}
*/

data class Reminder(val owner: String, val setAt: Long = System.currentTimeMillis(), val message: String, val expiresAt: Long)
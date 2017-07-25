package utils

abstract class Game(val type : GameType) {
    val gameId : Long
    val players = mutableListOf<String>()
}

enum class GameType(val readable : String, val id : Int) {
    COINFLIP("Coinflip", 1),
    BLACKJACK("Blackjack", 2),
    TRIVIA("Trivia", 3),
    CONNECT_FOUR("Connect Four", 4);

    override fun toString(): String {
        return readable
    }
}
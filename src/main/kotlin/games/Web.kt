package games

import spark.Spark.port

class Web {
    init {
        port(752)
    }
}

class PlayerData(var triviaData : TriviaPlayerData)

class TriviaPlayerData(var wins : Int, var losses : Int, var questionsCorrect : Int, var questionsWrong : Int)

class GameDataTrivia(val winner : String, val scores : HashMap<String, Int>)
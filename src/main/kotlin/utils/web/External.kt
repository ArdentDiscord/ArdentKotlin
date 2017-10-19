package utils.web

import main.httpClient
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

data class EightBallResult(val magic: Magic)
data class Magic /* The name was not my choice...... */(val question: String, val answer: String, val type: String)

data class UrbanDictionarySearch(val tags: List<String>, val result_type: String, val list: List<UrbanDictResult>, val sounds: List<String>)

data class UrbanDictResult(val definition: String, val permalink: String, val thumbs_up: Int, val author: String, val word: String,
                    val defid: String, val current_vote: String, val example: String, val thumbs_down: Int)


fun paste(toSend: String): String {
    try {
        val post = RequestBody.create(MediaType.parse("text/plain"), toSend);
        val toPost = Request.Builder()
                .url("https://hastebin.com/documents")
                .header("User-Agent", "Ardent")
                .header("Content-Type", "text/plain")
                .post(post)
                .build()
        val r = httpClient.newCall(toPost).execute()
        val response = org.json.JSONObject(r.body()?.string())
        r.close()
        return "https://hastebin.com/" + response.getString("key")
    } catch (e: Exception) {
        e.printStackTrace()
        return "Pastebin is unavaliable right now"
    }
}

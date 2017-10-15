package utils.discord

import main.httpClient
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

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

import main.test
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine
import web.Setting

val settings = mutableListOf<Setting>()

val handlebars = HandlebarsTemplateEngine()

val loginRedirect = if (test) "http://localhost/api/oauth/login" else "https://ardentbot.com/api/oauth/login"

class Web {
    init {
        startup()
        TODO()
    }
}

private fun startup() {
    if (!test) {
        port(443)
        secure("/root/Ardent/ssl/keystore.p12", "mortimer5", null, null)
    } else port(80)

}

fun restartWebServer() {
    stop()
    Web()
}
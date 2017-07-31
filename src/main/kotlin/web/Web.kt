package web

import main.conn
import spark.*
import spark.Spark.*
import main.factory
import main.r
import utils.*
import java.util.concurrent.atomic.AtomicLong

val webCalls = AtomicLong(0)
val apiCredentials = hashMapOf<String /* Code given to frontend */, String /* User ID */>()

class Web {
    init {
        val credentials = r.table("apiCodes").run<Any>(conn).queryAsArrayList(Credential::class.java)
        credentials.forEach { if (it != null) apiCredentials.put(it.code, it.id) }
        port(444)
       secure("/root/Ardent/keystore.p12", "ardent", null, null)
        internalServerError({ _, _ -> "Well, we fucked up".toJson() })
        get("", { request, response ->
            "hi"
        })
        path("/api", {
            before("/*", { _, _ -> webCalls.getAndIncrement() })
            get("/status", { _, _ -> Internals().toJson() })
            get("/commands", { _, _ -> factory.commands })
            path("/oauth", {
                get("/login", { request, response ->
                    val code = request.params("code")
                    if (code == null) Failure("Invalid URL").toJson()
                    response.redirect("https://ardentbot.com/onboard/$code")
                })
                post("/login", { request, response ->
                    if (request.headers("code") == null) Failure("No code provided.").toJson()
                    else {
                        val code = request.headers("code")
                        val token = retrieveToken(code)
                        if (token == null) Failure("Provided an invalid code.").toJson()
                        else {
                            val identification = identityObject(token.access_token)
                            if (identification == null) Failure("The user didn't allow the Identify scope.").toJson()
                            else {
                                apiCredentials.put(code, identification.id)
                                Credential(code, identification.id).insert("apiCodes")
                                identification.toJson()
                            }
                        }
                    }
                })
            })
            path("/internal", {

            })
        })
    }
}


package web

import spark.*
import spark.Spark.*
import main.factory
import utils.Internals
import utils.getGson
import utils.toJson
import java.util.concurrent.atomic.AtomicLong

val webCalls = AtomicLong(0)

class Web {
    init {
        port(8082)
        internalServerError({ _, _ -> "Well, we fucked up".toJson() })
        path("/api", {
            before("/*", { _, _ -> webCalls.getAndIncrement() })
            get("/status", { _, _ -> Internals().toJson() })
            get("/commands", { _, _ -> factory.commands })
        })
    }
}


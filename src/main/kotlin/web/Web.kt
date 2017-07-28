package web

import spark.Spark.*
import spark.*
import utils.Internals
import utils.getGson
import utils.toJson

class Web {
    val base = "/api/"

    init {
        port(752)
        get("${base}status", { _, response ->
            Internals().toJson()
        })
    }
}


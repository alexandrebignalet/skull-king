package org.skull.king.application.config

import java.net.URI

class PostgresConfig {
    lateinit var jdbcUrl: String
    var ssl: Boolean = false


    fun resolveConnection() = URI.create(jdbcUrl).let { uri ->
        Connection(
            uri.userInfo.split(":")[0],
            uri.userInfo.split(":")[1],
            "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}${if (ssl) "?sslmode=require" else ""}"
        )
    }

    data class Connection(val user: String, val password: String, val url: String)
}

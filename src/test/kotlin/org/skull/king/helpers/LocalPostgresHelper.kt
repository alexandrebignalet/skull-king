package org.skull.king.helpers

import io.dropwizard.testing.ResourceHelpers
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import org.skull.king.config.PostgresConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.DriverManager

class LocalPostgresHelper(jdbcUrl: String) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LocalPostgresHelper::class.java)
    }

    val connection = PostgresConfig.Connection("test", "test", jdbcUrl)

    fun createTables() {
        DriverManager.getConnection(connection.url, connection.user, connection.password).use { connection ->
            val liquibase = Liquibase(
                "db/changelog.xml",
                FileSystemResourceAccessor(),
                DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            )
            liquibase.dropAll()
            liquibase.update(Contexts())
        }
    }

    fun flushTables() {
        DriverManager.getConnection(connection.url, connection.user, connection.password).use { connection ->
            val initTableSql = File(ResourceHelpers.resourceFilePath("db/flush_tables.sql")).readText()
            connection
                .createStatement()
                .execute(initTableSql)
        }
    }
}

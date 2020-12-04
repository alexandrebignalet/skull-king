package org.skull.king.helpers

import io.dropwizard.testing.ResourceHelpers
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import java.io.File
import java.sql.DriverManager

class LocalPostgresHelper(private val jdbcUrl: String) {

    fun createTables() {
        DriverManager.getConnection(jdbcUrl, "test", "test").use { connection ->
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
        DriverManager.getConnection(jdbcUrl, "test", "test").use { connection ->
            val initTableSql = File(ResourceHelpers.resourceFilePath("db/flush_tables.sql")).readText()
            connection
                .createStatement()
                .execute(initTableSql)
        }
    }

    fun selectOne() {
        DriverManager.getConnection(jdbcUrl, "test", "test").use { connection ->
            val query = "SELECT * FROM EVENTS"

            val res = connection
                .createStatement()
                .execute(query)

            print(res)
        }
    }
}

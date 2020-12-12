package org.skull.king.helpers

import io.dropwizard.testing.ConfigOverride
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer

abstract class DockerIntegrationTestUtils {

    companion object {
        private const val POSTGRES_VERSION = "12"
        private const val DATABASE_NAME = "skullking"
        private const val DATABASE_USER = "test"
        private const val DATABASE_PASSWORD = "test"

        lateinit var localPostgres: LocalPostgresHelper
        lateinit var localFirebase: LocalFirebase

        @JvmStatic
        private val postgresContainer = object : PostgreSQLContainer<Nothing>("postgres:${POSTGRES_VERSION}") {
            init {
                withDatabaseName(DATABASE_NAME)
                withUsername(DATABASE_USER)
                withPassword(DATABASE_PASSWORD)
            }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            postgresContainer.start()

            localFirebase = LocalFirebase()
            localPostgres = LocalPostgresHelper(postgresContainer.jdbcUrl)
            localPostgres.createTables()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            postgresContainer.stop()
        }

        fun jdbcUrl() = postgresContainer.getMappedPort(5432).let { port ->
            "postgresql://$DATABASE_USER:$DATABASE_PASSWORD@${postgresContainer.host}:$port/$DATABASE_NAME"
        }

        fun configOverride() = arrayOf(ConfigOverride.config("postgres.jdbcUrl", jdbcUrl()))
    }

    @AfterEach
    fun afterEach() {
        localPostgres.flushTables()
        localFirebase.clearFirebaseData()
    }
}

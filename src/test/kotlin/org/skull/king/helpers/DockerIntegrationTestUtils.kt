package org.skull.king.helpers

import io.dropwizard.testing.ConfigOverride
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer

abstract class DockerIntegrationTestUtils {

    companion object {
        private const val POSTGRES_VERSION = "12"

        lateinit var localPostgres: LocalPostgresHelper
        lateinit var localFirebase: LocalFirebase

        @JvmStatic
        private val postgresContainer = object : PostgreSQLContainer<Nothing>("postgres:${POSTGRES_VERSION}") {
            init {
                withDatabaseName("skullking")
                withUsername("test")
                withPassword("test")
            }
        }

        init {
            postgresContainer.start()
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            localFirebase = LocalFirebase()
            localPostgres = LocalPostgresHelper(postgresContainer.jdbcUrl)
            localPostgres.createTables()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            localPostgres.flushTables()
            localFirebase.clearFirebaseData()
        }

        fun configOverride(): Array<ConfigOverride> {
            return arrayOf(
                ConfigOverride.config(
                    "postgres.url",
                    postgresContainer.jdbcUrl
                )
            )
        }
    }
}

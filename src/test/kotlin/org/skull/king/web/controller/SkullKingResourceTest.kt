package org.skull.king.web.controller

import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skull.king.SkullKingApplication
import org.skull.king.config.SkullKingConfig
import org.skull.king.helpers.ApiHelper
import org.skull.king.web.controller.dto.start.StartResponse
import java.util.UUID
import javax.ws.rs.client.Entity

@ExtendWith(DropwizardExtensionsSupport::class)
class SkullKingResourceTest {

    private val EXTENSION = DropwizardAppExtension<SkullKingConfig>(
        SkullKingApplication::class.java,
        ResourceHelpers.resourceFilePath("config.yml"),
        *configOverride()
    )

    private fun configOverride(): Array<ConfigOverride> {
        return arrayOf()
    }

    val api = ApiHelper(EXTENSION)

    @Test
    fun `Should start a new game with some players`() {
        val uuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns uuid

        val startRequest = """{
            "player_ids": ["1", "2", "3"]  
        }""".trimIndent()

        val commandResponse = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/start")
            .request()
            .post(Entity.json(startRequest))
            .readEntity(StartResponse::class.java)


        Assertions.assertThat(commandResponse).isEqualTo(StartResponse(uuid.toString()))

        unmockkStatic(UUID::class)
    }

    @Test
    fun `Should return a bad request if less than 2 players to start`() {
        val response = api.skullKing.start(setOf("1"))

        Assertions.assertThat(response.status).isEqualTo(400)
    }
}

package org.skull.king.resource

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
import org.skull.king.query.ReadSkullKing
import org.skull.king.resource.dto.start.StartResponse
import java.util.UUID
import javax.ws.rs.client.Entity

@ExtendWith(DropwizardExtensionsSupport::class)
class GameResourceTest {

    private val EXTENSION = DropwizardAppExtension<SkullKingConfig>(
        SkullKingApplication::class.java,
        ResourceHelpers.resourceFilePath("config.yml"),
        *configOverride()
    )

    private fun configOverride(): Array<ConfigOverride> {
        return arrayOf()
    }

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

        
        val readSkullKing = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/${commandResponse.gameId}")
            .request()
            .get()
            .readEntity(ReadSkullKing::class.java)

        Assertions.assertThat(readSkullKing.id).isEqualTo(uuid.toString())
        Assertions.assertThat(readSkullKing.players).isEqualTo(listOf("1", "2", "3"))

        unmockkStatic(UUID::class)
    }
}

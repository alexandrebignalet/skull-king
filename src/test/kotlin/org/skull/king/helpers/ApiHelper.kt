package org.skull.king.helpers

import io.dropwizard.testing.junit5.DropwizardAppExtension
import org.skull.king.config.SkullKingConfig
import org.skull.king.web.controller.dto.AnnounceWinningCardsFoldCountRequest
import org.skull.king.web.controller.dto.start.StartRequest
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response

class ApiHelper(private val EXTENSION: DropwizardAppExtension<SkullKingConfig>) {

    val skullKing = SkullKingApiHelper()

    inner class SkullKingApiHelper {
        fun start(playerIds: Set<String>): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/start")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(StartRequest(playerIds)))

        fun announce(gameId: String, playerId: String, count: Int): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/$gameId/players/$playerId/announce")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(AnnounceWinningCardsFoldCountRequest(count)))
    }
}

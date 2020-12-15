package org.skull.king.helpers

import io.dropwizard.testing.junit5.DropwizardAppExtension
import org.skull.king.config.SkullKingConfig
import org.skull.king.web.controller.dto.AnnounceWinningCardsFoldCountRequest
import org.skull.king.web.controller.dto.start.StartRequest
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response

class ApiHelper(private val EXTENSION: DropwizardAppExtension<SkullKingConfig>) {

    val skullKing = SkullKingApiHelper()
    val gameRoom = GameRoomHelper()

    inner class SkullKingApiHelper {
        fun start(playerIds: Set<String>, idToken: String = "token"): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/start")
            .request()
            .header("Authorization", "Bearer $idToken")
            .post(Entity.json(StartRequest(playerIds)))

        fun announce(gameId: String, playerId: String, count: Int, idToken: String = "token"): Response =
            EXTENSION.client()
                .target("http://localhost:${EXTENSION.localPort}/skullking/games/$gameId/players/$playerId/announce")
                .request()
                .header("Authorization", "Bearer $idToken")
                .post(Entity.json(AnnounceWinningCardsFoldCountRequest(count)))
    }

    inner class GameRoomHelper {
        fun join(gameRoomId: String): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(null))

        fun kick(gameRoomId: String, userId: String): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users/$userId")
            .request()
            .header("Authorization", "Bearer token")
            .delete()

        fun launch(gameRoomId: String): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/launch")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(null))
    }
}

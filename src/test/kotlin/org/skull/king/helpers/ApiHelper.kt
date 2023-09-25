package org.skull.king.helpers

import io.dropwizard.testing.junit5.DropwizardAppExtension
import org.skull.king.application.config.SkullKingConfig
import org.skull.king.application.infrastructure.authentication.User
import org.skull.king.core.infrastructure.web.AnnounceWinningCardsFoldCountRequest
import org.skull.king.core.infrastructure.web.StartRequest
import org.skull.king.game_room.domain.Configuration
import org.skull.king.game_room.infrastructure.web.CreateGameRoomResponse
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response

class ApiHelper(private val EXTENSION: DropwizardAppExtension<SkullKingConfig>) {

    val skullKing = SkullKingApiHelper()
    val gameRoom = GameRoomHelper()

    inner class SkullKingApiHelper {
        fun start(users: List<User>, configuration: Configuration? = null): Response {
            val gameRoomHelper = GameRoomHelper()
            val userIds = users.map { it.id }.toSet()
            val creator = userIds.first()
            val (id) = gameRoomHelper.create(configuration, creator).readEntity(CreateGameRoomResponse::class.java)

            userIds.forEach { gameRoomHelper.join(id, it) }

            return EXTENSION.client()
                .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/${id}/launch")
                .request()
                .header("Authorization", "Bearer $creator")
                .post(Entity.json(StartRequest(userIds)))
        }

        fun announce(gameId: String, playerId: String, count: Int, idToken: String = "token"): Response =
            EXTENSION.client()
                .target("http://localhost:${EXTENSION.localPort}/skullking/games/$gameId/players/$playerId/announce")
                .request()
                .header("Authorization", "Bearer $idToken")
                .post(Entity.json(AnnounceWinningCardsFoldCountRequest(count)))
    }

    inner class GameRoomHelper {

        fun create(configuration: Configuration?, idToken: String): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms")
            .request()
            .header("Authorization", "Bearer $idToken")
            .post(Entity.json(configuration))


        fun join(gameRoomId: String, idToken: String = "token"): Response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users")
            .request()
            .header("Authorization", "Bearer $idToken")
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

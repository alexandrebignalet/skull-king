package org.skull.king.game_room.infrastructure.web

import io.dropwizard.auth.Auth
import org.skull.king.application.infrastructure.authentication.User
import org.skull.king.core.infrastructure.web.StartResponse
import org.skull.king.game_room.domain.Configuration
import org.skull.king.game_room.domain.GameUser
import org.skull.king.game_room.infrastructure.GameRoomService
import javax.annotation.Nullable
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@PermitAll
@Path("/skullking/game_rooms")
@Produces(MediaType.APPLICATION_JSON)
class GameRoomResource @Inject constructor(private val service: GameRoomService) {

    @POST
    fun createRoom(@Auth creator: User, @Nullable configuration: Configuration?): Response {
        val gameRoomId = service.create(GameUser.from(creator), configuration)
        return Response.ok(CreateGameRoomResponse(gameRoomId)).build()
    }

    @POST
    @Path("/{game_room_id}/bots")
    fun addBot(
        @PathParam("game_room_id") gameRoomId: String,
        @Auth user: User,
    ): Response {
        service.join(gameRoomId, GameUser.bot())
        return Response.noContent().build()
    }

    @POST
    @Path("/{game_room_id}/users")
    fun join(
        @PathParam("game_room_id") gameRoomId: String,
        @Auth user: User
    ): Response {
        service.join(gameRoomId, GameUser.from(user))
        return Response.noContent().build()
    }

    @DELETE
    @Path("/{game_room_id}/users/{user_id}")
    fun kick(
        @PathParam("game_room_id") gameRoomId: String,
        @PathParam("user_id") kicked: String,
        @Auth user: User
    ): Response {
        service.kick(gameRoomId, user.id, kicked)
        return Response.noContent().build()
    }

    @POST
    @Path("/{game_room_id}/launch")
    fun launch(@PathParam("game_room_id") gameRoomId: String, @Auth user: User): Response {
        val gameId = service.startGame(gameRoomId, user.id)
        return Response.ok(StartResponse(gameId)).build()
    }
}

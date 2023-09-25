package org.skull.king.web.controller

import io.dropwizard.auth.Auth
import java.util.*
import javax.annotation.Nullable
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.skull.king.domain.supporting.room.GameRoomService
import org.skull.king.domain.supporting.room.domain.Configuration
import org.skull.king.domain.supporting.user.domain.GameUser
import org.skull.king.infrastructure.authentication.User
import org.skull.king.web.controller.dto.AnnounceWinningCardsFoldCountRequest
import org.skull.king.web.controller.dto.CreateGameRoomResponse
import org.skull.king.web.controller.dto.start.StartResponse
import javax.validation.Valid

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

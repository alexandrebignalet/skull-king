package org.skull.king.web.controller;

import org.skull.king.domain.core.command.StartSkullKing
import org.skull.king.domain.core.saga.AnnounceWinningCardsFoldCountSaga
import org.skull.king.domain.core.saga.PlayCardSaga
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.web.controller.dto.AnnounceWinningCardsFoldCountRequest
import org.skull.king.web.controller.dto.PlayCardRequest
import org.skull.king.web.controller.dto.start.StartRequest
import org.skull.king.web.controller.dto.start.StartResponse
import java.util.UUID
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@PermitAll
@Path("/skullking/games")
@Produces(MediaType.APPLICATION_JSON)
class SkullKingResource @Inject constructor(private val commandBus: CommandBus) {

    @POST
    @Path("/start")
    @Consumes(MediaType.APPLICATION_JSON)
    fun startGame(@Valid request: StartRequest): Response {

        val gameId = UUID.randomUUID().toString()

        val command = StartSkullKing(gameId, request.playerIds.toList())

        commandBus.send(command)


        return Response.ok(StartResponse(gameId)).build()
    }

    @POST
    @Path("/{game_id}/players/{player_id}/announce")
    @Consumes(MediaType.APPLICATION_JSON)
    fun startGame(
        @PathParam("game_id") gameId: String,
        @PathParam("player_id") playerId: String,
        @Valid request: AnnounceWinningCardsFoldCountRequest
    ): Response {

        val command = AnnounceWinningCardsFoldCountSaga(gameId, playerId, request.count)

        commandBus.send(command)

        return Response.noContent().build()
    }

    @POST
    @Path("/{game_id}/players/{player_id}/play")
    @Consumes(MediaType.APPLICATION_JSON)
    fun startGame(
        @PathParam("game_id") gameId: String,
        @PathParam("player_id") playerId: String,
        @Valid request: PlayCardRequest
    ): Response {

        val command = PlayCardSaga(gameId, playerId, request.card)

        commandBus.send(command)

        return Response.noContent().build()
    }
}

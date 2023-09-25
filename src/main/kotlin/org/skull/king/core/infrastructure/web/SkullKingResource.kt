package org.skull.king.core.infrastructure.web

import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.core.usecases.AnnounceWinningCardsFoldCountSaga
import org.skull.king.core.usecases.PlayCardSaga
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.validation.Valid
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@PermitAll
@Path("/skullking/games")
@Produces(MediaType.APPLICATION_JSON)
class SkullKingResource @Inject constructor(private val commandBus: CommandBus) {

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

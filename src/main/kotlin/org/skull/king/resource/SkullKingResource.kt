package org.skull.king.resource;

import org.skull.king.command.StartSkullKing
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.resource.dto.start.StartRequest
import org.skull.king.resource.dto.start.StartResponse
import java.util.UUID
import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.validation.Valid as JavaxValid

@Path("/skullking")
class SkullKingResource @Inject constructor(private val commandBus: CommandBus) {

    @POST
    @Path("/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun startGame(@JavaxValid request: StartRequest): Response? {

        val gameId = UUID.randomUUID().toString()

        val command = StartSkullKing(gameId, request.playerIds.toList())

        commandBus.send(command)


        return Response.ok(StartResponse(gameId)).build()
    }
}

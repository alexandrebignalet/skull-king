package org.skull.king.resource;

import kotlinx.coroutines.runBlocking
import org.skull.king.application.Application
import org.skull.king.command.StartSkullKing
import org.skull.king.event.Started
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid
import org.skull.king.resource.dto.start.StartRequest
import org.skull.king.resource.dto.start.StartResponse
import java.util.UUID
import javax.inject.Inject
import javax.ws.rs.BadRequestException
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.validation.Valid as JavaxValid

@Path("/skullking")
class SkullKingResource @Inject constructor(private val skullKingCore: Application) {

    @POST
    @Path("/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun startGame(@JavaxValid request: StartRequest): Response? {

        val event: Started = runBlocking {
            skullKingCore.run {
                val gameId = UUID.randomUUID().toString()

                val started = StartSkullKing(gameId, request.playerIds.toList()).process().await()

                if (started is Invalid) {
                    throw BadRequestException(started.err.toString())
                }

                (started as Valid).value.first() as Started
            }
        }

        return Response.ok(StartResponse(event.gameId)).build()
    }
}

package org.skull.king.resource;

import org.skull.king.cqrs.query.QueryBus
import org.skull.king.query.ReadSkullKing
import org.skull.king.query.handler.GetGame
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/skullking/games")
@Produces(MediaType.APPLICATION_JSON)
class GameResource @Inject constructor(private val queryBus: QueryBus) {

    @GET
    @Path("/{game_id}")
    fun getGame(@PathParam("game_id") gameId: String): ReadSkullKing {
        val query = GetGame(gameId)
        return queryBus.send(query)
    }

}

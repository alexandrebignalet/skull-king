package org.skull.king.resource;

import org.skull.king.application.Application
import org.skull.king.query.GetGame
import org.skull.king.query.ReadSkullKing
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/skullking/games")
@Produces(MediaType.APPLICATION_JSON)
class GameResource @Inject constructor(private val skullKingCore: Application) {

    @GET
    @Path("/{game_id}")
    fun getGame(@PathParam("game_id") gameId: String): ReadSkullKing {
        return skullKingCore.run {
            GetGame(gameId).process().first() as ReadSkullKing
        }
    }

}

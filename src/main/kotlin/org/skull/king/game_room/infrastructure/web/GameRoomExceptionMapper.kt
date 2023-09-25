package org.skull.king.game_room.infrastructure.web

import org.skull.king.application.web.exception.BaseErrorMessage
import org.skull.king.game_room.domain.BaseGameRoomException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class GameRoomExceptionMapper : ExceptionMapper<BaseGameRoomException> {
    override fun toResponse(exception: BaseGameRoomException): Response {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(BaseErrorMessage(exception.message))
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}

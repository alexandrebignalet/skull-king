package org.skull.king.web.exception

import org.skull.king.domain.core.command.error.CardNotAllowedError
import org.skull.king.domain.core.command.error.DomainError
import org.skull.king.domain.core.command.error.NotYourTurnError
import org.skull.king.domain.core.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.domain.core.command.error.SkullKingNotReadyError
import org.skull.king.domain.core.command.error.SkullKingOverError
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class DomainErrorExceptionMapper : ExceptionMapper<DomainError> {
    override fun toResponse(exception: DomainError): Response {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(BaseErrorMessage(exception.message, DomainErrorCode.resolve(exception).name))
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}

enum class DomainErrorCode {
    INTERNAL,
    ALREADY_ANNOUNCED,
    OVER,
    CARD_NOT_ALLOWED,
    NOT_YOUR_TURN,
    ALL_MUST_ANNOUNCE;

    companion object {
        fun resolve(error: DomainError) = when (error) {
            is PlayerAlreadyAnnouncedError -> ALREADY_ANNOUNCED
            is SkullKingOverError -> OVER
            is CardNotAllowedError -> CARD_NOT_ALLOWED
            is NotYourTurnError -> NOT_YOUR_TURN
            is SkullKingNotReadyError -> ALL_MUST_ANNOUNCE
            else -> INTERNAL
        }
    }
}


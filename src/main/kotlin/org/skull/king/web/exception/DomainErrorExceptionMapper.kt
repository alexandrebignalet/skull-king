package org.skull.king.web.exception

import org.skull.king.core.command.error.DomainError
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class DomainErrorExceptionMapper : ExceptionMapper<DomainError> {
    override fun toResponse(exception: DomainError): Response {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(BaseErrorMessage(exception.message, Response.Status.BAD_REQUEST.statusCode))
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}

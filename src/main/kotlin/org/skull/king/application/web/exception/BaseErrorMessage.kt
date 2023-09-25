package org.skull.king.application.web.exception

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseErrorMessage(
    val message: String? = null,
    val code: String? = null
)

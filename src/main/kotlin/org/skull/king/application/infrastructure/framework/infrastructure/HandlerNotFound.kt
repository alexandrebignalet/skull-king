package org.skull.king.application.infrastructure.framework.infrastructure


data class HandlerNotFound(val javaClass: Class<*>) : RuntimeException("HANDLER_NOT_FOUND - $javaClass")

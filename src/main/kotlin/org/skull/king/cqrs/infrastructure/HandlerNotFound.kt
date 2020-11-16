package org.skull.king.cqrs.infrastructure


data class HandlerNotFound(val javaClass: Class<*>) : RuntimeException("HANDLER_NOT_FOUND - $javaClass")

package org.skull.king.infrastructure.framework.infrastructure


data class HandlerNotFound(val javaClass: Class<*>) : RuntimeException("HANDLER_NOT_FOUND - $javaClass")

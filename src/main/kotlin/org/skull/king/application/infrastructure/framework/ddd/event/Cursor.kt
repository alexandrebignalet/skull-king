package org.skull.king.application.infrastructure.framework.ddd.event

interface Cursor {
    fun count(): Long
    fun <TRoot> consume(consumer: (events: Sequence<Event>) -> TRoot): TRoot
}

package org.skull.king.cqrs.ddd.event

interface Cursor {
    fun count(): Long
    fun <TRoot> consume(consumer: (events: Sequence<Event>) -> TRoot): TRoot
}

package org.skull.king.infrastructure.event

import org.skull.king.cqrs.ddd.event.Cursor
import org.skull.king.cqrs.ddd.event.Event

class EventCursor(private val values: List<Event>) : Cursor {
    override fun count(): Long = values.size.toLong()

    override fun <TRoot> consume(consumer: (events: Sequence<Event>) -> TRoot): TRoot =
        consumer(values.asSequence().sortedBy { it.timestamp })
}

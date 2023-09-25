package org.skull.king.application.infrastructure.event

import org.skull.king.application.infrastructure.framework.ddd.event.Cursor
import org.skull.king.application.infrastructure.framework.ddd.event.Event

class EventCursor(private val values: List<Event>) : Cursor {
    override fun count(): Long = values.size.toLong()

    override fun <TRoot> consume(consumer: (events: Sequence<Event>) -> TRoot): TRoot =
        consumer(values.asSequence().sortedBy { it.timestamp })
}

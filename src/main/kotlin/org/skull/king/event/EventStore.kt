package org.skull.king.event

import kotlinx.coroutines.channels.SendChannel

abstract class EventStore {

    abstract val sendChannel: SendChannel<List<Event>>

    abstract fun addListener(listener: SendChannel<Event>)

    inline fun <reified T : Event> getEvents(pk: String): List<T> =
        when (T::class) {
            SkullKingEvent::class -> getSkullKingEvents(pk) as List<T>
            else -> emptyList()
        }

    abstract fun getSkullKingEvents(pk: String): List<SkullKingEvent>
}

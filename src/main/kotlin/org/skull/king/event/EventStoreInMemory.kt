package org.skull.king.event

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.skull.king.application.createActor

class EventStoreInMemory : EventStore() {

    override fun getSkullKingEvents(pk: String) = skullKingEventCache.getOrDefault(pk, emptyList())

    private val skullKingEventCache = mutableMapOf<String, List<SkullKingEvent>>()
    private val listeners: MutableList<SendChannel<Event>> = mutableListOf()

    override val sendChannel = createActor<List<Event>> {
        processEvents(it)
    }

    private fun processEvents(events: List<Event>) = events.forEach { event ->

        when (event) {
            is SkullKingEvent -> skullKingEventCache.compute(event.key()) { _, el ->
                (el ?: emptyList()).plus(event)
            }
        }

        for (listener in listeners) {
            runBlocking {
                listener.send(event)
            }
        }

        println("Processed Event $event")
    }

    override fun addListener(listener: SendChannel<Event>) {
        listeners.add(listener)
    }

    fun saveAllEvents() {
        //persist all events
        //not implemented

    }

    fun loadAllEvents() {
        //load all events from persistence
        //not implemented
    }

}



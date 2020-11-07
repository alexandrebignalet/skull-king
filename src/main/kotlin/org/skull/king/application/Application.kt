package org.skull.king.application

import org.skull.king.query.Query
import org.skull.king.eventStore.EventStoreInMemory
import org.skull.king.functional.Invalid
import org.skull.king.query.ReadEntity
import org.skull.king.query.QueryHandler
import kotlinx.coroutines.*
import org.skull.king.command.CmdResult
import org.skull.king.command.Command
import org.skull.king.command.CommandHandler
import org.skull.king.command.DomainError


class Application {

    private val eventStore = EventStoreInMemory()
    private val commandHandler = CommandHandler(eventStore)
    private val queryHandler = QueryHandler()

    fun start() {
        eventStore.addListener(queryHandler.eventChannel)
        eventStore.loadAllEvents()
    }

    fun stop() {
        eventStore.saveAllEvents()
    }

    fun List<Command>.processAllInSync(): List<DomainError> =
        runBlocking {
            map {
                it.process().await()
                yield()
            }
            .filterIsInstance<Invalid<DomainError>>()
            .map { it.err }
        }

    fun List<Command>.processAllAsync(): Deferred<List<CmdResult>> =
        GlobalScope.async {
            map { it.process() }.map { it.await() }
        }

    fun Command.process(): CompletableDeferred<CmdResult> {
        return commandHandler.handle(this)
    }

    fun Query.process(): List<ReadEntity> {
        return queryHandler.handle(this)
    }

}



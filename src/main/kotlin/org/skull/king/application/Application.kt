package org.skull.king.application

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.skull.king.command.CmdResult
import org.skull.king.command.Command
import org.skull.king.command.CommandHandler
import org.skull.king.command.error.DomainError
import org.skull.king.event.EventStoreInMemory
import org.skull.king.event.handler.FoldSettledHandler
import org.skull.king.functional.Invalid
import org.skull.king.query.Query
import org.skull.king.query.QueryHandler
import org.skull.king.query.ReadEntity


class Application {

    private val eventStore = EventStoreInMemory()
    private val commandHandler = CommandHandler(eventStore)
    private val foldSettledHandler = FoldSettledHandler(commandHandler, eventStore)
    private val queryHandler = QueryHandler()

    fun start() {
        eventStore.addListener(queryHandler.eventChannel)
        eventStore.addListener(foldSettledHandler.eventChannel)
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



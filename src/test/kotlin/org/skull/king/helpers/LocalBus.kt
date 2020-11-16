package org.skull.king.helpers

import org.skull.king.command.handler.AnnounceHandler
import org.skull.king.command.handler.PlayCardHandler
import org.skull.king.command.handler.SettleFoldHandler
import org.skull.king.command.handler.StartHandler
import org.skull.king.cqrs.command.Command
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.command.CommandMiddleware
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.ddd.event.EventBus
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.cqrs.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.cqrs.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.cqrs.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.cqrs.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.cqrs.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.cqrs.query.QueryBus
import org.skull.king.cqrs.saga.Saga
import org.skull.king.cqrs.saga.SagaHandler
import org.skull.king.cqrs.saga.SagaMiddleware
import org.skull.king.event.EventStoreInMemory
import org.skull.king.query.handler.GetGameHandler
import org.skull.king.query.handler.GetPlayerHandler
import org.skull.king.query.sync.OnCardPlayed
import org.skull.king.query.sync.OnFoldWinnerSettled
import org.skull.king.query.sync.OnGameFinished
import org.skull.king.query.sync.OnGameStarted
import org.skull.king.query.sync.OnNewRoundStarted
import org.skull.king.query.sync.OnPlayerAnnounced
import org.skull.king.repository.QueryRepositoryInMemory
import org.skull.king.repository.SkullkingEventSourcedRepositoryInMemory
import org.skull.king.saga.PlayCardSagaHandler
import org.slf4j.LoggerFactory
import java.util.function.Supplier

open class LocalBus {

    private val queryRepository = QueryRepositoryInMemory()
    private val eventStore = EventStoreInMemory()
    private val commandRepository = SkullkingEventSourcedRepositoryInMemory(eventStore)

    val queryBus: QueryBus = QueryBusSynchronous(
        setOf(),
        setOf(GetGameHandler(queryRepository), GetPlayerHandler(queryRepository))
    )

    val eventBus: EventBus = EventBusSynchronous(
        setOf(),
        setOf(
            OnCardPlayed(queryRepository),
            OnFoldWinnerSettled(queryRepository),
            OnGameFinished(queryRepository),
            OnGameStarted(queryRepository),
            OnNewRoundStarted(queryRepository),
            OnPlayerAnnounced(queryRepository)
        ) as Set<EventCaptor<Event>>
    )

    val commandBus: CommandBus = CommandBusSynchronous(
        setOf(
            EventStoreMiddleware(eventStore),
            EventDispatcherMiddleware(eventBus),
            BusContextLoggerMiddleware(),
            SagaMiddleware(
                setOf(PlayCardSagaHandler(commandRepository)) as Set<SagaHandler<*, Saga<*>>>
            )
        ),
        setOf(
            AnnounceHandler(commandRepository),
            PlayCardHandler(commandRepository),
            SettleFoldHandler(commandRepository),
            StartHandler(commandRepository)
        )
    )

    class BusContextLoggerMiddleware : CommandMiddleware {
        companion object {
            private val LOGGER = LoggerFactory.getLogger(BusContextLoggerMiddleware::class.java)
        }

        override fun <T> intercept(
            bus: CommandBus,
            message: Command<T>,
            next: Supplier<Pair<T, Sequence<Event>>>
        ): Pair<T, Sequence<Event>> {
            LOGGER.info("Processing $message")

            var response: Pair<T, Sequence<Event>>? = null

            kotlin.runCatching {
                response = next.get()
            }.onFailure {
                LOGGER.info("Failed to process $message : $it")
                throw it
            }.onSuccess { LOGGER.info("Processed $message: ${response?.second}") }

            return requireNotNull(response)
        }
    }
}

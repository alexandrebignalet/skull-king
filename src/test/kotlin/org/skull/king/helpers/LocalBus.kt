package org.skull.king.helpers

import java.util.function.Supplier
import org.skull.king.domain.core.command.handler.AnnounceHandler
import org.skull.king.domain.core.command.handler.PlayCardHandler
import org.skull.king.domain.core.command.handler.SettleFoldHandler
import org.skull.king.domain.core.command.handler.StartHandler
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.handler.GetGameHandler
import org.skull.king.domain.core.query.handler.GetPlayerHandler
import org.skull.king.domain.core.query.sync.OnCardPlayed
import org.skull.king.domain.core.query.sync.OnGameFinished
import org.skull.king.domain.core.query.sync.OnGameStarted
import org.skull.king.domain.core.query.sync.OnNewRoundStarted
import org.skull.king.domain.core.query.sync.OnPlayerAnnounced
import org.skull.king.domain.core.query.sync.ProjectOnFoldSettled
import org.skull.king.domain.core.saga.AnnounceWinningCardsFoldCountSagaHandler
import org.skull.king.domain.core.saga.PlayCardSagaHandler
import org.skull.king.infrastructure.event.InMemoryEventStore
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.framework.command.Command
import org.skull.king.infrastructure.framework.command.CommandBus
import org.skull.king.infrastructure.framework.command.CommandMiddleware
import org.skull.king.infrastructure.framework.ddd.event.Event
import org.skull.king.infrastructure.framework.ddd.event.EventBus
import org.skull.king.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.infrastructure.framework.ddd.event.EventStore
import org.skull.king.infrastructure.framework.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.infrastructure.framework.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.infrastructure.framework.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.infrastructure.framework.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.infrastructure.framework.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.infrastructure.framework.query.QueryBus
import org.skull.king.infrastructure.framework.saga.Saga
import org.skull.king.infrastructure.framework.saga.SagaHandler
import org.skull.king.infrastructure.framework.saga.SagaMiddleware
import org.skull.king.infrastructure.repository.FirebaseQueryRepository
import org.skull.king.utils.JsonObjectMapper
import org.slf4j.LoggerFactory

open class LocalBus : LocalFirebase() {

    companion object {
        private val mapper = JsonObjectMapper.getObjectMapper()
    }

    val inMemoryEventStore = InMemoryEventStore()
    val firebaseBuses = Builder(
        inMemoryEventStore,
        SkullkingEventSourcedRepository(inMemoryEventStore),
        FirebaseQueryRepository(database, mapper)
    )

    val queryBus = firebaseBuses.queryBus
    val commandBus = firebaseBuses.commandBus

    class Builder(
        eventStore: EventStore,
        eventSourcedRepository: SkullkingEventSourcedRepository,
        queryRepository: QueryRepository
    ) {
        val queryBus: QueryBus = QueryBusSynchronous(
            setOf(),
            setOf(GetGameHandler(queryRepository), GetPlayerHandler(queryRepository))
        )

        @Suppress("UNCHECKED_CAST")
        val eventBus: EventBus = EventBusSynchronous(
            setOf(),
            setOf(
                OnCardPlayed(queryRepository),
                ProjectOnFoldSettled(queryRepository),
                OnGameFinished(queryRepository),
                OnGameStarted(queryRepository),
                OnNewRoundStarted(queryRepository),
                OnPlayerAnnounced(queryRepository)
            ) as Set<EventCaptor<Event>>
        )

        @Suppress("UNCHECKED_CAST")
        val commandBus: CommandBus = CommandBusSynchronous(
            setOf(
                BusContextLoggerMiddleware(),
                SagaMiddleware(
                    setOf(
                        PlayCardSagaHandler(),
                        AnnounceWinningCardsFoldCountSagaHandler()
                    ) as Set<SagaHandler<*, Saga<*>>>
                ),
                EventDispatcherMiddleware(eventBus),
                EventStoreMiddleware(eventStore),
            ),
            setOf(
                AnnounceHandler(eventSourcedRepository),
                PlayCardHandler(eventSourcedRepository),
                SettleFoldHandler(eventSourcedRepository),
                StartHandler(eventSourcedRepository)
            )
        )
    }

    private class BusContextLoggerMiddleware : CommandMiddleware {
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
            }.onSuccess { LOGGER.info("Processed $message: ${response?.second?.toList()}") }

            return requireNotNull(response)
        }
    }
}

package org.skull.king.helpers

import org.skull.king.application.infrastructure.framework.command.Command
import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.application.infrastructure.framework.command.CommandMiddleware
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import org.skull.king.application.infrastructure.framework.ddd.event.EventBus
import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.application.infrastructure.framework.ddd.event.EventStore
import org.skull.king.application.infrastructure.framework.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.application.infrastructure.framework.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.application.infrastructure.framework.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.application.infrastructure.framework.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.application.infrastructure.framework.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.application.infrastructure.framework.query.QueryBus
import org.skull.king.application.infrastructure.framework.saga.Saga
import org.skull.king.application.infrastructure.framework.saga.SagaHandler
import org.skull.king.application.infrastructure.framework.saga.SagaMiddleware
import org.skull.king.application.utils.JsonObjectMapper
import org.skull.king.core.domain.QueryRepository
import org.skull.king.core.infrastructure.InMemoryEventStore
import org.skull.king.core.infrastructure.SkullkingEventSourcedRepository
import org.skull.king.core.usecases.*
import org.skull.king.core.usecases.captor.*
import org.skull.king.game_room.infrastructure.repository.FirebaseQueryRepository
import org.slf4j.LoggerFactory
import java.util.function.Supplier

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

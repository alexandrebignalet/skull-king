package org.skull.king.helpers

import org.junit.jupiter.api.AfterEach
import org.skull.king.core.command.handler.AnnounceHandler
import org.skull.king.core.command.handler.PlayCardHandler
import org.skull.king.core.command.handler.SettleFoldHandler
import org.skull.king.core.command.handler.StartHandler
import org.skull.king.core.query.handler.GetGameHandler
import org.skull.king.core.query.handler.GetPlayerHandler
import org.skull.king.core.query.sync.OnCardPlayed
import org.skull.king.core.query.sync.OnFoldWinnerSettled
import org.skull.king.core.query.sync.OnGameFinished
import org.skull.king.core.query.sync.OnGameStarted
import org.skull.king.core.query.sync.OnNewRoundStarted
import org.skull.king.core.query.sync.OnPlayerAnnounced
import org.skull.king.core.saga.PlayCardSagaHandler
import org.skull.king.cqrs.command.Command
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.command.CommandMiddleware
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.ddd.event.EventBus
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.cqrs.ddd.event.EventStore
import org.skull.king.cqrs.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.cqrs.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.cqrs.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.cqrs.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.cqrs.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.cqrs.query.QueryBus
import org.skull.king.cqrs.saga.Saga
import org.skull.king.cqrs.saga.SagaHandler
import org.skull.king.cqrs.saga.SagaMiddleware
import org.skull.king.infrastructure.event.EventStoreInMemory
import org.skull.king.infrastructure.event.FirebaseEventStore
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory
import org.skull.king.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import java.util.function.Supplier

open class LocalBus : LocalFirebase() {

    companion object {
        private val mapper = JsonObjectMapper.getObjectMapper()
    }

    @AfterEach
    fun tearDown() {
        clearFirebaseData()
    }

    private val eventStoreInMemory = EventStoreInMemory()
    private val firebaseEventStore = FirebaseEventStore(database, mapper)
    private val inMemoryBuses = Builder(
        eventStoreInMemory,
        SkullkingEventSourcedRepository(eventStoreInMemory),
        QueryRepositoryInMemory()
    )
    val firebaseBuses = Builder(
        firebaseEventStore,
        SkullkingEventSourcedRepository(firebaseEventStore),
        QueryRepositoryInMemory()
    )

    val queryBus = firebaseBuses.queryBus
    val commandBus = firebaseBuses.commandBus

    class Builder(
        val eventStore: EventStore,
        val eventSourcedRepository: SkullkingEventSourcedRepository,
        val queryRepository: QueryRepositoryInMemory
    ) {
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
                    setOf(PlayCardSagaHandler(eventSourcedRepository)) as Set<SagaHandler<*, Saga<*>>>
                )
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
            }.onSuccess { LOGGER.info("Processed $message: ${response?.second}") }

            return requireNotNull(response)
        }
    }
}

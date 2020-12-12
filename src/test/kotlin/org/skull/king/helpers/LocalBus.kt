package org.skull.king.helpers

import org.skull.king.domain.core.command.handler.AnnounceHandler
import org.skull.king.domain.core.command.handler.PlayCardHandler
import org.skull.king.domain.core.command.handler.SettleFoldHandler
import org.skull.king.domain.core.command.handler.StartHandler
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.handler.GetGameHandler
import org.skull.king.domain.core.query.handler.GetPlayerHandler
import org.skull.king.domain.core.query.sync.OnCardPlayed
import org.skull.king.domain.core.query.sync.OnFoldWinnerSettled
import org.skull.king.domain.core.query.sync.OnGameFinished
import org.skull.king.domain.core.query.sync.OnGameStarted
import org.skull.king.domain.core.query.sync.OnNewRoundStarted
import org.skull.king.domain.core.query.sync.OnPlayerAnnounced
import org.skull.king.domain.core.saga.AnnounceWinningCardsFoldCountSagaHandler
import org.skull.king.domain.core.saga.PlayCardSagaHandler
import org.skull.king.infrastructure.cqrs.command.Command
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.command.CommandMiddleware
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.cqrs.ddd.event.EventBus
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor
import org.skull.king.infrastructure.cqrs.ddd.event.EventStore
import org.skull.king.infrastructure.cqrs.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.infrastructure.cqrs.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.infrastructure.cqrs.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.infrastructure.cqrs.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.infrastructure.cqrs.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.infrastructure.cqrs.query.QueryBus
import org.skull.king.infrastructure.cqrs.saga.Saga
import org.skull.king.infrastructure.cqrs.saga.SagaHandler
import org.skull.king.infrastructure.cqrs.saga.SagaMiddleware
import org.skull.king.infrastructure.event.PostgresEventStore
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.repository.FirebaseQueryRepository
import org.skull.king.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import java.util.function.Supplier

open class LocalBus : DockerIntegrationTestUtils() {

    companion object {
        private val mapper = JsonObjectMapper.getObjectMapper()
    }

    val postgresEventStore = PostgresEventStore(localPostgres.connection, mapper)
    val firebaseBuses = Builder(
        postgresEventStore,
        SkullkingEventSourcedRepository(postgresEventStore),
        FirebaseQueryRepository(LocalFirebase.database, mapper)
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
                OnFoldWinnerSettled(queryRepository),
                OnGameFinished(queryRepository),
                OnGameStarted(queryRepository),
                OnNewRoundStarted(queryRepository),
                OnPlayerAnnounced(queryRepository)
            ) as Set<EventCaptor<Event>>
        )

        @Suppress("UNCHECKED_CAST")
        val commandBus: CommandBus = CommandBusSynchronous(
            setOf(
                EventStoreMiddleware(eventStore),
                EventDispatcherMiddleware(eventBus),
                BusContextLoggerMiddleware(),
                SagaMiddleware(
                    setOf(
                        PlayCardSagaHandler(),
                        AnnounceWinningCardsFoldCountSagaHandler()
                    ) as Set<SagaHandler<*, Saga<*>>>
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
            }.onSuccess { LOGGER.info("Processed $message: ${response?.second?.toList()}") }

            return requireNotNull(response)
        }
    }
}

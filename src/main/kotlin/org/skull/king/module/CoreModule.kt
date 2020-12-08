package org.skull.king.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import org.skull.king.config.PostgresConfig
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
import org.skull.king.domain.core.saga.PlayCardSagaHandler
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.command.CommandHandler
import org.skull.king.infrastructure.cqrs.command.CommandMiddleware
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.cqrs.ddd.event.EventBus
import org.skull.king.infrastructure.cqrs.ddd.event.EventBusMiddleware
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor
import org.skull.king.infrastructure.cqrs.ddd.event.EventStore
import org.skull.king.infrastructure.cqrs.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.infrastructure.cqrs.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.infrastructure.cqrs.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.infrastructure.cqrs.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.infrastructure.cqrs.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.infrastructure.cqrs.query.QueryBus
import org.skull.king.infrastructure.cqrs.query.QueryHandler
import org.skull.king.infrastructure.cqrs.query.QueryMiddleware
import org.skull.king.infrastructure.cqrs.saga.Saga
import org.skull.king.infrastructure.cqrs.saga.SagaHandler
import org.skull.king.infrastructure.cqrs.saga.SagaMiddleware
import org.skull.king.infrastructure.event.FirebaseEventStore
import org.skull.king.infrastructure.event.PostgresEventStore
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.repository.FirebaseQueryRepository
import javax.inject.Singleton

@Module
class CoreModule {

    @Singleton
    @Provides
    fun providePostgresEventStore(config: PostgresConfig, objectMapper: ObjectMapper): PostgresEventStore =
        PostgresEventStore(config.resolveConnection(), objectMapper)

    @Singleton
    @Provides
    fun provideEventStore(database: FirebaseDatabase, objectMapper: ObjectMapper): EventStore =
        FirebaseEventStore(database, objectMapper)

    @Singleton
    @Provides
    fun provideSkullkingRepository(eventStore: EventStore): SkullkingEventSourcedRepository =
        SkullkingEventSourcedRepository(eventStore)

    @Provides
    @Singleton
    fun provideCommandHandler(repository: SkullkingEventSourcedRepository): Set<CommandHandler<*, *>> = setOf(
        AnnounceHandler(repository),
        PlayCardHandler(repository),
        SettleFoldHandler(repository),
        StartHandler(repository)
    )

    @Provides
    @Singleton
    fun provideSagaHandler(repository: SkullkingEventSourcedRepository): Set<SagaHandler<*, *>> = setOf(
        PlayCardSagaHandler(repository)
    )

    @Singleton
    @Provides
    @Suppress("UNCHECKED_CAST")
    fun provideCommandMiddlewares(
        eventStore: EventStore,
        eventBus: EventBus,
        sagaHandlers: Set<@JvmSuppressWildcards SagaHandler<*, *>>
    ): Set<CommandMiddleware> = setOf(
        EventStoreMiddleware(eventStore),
        EventDispatcherMiddleware(eventBus),
        SagaMiddleware(sagaHandlers as Set<SagaHandler<*, Saga<*>>>)
    )

    @Singleton
    @Provides
    fun provideCommandBus(
        middlewares: Set<@JvmSuppressWildcards CommandMiddleware>,
        handlers: Set<@JvmSuppressWildcards CommandHandler<*, *>>
    ): CommandBus = CommandBusSynchronous(middlewares, handlers)

    @Singleton
    @Provides
    @Suppress("UNCHECKED_CAST")
    fun provideEventBus(
        middlewares: Set<@JvmSuppressWildcards EventBusMiddleware>,
        eventCaptors: Set<@JvmSuppressWildcards EventCaptor<*>>
    ): EventBus =
        EventBusSynchronous(middlewares, eventCaptors as Set<EventCaptor<Event>>)

    @Singleton
    @Provides
    fun provideQueryRepository(database: FirebaseDatabase, objectMapper: ObjectMapper): QueryRepository =
        FirebaseQueryRepository(database, objectMapper)

    @Singleton
    @Provides
    fun provideEventBusMiddlewares(): Set<EventBusMiddleware> = setOf()

    @Singleton
    @Provides
    fun provideEventCaptors(repository: QueryRepository): Set<EventCaptor<*>> = setOf(
        OnCardPlayed(repository),
        OnFoldWinnerSettled(repository),
        OnGameFinished(repository),
        OnGameStarted(repository),
        OnNewRoundStarted(repository),
        OnPlayerAnnounced(repository)
    )

    @Singleton
    @Provides
    fun provideQueryBus(
        middlewares: Set<@JvmSuppressWildcards QueryMiddleware>,
        handlers: Set<@JvmSuppressWildcards QueryHandler<*, *>>
    ): QueryBus =
        QueryBusSynchronous(middlewares, handlers)

    @Singleton
    @Provides
    fun provideQueryHandlers(repository: QueryRepository): Set<QueryHandler<*, *>> = setOf(
        GetGameHandler(repository),
        GetPlayerHandler(repository)
    )

    @Singleton
    @Provides
    fun provideQueryMiddleware(): Set<QueryMiddleware> = setOf()
}

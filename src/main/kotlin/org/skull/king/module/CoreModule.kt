package org.skull.king.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
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
import org.skull.king.infrastructure.framework.command.CommandBus
import org.skull.king.infrastructure.framework.command.CommandHandler
import org.skull.king.infrastructure.framework.command.CommandMiddleware
import org.skull.king.infrastructure.framework.ddd.event.Event
import org.skull.king.infrastructure.framework.ddd.event.EventBus
import org.skull.king.infrastructure.framework.ddd.event.EventBusMiddleware
import org.skull.king.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.infrastructure.framework.ddd.event.EventStore
import org.skull.king.infrastructure.framework.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.infrastructure.framework.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.infrastructure.framework.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.infrastructure.framework.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.infrastructure.framework.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.infrastructure.framework.query.QueryBus
import org.skull.king.infrastructure.framework.query.QueryHandler
import org.skull.king.infrastructure.framework.query.QueryMiddleware
import org.skull.king.infrastructure.framework.saga.Saga
import org.skull.king.infrastructure.framework.saga.SagaHandler
import org.skull.king.infrastructure.framework.saga.SagaMiddleware
import org.skull.king.infrastructure.repository.FirebaseQueryRepository

@Module
class CoreModule {

    @Singleton
    @Provides
    fun provideEventStore(): EventStore = InMemoryEventStore()

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
    fun provideSagaHandler(): Set<SagaHandler<*, *>> =
        setOf(PlayCardSagaHandler(), AnnounceWinningCardsFoldCountSagaHandler())

    @Singleton
    @Provides
    @Suppress("UNCHECKED_CAST")
    fun provideCommandMiddlewares(
        eventStore: EventStore,
        eventBus: EventBus,
        sagaHandlers: Set<@JvmSuppressWildcards SagaHandler<*, *>>
    ): Set<CommandMiddleware> = setOf(
        SagaMiddleware(sagaHandlers as Set<SagaHandler<*, Saga<*>>>),
        EventDispatcherMiddleware(eventBus),
        EventStoreMiddleware(eventStore)
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
        ProjectOnFoldSettled(repository),
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

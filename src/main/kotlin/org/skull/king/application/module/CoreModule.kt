package org.skull.king.application.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.application.infrastructure.framework.command.CommandHandler
import org.skull.king.application.infrastructure.framework.command.CommandMiddleware
import org.skull.king.application.infrastructure.framework.ddd.event.*
import org.skull.king.application.infrastructure.framework.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.application.infrastructure.framework.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.application.infrastructure.framework.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.application.infrastructure.framework.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.application.infrastructure.framework.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.application.infrastructure.framework.query.QueryBus
import org.skull.king.application.infrastructure.framework.query.QueryHandler
import org.skull.king.application.infrastructure.framework.query.QueryMiddleware
import org.skull.king.application.infrastructure.framework.saga.Saga
import org.skull.king.application.infrastructure.framework.saga.SagaHandler
import org.skull.king.application.infrastructure.framework.saga.SagaMiddleware
import org.skull.king.core.domain.QueryRepository
import org.skull.king.core.infrastructure.InMemoryEventStore
import org.skull.king.core.infrastructure.SkullkingEventSourcedRepository
import org.skull.king.core.usecases.*
import org.skull.king.core.usecases.captor.*
import org.skull.king.game_room.infrastructure.repository.FirebaseQueryRepository
import javax.inject.Singleton

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

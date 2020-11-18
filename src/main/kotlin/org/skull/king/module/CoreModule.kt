package org.skull.king.module

import dagger.Module
import dagger.Provides
import org.skull.king.core.command.handler.AnnounceHandler
import org.skull.king.core.command.handler.PlayCardHandler
import org.skull.king.core.command.handler.SettleFoldHandler
import org.skull.king.core.command.handler.StartHandler
import org.skull.king.infrastructure.event.EventStoreInMemory
import org.skull.king.core.query.handler.GetGameHandler
import org.skull.king.core.query.handler.GetPlayerHandler
import org.skull.king.core.query.sync.OnCardPlayed
import org.skull.king.core.query.sync.OnFoldWinnerSettled
import org.skull.king.core.query.sync.OnGameFinished
import org.skull.king.core.query.sync.OnGameStarted
import org.skull.king.core.query.sync.OnNewRoundStarted
import org.skull.king.core.query.sync.OnPlayerAnnounced
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.command.CommandMiddleware
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.ddd.event.EventBus
import org.skull.king.cqrs.ddd.event.EventBusMiddleware
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.cqrs.ddd.event.EventStore
import org.skull.king.cqrs.infrastructure.bus.command.CommandBusSynchronous
import org.skull.king.cqrs.infrastructure.bus.event.EventBusSynchronous
import org.skull.king.cqrs.infrastructure.bus.event.EventDispatcherMiddleware
import org.skull.king.cqrs.infrastructure.bus.query.QueryBusSynchronous
import org.skull.king.cqrs.infrastructure.persistence.EventStoreMiddleware
import org.skull.king.cqrs.query.QueryBus
import org.skull.king.cqrs.query.QueryHandler
import org.skull.king.cqrs.query.QueryMiddleware
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepositoryInMemory
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory
import javax.inject.Singleton

@Module
class CoreModule {

    @Singleton
    @Provides
    fun provideEventStore(): EventStore = EventStoreInMemory()

    @Singleton
    @Provides
    fun provideSkullkingRepository(eventStore: EventStore): SkullkingEventSourcedRepositoryInMemory =
        SkullkingEventSourcedRepositoryInMemory(eventStore)

    @Provides
    @Singleton
    fun provideCommandHandler(repository: SkullkingEventSourcedRepositoryInMemory): Set<CommandHandler<*, *>> = setOf(
        AnnounceHandler(repository),
        PlayCardHandler(repository),
        SettleFoldHandler(repository),
        StartHandler(repository)
    )

    @Singleton
    @Provides
    fun provideCommandMiddlewares(eventStore: EventStore, eventBus: EventBus): Set<CommandMiddleware> = setOf(
        EventStoreMiddleware(eventStore),
        EventDispatcherMiddleware(eventBus)
    )

    @Singleton
    @Provides
    fun provideCommandBus(
        middlewares: Set<@JvmSuppressWildcards CommandMiddleware>,
        handlers: Set<@JvmSuppressWildcards CommandHandler<*, *>>
    ): CommandBus = CommandBusSynchronous(middlewares, handlers)

    @Singleton
    @Provides
    fun provideEventBus(
        middlewares: Set<@JvmSuppressWildcards EventBusMiddleware>,
        eventCaptors: Set<@JvmSuppressWildcards EventCaptor<*>>
    ): EventBus =
        EventBusSynchronous(middlewares, eventCaptors as Set<EventCaptor<Event>>)

    @Singleton
    @Provides
    fun provideQueryRepository(): QueryRepositoryInMemory = QueryRepositoryInMemory()

    @Singleton
    @Provides
    fun provideEventBusMiddlewares(): Set<EventBusMiddleware> = setOf()

    @Singleton
    @Provides
    fun provideEventCaptors(repository: QueryRepositoryInMemory): Set<EventCaptor<*>> = setOf(
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
    fun provideQueryHandlers(repository: QueryRepositoryInMemory): Set<QueryHandler<*, *>> = setOf(
        GetGameHandler(repository),
        GetPlayerHandler(repository)
    )

    @Singleton
    @Provides
    fun provideQueryMiddleware(): Set<QueryMiddleware> = setOf()
}

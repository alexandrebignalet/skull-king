package org.skull.king.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import org.skull.king.domain.core.GameLauncher
import org.skull.king.domain.supporting.room.GameRoomService
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.game.StartGameFromGameRoom
import org.skull.king.infrastructure.repository.FirebaseGameRoomRepository
import javax.inject.Singleton

@Module
class GameRoomModule {

    @Singleton
    @Provides
    fun provideGameRoomRepository(database: FirebaseDatabase, objectMapper: ObjectMapper) =
        FirebaseGameRoomRepository(database, objectMapper)

    @Singleton
    @Provides
    fun provideGameLauncher(commandBus: CommandBus): GameLauncher = StartGameFromGameRoom(commandBus)


    @Singleton
    @Provides
    fun provideGameRoomService(gameRoomRepository: FirebaseGameRoomRepository, gameLauncher: GameLauncher) =
        GameRoomService(gameRoomRepository, gameLauncher)
}

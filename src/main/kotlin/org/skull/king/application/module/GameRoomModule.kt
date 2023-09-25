package org.skull.king.application.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.core.domain.GameLauncher
import org.skull.king.core.infrastructure.StartGameFromGameRoom
import org.skull.king.game_room.infrastructure.GameRoomService
import org.skull.king.game_room.infrastructure.repository.FirebaseGameRoomRepository
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

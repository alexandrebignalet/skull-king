package org.skull.king.application.component

import dagger.Component
import org.skull.king.application.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.application.module.ConfigurationModule
import org.skull.king.application.module.CoreModule
import org.skull.king.application.module.FirebaseModule
import org.skull.king.application.module.GameRoomModule
import org.skull.king.core.infrastructure.web.SkullKingResource
import org.skull.king.game_room.infrastructure.web.GameRoomResource
import javax.inject.Singleton

@Singleton
@Component(modules = [CoreModule::class, ConfigurationModule::class, FirebaseModule::class, GameRoomModule::class])
interface SkullKingComponent {

    fun provideSkullKingResource(): SkullKingResource
    fun provideGameRoomResource(): GameRoomResource
    fun provideFirebaseAuthenticator(): FirebaseAuthenticator
}

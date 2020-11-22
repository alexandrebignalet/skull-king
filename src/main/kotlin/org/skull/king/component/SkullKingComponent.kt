package org.skull.king.component

import dagger.Component
import org.skull.king.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.module.ConfigurationModule
import org.skull.king.module.CoreModule
import org.skull.king.module.FirebaseModule
import org.skull.king.web.controller.SkullKingResource
import javax.inject.Singleton

@Singleton
@Component(modules = [CoreModule::class, ConfigurationModule::class, FirebaseModule::class])
interface SkullKingComponent {

    fun provideSkullKingResource(): SkullKingResource
    fun provideFirebaseAuthenticator(): FirebaseAuthenticator
}

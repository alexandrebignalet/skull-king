package org.skull.king.component

import dagger.Component
import org.skull.king.module.ConfigurationModule
import org.skull.king.module.CoreModule
import org.skull.king.resource.GameResource
import org.skull.king.resource.HelloWorldResource
import org.skull.king.resource.SkullKingResource
import javax.inject.Singleton

@Singleton
@Component(modules = [CoreModule::class, ConfigurationModule::class])
interface SkullKingComponent {

    fun provideHelloWorldResource(): HelloWorldResource
    fun provideGameResource(): GameResource
    fun provideSkullKingResource(): SkullKingResource
}

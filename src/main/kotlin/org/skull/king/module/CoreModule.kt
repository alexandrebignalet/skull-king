package org.skull.king.module

import dagger.Module
import dagger.Provides
import org.skull.king.application.Application
import javax.inject.Singleton

@Module
class CoreModule {

    @Singleton
    @Provides
    fun provideSkullKingCore(): Application = Application().also { it.start() }
}

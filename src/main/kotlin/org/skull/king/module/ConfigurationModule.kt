package org.skull.king.module

import dagger.Module
import dagger.Provides
import org.skull.king.config.SkullKingConfig
import javax.inject.Singleton

@Module
class ConfigurationModule(private val configuration: SkullKingConfig) {

    @Provides
    @Singleton
    fun provideConfiguration() = configuration
}

package org.skull.king.module

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.skull.king.config.FirebaseConfig
import org.skull.king.config.SkullKingConfig
import javax.inject.Singleton

@Module
class ConfigurationModule(private val configuration: SkullKingConfig, private val objectMapper: ObjectMapper) {

    @Provides
    @Singleton
    fun provideConfiguration() = configuration

    @Provides
    @Singleton
    fun provideFirebaseConfiguration(): FirebaseConfig = configuration.firebase

    @Provides
    @Singleton
    fun provideObjectMapper(): ObjectMapper = objectMapper
}

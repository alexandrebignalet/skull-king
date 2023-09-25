package org.skull.king.application.module

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.skull.king.application.config.FirebaseConfig
import org.skull.king.application.config.PostgresConfig
import org.skull.king.application.config.SkullKingConfig
import javax.inject.Singleton

@Module
class ConfigurationModule(
    private val configuration: SkullKingConfig,
    private val objectMapper: ObjectMapper
) {

    @Provides
    @Singleton
    fun provideConfiguration() = configuration

    @Provides
    @Singleton
    fun provideFirebaseConfiguration(): FirebaseConfig = configuration.firebase

    @Provides
    @Singleton
    fun provideObjectMapper(): ObjectMapper = objectMapper

    @Provides
    @Singleton
    fun providePostgresConfig(): PostgresConfig = configuration.postgres
}

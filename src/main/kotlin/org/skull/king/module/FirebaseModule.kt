package org.skull.king.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import org.skull.king.config.FirebaseConfig
import javax.inject.Singleton

@Module
class FirebaseModule {

    @Singleton
    @Provides
    fun provideFirebaseOptions(config: FirebaseConfig, objectMapper: ObjectMapper): FirebaseOptions =
        objectMapper.writeValueAsString(config.serviceAccount).let {
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(it.byteInputStream()))
                .setDatabaseUrl(config.databaseURL)
                .build()
        }

    @Singleton
    @Provides
    fun provideFirebaseDatabase(options: FirebaseOptions): FirebaseDatabase =
        kotlin.runCatching { FirebaseApp.initializeApp(options).let { FirebaseDatabase.getInstance() } }
            .getOrElse { FirebaseDatabase.getInstance() }
}

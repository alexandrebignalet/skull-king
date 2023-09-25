package org.skull.king.application.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import org.skull.king.application.config.FirebaseConfig
import org.skull.king.application.infrastructure.authentication.FirebaseAuthenticator
import javax.inject.Singleton

@Module
class FirebaseModule {

    @Singleton
    @Provides
    fun provideFirebaseOptions(config: FirebaseConfig, objectMapper: ObjectMapper): FirebaseOptions =
        objectMapper.writeValueAsString(config.serviceAccount).let {
            FirebaseOptions.builder()
                .setProjectId(config.serviceAccount.projectId)
                .setCredentials(GoogleCredentials.fromStream(it.byteInputStream()))
                .setDatabaseUrl(config.databaseURL)
                .build()
        }

    @Singleton
    @Provides
    fun provideFirebaseDatabase(options: FirebaseOptions): FirebaseDatabase =
        kotlin.runCatching { FirebaseApp.initializeApp(options).let { FirebaseDatabase.getInstance() } }
            .getOrElse { FirebaseDatabase.getInstance() }

    @Singleton
    @Provides
    fun provideFirebaseAuth(options: FirebaseOptions): FirebaseAuth =
        kotlin
            .runCatching {
                FirebaseApp.initializeApp(options)
                    .let { FirebaseAuth.getInstance() }
            }
            .getOrElse { FirebaseAuth.getInstance() }

    @Singleton
    @Provides
    fun provideFirebaseAuthenticator(firebaseAuth: FirebaseAuth) =
        FirebaseAuthenticator(firebaseAuth)
}

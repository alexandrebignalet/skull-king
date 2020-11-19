package org.skull.king.helpers

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import org.skull.king.config.FirebaseConfig
import org.skull.king.utils.JsonObjectMapper
import kotlin.coroutines.suspendCoroutine

open class LocalFirebase {

    companion object {
        private val objectMapper = JsonObjectMapper.getObjectMapper()
        private const val GAMES_PATH = "games"
        private const val PLAYERS_PATH = "players"
        private const val EVENTS_PATH = "events"

        private val firebaseConfig = FirebaseConfig().apply {
            credentialsPath = "/service-account-file.json"
            databaseURL = "http://localhost:9000/?ns=skullking"
        }

        private val serviceAccount = LocalFirebase::class.java.getResourceAsStream(firebaseConfig.credentialsPath)

        private val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl(firebaseConfig.databaseURL)
            .build()

        internal val database: FirebaseDatabase =
            FirebaseApp.initializeApp(options).let { FirebaseDatabase.getInstance() }
    }

    fun clearFirebaseData() {
        runBlocking {
            clearRefData(GAMES_PATH)
            clearRefData(PLAYERS_PATH)
            clearRefData(EVENTS_PATH)
        }
    }

    private suspend fun clearRefData(path: String): Unit = suspendCoroutine { cont ->
        database.getReference(path).removeValue { error, ref ->
            error?.let {
                cont.resumeWith(Result.failure(Error(error.message)))
            } ?: cont.resumeWith(Result.success(Unit))
        }
    }
}

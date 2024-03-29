package org.skull.king.helpers

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.skull.king.application.config.FirebaseConfig
import org.skull.king.application.utils.JsonObjectMapper
import kotlin.coroutines.suspendCoroutine

open class LocalFirebase {

    companion object {
        val objectMapper = JsonObjectMapper.getObjectMapper()
        private const val GAMES_PATH = "games"
        private const val PLAYERS_PATH = "players"
        private const val EVENTS_PATH = "events"
        private const val USERS_PATH = "users"
        private const val GAME_ROOMS_PATH = "game_rooms"
        private val serviceAccountJson: String = """{
          "type": "service_account",
          "project_id": "skullking",
          "private_key_id": "5f134f611d6093a1ac80cb69983fac030797e629",
          "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC87tYtPEX398g6\neauVKuPdawiJFyPq2ClFsT83LcDMOTNkgwazHYlBkyBaZU++t8PSa3TWgcHfcP7g\n1ydlFNmUIepnLgBtQ+56FVDlLRQdSsDURALSHIHHlNW7wE3Rn2D4dgogxNx5S+RA\nH351vuiSnh6JwkCnA8Neer0Rv1SiXuQO20c3P2/TjGrxVkP5MONZNhsMrp4MBxIB\n9XI2LBJp8qY+IjXdLuW0CYT6jDu4BK49wpBjb5PWUjkifTvPtNuNWJH8JXyET4h6\nm9C8xBw1qiCuZrnQO87nklLrGzsf38AivaeFlmtQLVDZTTC+JyI5oqETf8GVVzuO\n61OuDKCrAgMBAAECggEAA9lW1nhqNdSQ6RkgQaxwnSig/hZAIavvJ5UMwizztdNB\nkHEbtRmC4CEv6+EgGNcqfnNBTrJ9om06J0GJyIUxfH3x/Y1US/ag7KYNDi4udEGD\nrRfmaK2IIS7TpTRHZddkxvFwJJVK633ztduDunygCYbCuSxn25SEpWcZQcRNW9Rc\nm02X1b+rsLfkq6+HfrKXlgTxe82hqKIgDAZsMcBgTZjEOkRDMx7QFKnWS9J0oBzv\naBUOyEfBWxiqQvWN2MPjQDgLXbHbIe9+L2q4ZlCnyOEGb4dfD1qw8KwEhPfLzUvt\nPfyX1MB25/2+yONsccFBL3dQPfyFV3ziLTGf/A3t0QKBgQDtkVFiOI5otXt/9heN\nih4sX1Pp0N+ahWTwLaneROppDWh0zin4r6Zb9nVjWhaAod3l8guM9KLQOZt98ayH\nWDTLqtMtkL8BwHPQ5ZDBCHDwXiXE3M8c1hvk/aQDrfAz73FRdbY4JK/S4V5RV8YU\nQeMVMryu4bq6yLDh/HCuXDh9EwKBgQDLl4NjHt75apzAHLXQLFSbRhHJzlCuzWBJ\nHsRVLyFwR2jMx5qKob0m0CRkcZG6ivsEVT0NvBvrs6RbkjQ3q+wMLtdiOEPfFe5u\n98uUPeupvSg5Z1P0zwscYxtLLUHJgejTeL2plsL/RanyDVS/VDLRg052bO9ti6HG\nLB1V/M05CQKBgQCi4+jxd+XtVcKAUrSDkBhwREy9HUK0KJK1PBolFQvFTDQ7IsEU\nixt+ItcKcFLNkC8d44CX/YVFULqU+IhbpNdObqqtq3nMMbE3orBGKwuFRIiRGvXH\nx+cIdAFppHH2qk8Ak72FcWI9LdoF3DEs4qBZgJhvMMdgGwttaDG62/C/kQKBgHl8\nkO9d5YoJvz24JBnzygzZ1BWLIoQck38ud9OpCxgX1IaV+TcOanO1snGpf19EPaOJ\nRaRjgnm0ubfW14f89B8U+Htovb9qM9xNy1JLXMvtzwnqCaRExAmNbiT+/YnEFm3S\nV8LR7swrAs0ofVCqaqSw6Oor+PdyYfeCLYM5FjEZAoGAXrD9D3/UbS7q7kIjZ6uq\n+/4e6EvodXtYYguiP5WHTFylGzAb86fpvjup6GExXB5G1ww9Yv+z1JBnYpMSvHWI\nJYUfF8DD0m8DJkhwPLiDGDnr45c92U+U3/ZHLy3EqalAHvLAC+Eo+FgkdE+LSfIz\nve1ipLrpcpONkRjFK6E3szY=\n-----END PRIVATE KEY-----\n",
          "client_email": "firebase-adminsdk-exqh6@skullking.iam.gserviceaccount.com",
          "client_id": "106170568360149190763",
          "auth_uri": "https://accounts.google.com/o/oauth2/auth",
          "token_uri": "https://oauth2.googleapis.com/token",
          "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
          "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-exqh6%40skullking.iam.gserviceaccount.com"
        }""".trimIndent()

        private val firebaseConfig = FirebaseConfig().apply {
            serviceAccount =
                objectMapper.readValue(serviceAccountJson, FirebaseConfig.FirebaseServiceAccount::class.java)
            databaseURL = "http://localhost:9000/?ns=skullking"
        }

        private val serviceAccount = objectMapper.writeValueAsString(firebaseConfig.serviceAccount).byteInputStream()
        private val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl(firebaseConfig.databaseURL)
            .build()

        val database: FirebaseDatabase =
            FirebaseApp.initializeApp(options).let { FirebaseDatabase.getInstance() }
    }

    @AfterEach
    fun tearDown() {
        clearFirebaseData()
    }

    fun clearFirebaseData() {
        runBlocking {
            clearRefData(GAMES_PATH)
            clearRefData(PLAYERS_PATH)
            clearRefData(EVENTS_PATH)
            clearRefData(USERS_PATH)
            clearRefData(GAME_ROOMS_PATH)
        }
    }

    private suspend fun clearRefData(path: String): Unit = suspendCoroutine { cont ->
        database.getReference(path).removeValue { error, _ ->
            error?.let {
                cont.resumeWith(Result.failure(Error(error.message)))
            } ?: cont.resumeWith(Result.success(Unit))
        }
    }
}

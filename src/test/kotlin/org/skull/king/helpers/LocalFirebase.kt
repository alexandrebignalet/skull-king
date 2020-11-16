package org.skull.king.helpers

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.skull.king.config.FirebaseConfig
import org.skull.king.query.ReadSkullKing
import org.skull.king.utils.JsonObjectMapper
import kotlin.coroutines.suspendCoroutine

open class LocalFirebase {

    companion object {
        private val objectMapper = JsonObjectMapper.getObjectMapper()
        private const val GAME_PATH = "games"
    }

    private val firebaseConfig = FirebaseConfig().apply {
        credentialsPath = "/service-account-file.json"
        databaseURL = "http://localhost:9000/?ns=skullking"
    }

    private val serviceAccount = LocalFirebase::class.java.getResourceAsStream(firebaseConfig.credentialsPath)

    private val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setDatabaseUrl(firebaseConfig.databaseURL)
        .build()

    internal val database: FirebaseDatabase = FirebaseApp.initializeApp(options).let { FirebaseDatabase.getInstance() }

    suspend fun getGame(gameId: String): ReadSkullKing = suspendCoroutine { cont ->
        val gamesRef: DatabaseReference = database.reference.child("$GAME_PATH/${gameId}")

        gamesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val json = objectMapper.writeValueAsString(snapshot.value)
                val game = objectMapper.readValue<ReadSkullKing>(json)
                cont.resumeWith(Result.success(game))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }
}

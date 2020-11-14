package org.skull.king.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.slf4j.LoggerFactory
import kotlin.coroutines.suspendCoroutine

class FirebaseRepository {
    companion object {
        private val logger = LoggerFactory.getLogger(FirebaseRepository::class.java)
    }

    private val serviceAccount = javaClass.getResourceAsStream("/service-account-file.json")

    private val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setDatabaseUrl("http://localhost:9000/?ns=skullking")
        .build()

    private var database: FirebaseDatabase

    init {

        FirebaseApp.initializeApp(options)

        database = FirebaseDatabase.getInstance()
    }

    fun add(game: ReadSkullKing) {
        val gamesRef: DatabaseReference = database.reference.child("games")

        val games: MutableMap<String, ReadSkullKing> = HashMap<String, ReadSkullKing>()
        games[game.id] = game

        gamesRef.setValue(games) { databaseError, _ ->
            if (databaseError != null) {
                logger.error("Data could not be saved " + databaseError.message)
            }
        }
    }

    suspend fun get(gameId: String): ReadSkullKing = suspendCoroutine { cont ->
        val gamesRef: DatabaseReference = database.reference.child("games/${gameId}")

        gamesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val objectMapper = ObjectMapper().registerKotlinModule()
                cont.resumeWith(Result.success(snapshot.getValue(ReadSkullKing::class.java)))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }

    fun ReadSkullKing.fireMap() = mapOf(
        "id" to id,
        "players" to players,
        "round_nb" to roundNb,
        "fold" to fold.toMap(),
        "is_ended" to isEnded,
        "first_player_id" to firstPlayerId
    )
}

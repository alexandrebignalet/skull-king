package org.skull.king.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.domain.core.query.ReadSkullKing
import org.slf4j.LoggerFactory
import kotlin.coroutines.suspendCoroutine

class FirebaseQueryRepository(
    private val database: FirebaseDatabase,
    private val objectMapper: ObjectMapper
) : QueryRepository {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FirebaseQueryRepository::class.java)
        private const val GAME_PATH = "games"
        private const val PLAYERS_PATH = "players"
    }

    override fun getGame(gameId: String): ReadSkullKing? {
        return runBlocking { retrieveGameItem(gameId) }
    }

    override fun getGamePlayers(gameId: String): List<ReadPlayer> {
        return getGame(gameId)?.let {
            val players = it.players.mapNotNull { playerId -> getPlayer(gameId, playerId) }

            if (players.size != it.players.size) listOf()
            else players
        } ?: listOf()
    }

    override fun addGame(game: ReadSkullKing) {
        runBlocking { persistGame(game) }
    }

    override fun getPlayer(gameId: String, playerId: String): ReadPlayer? {
        return runBlocking { retrievePlayerItem(gameId, playerId) }
    }

    override fun addPlayer(player: ReadPlayer) {
        runBlocking { persistPlayer(player) }
    }

    private suspend fun retrieveGameItem(gameId: String): ReadSkullKing? = suspendCoroutine { cont ->
        val gamesRef: DatabaseReference = database.reference.child("$GAME_PATH/$gameId")

        gamesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                snapshot?.value?.let {
                    val json = objectMapper.writeValueAsString(snapshot.value)
                    val game = objectMapper.readValue<ReadSkullKing>(json)
                    cont.resumeWith(Result.success(game))
                } ?: cont.resumeWith(Result.success(null))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }

    private suspend fun retrievePlayerItem(gameId: String, playerId: String): ReadPlayer? = suspendCoroutine { cont ->
        val playersRef: DatabaseReference = database.reference.child("$PLAYERS_PATH/${gameId}_$playerId")

        playersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                snapshot?.value?.let {
                    val json = objectMapper.writeValueAsString(snapshot.value)
                    val player = objectMapper.readValue<ReadPlayer>(json)
                    cont.resumeWith(Result.success(player))
                } ?: cont.resumeWith(Result.success(null))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }

    private suspend fun persistPlayer(record: ReadPlayer): Unit = suspendCoroutine { cont ->
        val playersRef = database.reference.child("$PLAYERS_PATH/${record.gameId}_${record.id}")
        playersRef.setValue(record.fireMap()) { databaseError, _ ->
            databaseError?.let {
                LOGGER.error("Data could not be saved " + databaseError.message)
                cont.resumeWith(Result.failure(Error(databaseError.message)))
            } ?: cont.resumeWith(Result.success(Unit))
        }
    }

    private suspend fun persistGame(record: ReadSkullKing): Unit = suspendCoroutine { cont ->
        val gamesRef = database.reference.child("$GAME_PATH/${record.id}")
        gamesRef.setValue(record.fireMap()) { databaseError, _ ->
            databaseError?.let {
                LOGGER.error("Data could not be saved " + databaseError.message)
                cont.resumeWith(Result.failure(Error(databaseError.message)))
            } ?: cont.resumeWith(Result.success(Unit))
        }
    }
}

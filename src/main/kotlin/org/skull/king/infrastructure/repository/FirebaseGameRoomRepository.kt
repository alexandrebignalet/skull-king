package org.skull.king.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import org.skull.king.domain.supporting.room.domain.GameRoom
import org.skull.king.domain.supporting.room.domain.GameRoomRepository
import org.slf4j.LoggerFactory
import kotlin.coroutines.suspendCoroutine

class FirebaseGameRoomRepository(private val database: FirebaseDatabase, private val objectMapper: ObjectMapper) :
    GameRoomRepository {

    companion object {
        private const val PATH: String = "game_rooms"
        private const val USER_PATH: String = "users"

        private val LOGGER = LoggerFactory.getLogger(FirebaseGameRoomRepository::class.java)
    }

    override fun save(gameRoom: GameRoom) {
        runBlocking { saveGameRoomAndUsers(gameRoom) }
    }

    override fun findOne(gameRoomId: String): GameRoom? = runBlocking { fetch(gameRoomId) }

    override fun remove(gameRoomId: String) {
        runBlocking { removeGameRoom(gameRoomId) }
    }

    fun kick(gameRoom: GameRoom, kicked: String) {
        runBlocking { kickUserFromGameRoom(gameRoom, kicked) }
    }

    private suspend fun saveGameRoomAndUsers(gameRoom: GameRoom) = suspendCoroutine<Unit> { cont ->
        val ref = database.reference
        val gameRoomPayload = gameRoom.fireMap()
        val userUpdate = gameRoom.users.fold(mapOf<String, Any?>()) { acc, user ->
            acc + mapOf(
                "$USER_PATH/${user.id}/rooms/${gameRoom.id}" to gameRoomPayload,
                "$USER_PATH/${user.id}/id" to user.id,
                "$USER_PATH/${user.id}/name" to user.name
            )
        }
        val update = mapOf("$PATH/${gameRoom.id}" to gameRoomPayload) + userUpdate

        ref.updateChildren(update) { databaseError, _ ->
            databaseError?.let {
                LOGGER.error("Data could not be saved " + databaseError.message)
                cont.resumeWith(Result.failure(Error(databaseError.message)))
            } ?: cont.resumeWith(Result.success(Unit))
        }
    }

    private suspend fun fetch(id: String): GameRoom? = suspendCoroutine { cont ->
        val gameRoomRef = database.reference.child("$PATH/$id")

        gameRoomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                snapshot?.value?.let {
                    val json = objectMapper.writeValueAsString(snapshot.value)
                    val gameRoom = objectMapper.readValue<GameRoom>(json)
                    cont.resumeWith(Result.success(gameRoom))
                } ?: cont.resumeWith(Result.success(null))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }

    private suspend fun removeGameRoom(gameRoomId: String): Unit = suspendCoroutine { cont ->
        database.getReference("$PATH/$gameRoomId").removeValue { error, _ ->
            error?.let {
                cont.resumeWith(Result.failure(Error(error.message)))
            } ?: cont.resumeWith(Result.success(Unit))
        }
    }

    private suspend fun kickUserFromGameRoom(gameRoom: GameRoom, kicked: String): Unit = suspendCoroutine { cont ->
        val newUsers = gameRoom.users.filter { it.id != kicked }

        val userUpdate = mapOf("$USER_PATH/$kicked/rooms/${gameRoom.id}" to null)

        val gameRoomUpdate =
            if (newUsers.count() == 0) mapOf("$PATH/${gameRoom.id}" to null)
            else mapOf(
                "$PATH/${gameRoom.id}/creator" to if (kicked == gameRoom.creator) newUsers.random().id else gameRoom.creator,
                "$PATH/${gameRoom.id}/users" to newUsers.map { it.fireRelationMap() }
            )

        val finalUpdate = userUpdate + gameRoomUpdate
        val ref = database.reference
        ref.updateChildren(finalUpdate) { error, _ ->
            error?.let { cont.resumeWith(Result.failure(Error(error.message))) }
                ?: cont.resumeWith(Result.success(Unit))
        }
    }
}

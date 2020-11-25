package org.skull.king.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import org.skull.king.domain.supporting.user.domain.GameUser
import org.skull.king.domain.supporting.user.domain.UserRepository
import org.slf4j.LoggerFactory
import kotlin.coroutines.suspendCoroutine

class FirebaseUserRepository(private val database: FirebaseDatabase, private val objectMapper: ObjectMapper) :
    UserRepository {

    companion object {
        private const val PATH: String = "users"
        private val LOGGER = LoggerFactory.getLogger(FirebaseUserRepository::class.java)
    }

    override fun findOne(id: String): GameUser? = runBlocking { fetch(id) }

    private suspend fun fetch(id: String): GameUser? = suspendCoroutine { cont ->
        val usersRef = database.reference.child("$PATH/$id")

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                snapshot?.value?.let {
                    val json = objectMapper.writeValueAsString(snapshot.value)
                    val user = objectMapper.readValue(json, GameUser::class.java)
                    cont.resumeWith(Result.success(user))
                } ?: cont.resumeWith(Result.success(null))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }
}

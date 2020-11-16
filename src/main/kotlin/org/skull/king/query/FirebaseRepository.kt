package org.skull.king.query

import com.google.firebase.database.FirebaseDatabase
import org.slf4j.LoggerFactory

class FirebaseRepository(private val database: FirebaseDatabase) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FirebaseRepository::class.java)
        private const val GAME_PATH = "games"
    }

    fun add(game: ReadSkullKing) {
        val gamesRef = database.reference.child(GAME_PATH)

        gamesRef.setValue(game.fireMap()) { databaseError, _ ->
            databaseError?.let {
                LOGGER.error("Data could not be saved " + databaseError.message)
            }
        }
    }
}

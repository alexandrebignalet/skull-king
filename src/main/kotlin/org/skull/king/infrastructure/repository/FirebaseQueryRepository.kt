package org.skull.king.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.runBlocking
import org.skull.king.domain.core.query.Play
import org.skull.king.domain.core.query.PlayerRoundScore
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.domain.core.query.ReadSkullKing
import org.skull.king.domain.core.query.RoundNb
import org.skull.king.domain.core.query.Score
import org.skull.king.domain.core.query.SkullKingPhase

class FirebaseQueryRepository(
    private val database: FirebaseDatabase,
    private val objectMapper: ObjectMapper
) : QueryRepository {

    companion object {
        private const val GAME_PATH = "games"
        private const val PLAYERS_PATH = "players"
    }

    override fun getGame(gameId: String): ReadSkullKing? = runBlocking { retrieveItem("$GAME_PATH/$gameId") }

    override fun getGamePlayers(gameId: String): List<ReadPlayer> {
        return getGame(gameId)?.let {
            val players = it.players.mapNotNull { playerId -> getPlayer(gameId, playerId) }

            if (players.size != it.players.size) listOf()
            else players
        } ?: listOf()
    }

    override fun getPlayer(gameId: String, playerId: String): ReadPlayer? {
        return runBlocking { retrieveItem("$PLAYERS_PATH/${gameId}_$playerId") }
    }

    override fun registerPlayerAnnounce(
        gameId: String,
        playerScore: PlayerRoundScore,
        isLastAnnounce: Boolean
    ): Result<Unit> = runBlocking {
        val playerScoreUpdate = mapOf(
            "$GAME_PATH/${gameId}/score_board/${playerScore.playerId}_${playerScore.roundNb}" to playerScore.fireMap()
        )

        val gameUpdate =
            playerScoreUpdate + if (isLastAnnounce) mapOf("$GAME_PATH/${gameId}/phase" to SkullKingPhase.CARDS) else mapOf()

        multiPathUpdate(gameUpdate)
    }


    override fun projectFoldSettled(
        gameId: String,
        playerId: String,
        roundNb: RoundNb,
        score: Score,
        butinAllies: List<Pair<String, Score>>
    ): Result<Unit> = runBlocking {
        multiPathUpdate(
            mapOf(
                "$GAME_PATH/${gameId}/score_board/${playerId}_${roundNb}/score/done" to score.done,
                "$GAME_PATH/${gameId}/score_board/${playerId}_${roundNb}/score/potential_bonus" to score.potentialBonus,
                "$GAME_PATH/${gameId}/fold" to null,
                "$GAME_PATH/${gameId}/current_player_id" to playerId,
                *(butinAllies.map {
                    "$GAME_PATH/${gameId}/score_board/${it.first}_${roundNb}/score/potential_bonus" to it.second.potentialBonus
                }.toTypedArray())
            )
        )
    }

    override fun movePlayerCardToGameFold(
        gameId: String,
        playerId: String,
        card: ReadCard,
        nextPlayerId: String
    ) = runBlocking {
        multiPathUpdate(
            mapOf(
                "$GAME_PATH/${gameId}/fold/${playerId}_${card.id}" to Play(playerId, card).fireMap(),
                "$GAME_PATH/${gameId}/current_player_id" to nextPlayerId,
                "$PLAYERS_PATH/${gameId}_${playerId}/cards/${card.id}" to null
            )
        )
    }

    override fun saveGameAndPlayers(game: ReadSkullKing, players: List<ReadPlayer>): Result<Unit> = runBlocking {
        val playersPayload = players.associate { player ->
            "$PLAYERS_PATH/${game.id}_${player.id}" to player.fireMap()
        }

        multiPathUpdate(
            playersPayload + mapOf(
                "$GAME_PATH/${game.id}" to game.fireMap()
            )
        )
    }

    override fun endGame(gameId: String): Result<Unit> = runBlocking {
        multiPathUpdate(mapOf("$GAME_PATH/${gameId}/is_ended" to true))
    }

    override fun saveNewRound(
        gameId: String,
        nextRoundNb: Int,
        newFirstPlayerId: String,
        players: List<ReadPlayer>
    ): Result<Unit> = runBlocking {
        val gameUpdate = mapOf(
            "$GAME_PATH/${gameId}/round_nb" to nextRoundNb,
            "$GAME_PATH/${gameId}/phase" to SkullKingPhase.ANNOUNCEMENT,
            "$GAME_PATH/${gameId}/current_player_id" to newFirstPlayerId
        )

        val playersUpdate = players.associate { player ->
            "$PLAYERS_PATH/${gameId}_${player.id}" to player.fireMap()
        }

        multiPathUpdate(gameUpdate + playersUpdate)
    }

    private suspend fun multiPathUpdate(payload: Map<String, Any?>): Result<Unit> = suspendCoroutine { cont ->
        val ref = database.reference
        ref.updateChildren(payload) { error, _ ->
            error?.let { cont.resume(Result.failure(Error(error.message))) }
                ?: cont.resume(Result.success(Unit))
        }
    }

    private suspend inline fun <reified T> retrieveItem(path: String): T? = suspendCoroutine { cont ->
        val ref: DatabaseReference = database.reference.child(path)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                snapshot?.value?.let {
                    runCatching {
                        val json = objectMapper.writeValueAsString(snapshot.value)
                        objectMapper.readValue<T>(json)
                    }
                        .onSuccess { cont.resumeWith(Result.success(it)) }
                        .onFailure { cont.resumeWith(Result.failure(it)) }
                } ?: cont.resumeWith(Result.success(null))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }
}

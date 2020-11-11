package org.skull.king.query

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.skull.king.application.createActor
import org.skull.king.command.domain.NewPlayer
import org.skull.king.event.CardPlayed
import org.skull.king.event.Event
import org.skull.king.event.FoldWinnerSettled
import org.skull.king.event.GameFinished
import org.skull.king.event.NewRoundStarted
import org.skull.king.event.PlayerAnnounced
import org.skull.king.event.SkullKingEvent
import org.skull.king.event.Started

data class QueryMsg(val query: Query, val response: CompletableDeferred<List<ReadEntity>>) // a request with reply

class QueryHandler {

    private val queryChannel = createActor { qm: QueryMsg -> qm.response.complete(processQuery(qm.query)) }

    val eventChannel = createActor { e: Event -> processEvent(e) }

    private val skullKingGames = mutableMapOf<String, ReadSkullKing>()
    private val players = mutableMapOf<String, ReadPlayer>()

    private fun processEvent(e: Event): Any? {
        return when (e) {
            is SkullKingEvent -> when (e) {
                is Started -> {
                    skullKingGames[e.gameId] =
                        ReadSkullKing(e.gameId, e.players.map { it.id }, 1, firstPlayerId = e.players.first().id)

                    for (player in e.players) {
                        player as NewPlayer
                        players[buildPlayerId(e.gameId, player.id)] = ReadPlayer(player.id, e.gameId, player.cards)
                    }
                }
                is PlayerAnnounced -> {
                    players[buildPlayerId(e.gameId, e.playerId)]?.let {
                        it.scorePerRound[e.roundNb] = Score(e.count)
                        players[buildPlayerId(e.gameId, e.playerId)] = it
                    }
                }
                is CardPlayed -> {
                    skullKingGames[e.gameId]?.let { game ->
                        val readPlayerId = buildPlayerId(game.id, e.playerId)
                        players[readPlayerId]?.let { player ->
                            val cardsUpdate = player.cards.filterNot { it == e.card }
                            players[readPlayerId] = ReadPlayer(e.playerId, game.id, cardsUpdate, player.scorePerRound)
                        }

                        val foldUpdate = game.fold.let {
                            val fold = it.toMutableMap()
                            fold[e.playerId] = e.card
                            fold
                        }
                        skullKingGames[e.gameId] = game.copy(fold = foldUpdate)
                    }
                }
                is FoldWinnerSettled -> {
                    val gamePlayers = players
                        .filter { it.value.gameId == e.gameId }

                    gamePlayers
                        .filter { (_, player) -> player.id == e.winner }
                        .forEach { (readPlayerId, player) ->
                            skullKingGames[player.gameId]?.let { game ->
                                player.scorePerRound[game.roundNb]?.let { (announced, done, potentialBonus) ->
                                    player.scorePerRound[game.roundNb] =
                                        Score(announced, done + 1, potentialBonus + e.potentialBonus)
                                    players[readPlayerId] = player
                                }
                            }

                            Unit
                        }

                    skullKingGames[e.gameId]?.let {
                        skullKingGames[e.gameId] = it.copy(firstPlayerId = e.winner)
                    }
                }
                is NewRoundStarted -> {
                    val gamePlayers = players.values.filter { it.gameId == e.gameId }
                    skullKingGames[e.gameId]?.let { game ->
                        skullKingGames[e.gameId] =
                            ReadSkullKing(
                                e.gameId,
                                gamePlayers.map { it.id },
                                e.nextRoundNb,
                                firstPlayerId = gamePlayers.first().id
                            )

                        e.players.forEach { player ->
                            players[buildPlayerId(game.id, player.id)]?.let {
                                players[buildPlayerId(game.id, player.id)] = ReadPlayer(
                                    player.id,
                                    game.id,
                                    player.cards,
                                    it.scorePerRound
                                )
                            }
                        }
                    }
                }
                is GameFinished -> skullKingGames[e.gameId]?.let { game ->
                    skullKingGames[e.gameId] = game.copy(isEnded = true)
                }
            }
        }
    }

    private fun processQuery(q: Query): List<ReadEntity> {
        println("Processing $q")
        return when (q) {
            is GetGame -> skullKingGames[q.gameId]?.run { listOf(this) } ?: emptyList()
            is GetPlayer -> players[buildPlayerId(q.gameId, q.playerId)]?.run { listOf(this) } ?: emptyList()
        }
    }


    fun handle(q: Query): List<ReadEntity> {

        val msg = QueryMsg(q, CompletableDeferred())

        runBlocking {
            queryChannel.send(msg)

            msg.response.await()

        }
        return msg.response.getCompleted()

    }

}

fun buildPlayerId(gameId: String, playerId: String) = "${gameId}_${playerId}"

package org.skull.king.query

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.skull.king.application.createActor
import org.skull.king.command.NewPlayer
import org.skull.king.eventStore.CardPlayed
import org.skull.king.eventStore.Event
import org.skull.king.eventStore.FoldWinnerSettled
import org.skull.king.eventStore.GameFinished
import org.skull.king.eventStore.NewRoundStarted
import org.skull.king.eventStore.PlayerAnnounced
import org.skull.king.eventStore.SkullKingEvent
import org.skull.king.eventStore.Started

data class QueryMsg(val query: Query, val response: CompletableDeferred<List<ReadEntity>>) // a request with reply

class QueryHandler {

    val queryChannel = createActor { qm: QueryMsg -> qm.response.complete(processQuery(qm.query)) }

    val eventChannel = createActor { e: Event -> processEvent(e) }

    private val skullKingGames = mutableMapOf<String, ReadSkullKing>()
    private val players = mutableMapOf<String, ReadPlayer>()

    private fun processEvent(e: Event): Any? {
        return when (e) {
            is SkullKingEvent -> when (e) {
                is Started -> {
                    skullKingGames[e.gameId] = ReadSkullKing(e.gameId, e.players.map { it.id }, 1)

                    for (player in e.players) {
                        player as NewPlayer
                        players[buildPlayerId(e.gameId, player.id)] = ReadPlayer(player.id, e.gameId, player.cards)
                    }
                }
                is PlayerAnnounced -> {
                    players[buildPlayerId(e.gameId, e.playerId)]?.let {
                        it.score[e.roundNb] = Pair(e.count, 0)
                        players[buildPlayerId(e.gameId, e.playerId)] = it
                    }
                }
                is CardPlayed -> {
                    skullKingGames[e.gameId]?.let { game ->
                        val readPlayerId = buildPlayerId(game.id, e.playerId)
                        players[readPlayerId]?.let { player ->
                            val cardsUpdate = player.cards.filterNot { it == e.card }
                            players[readPlayerId] = ReadPlayer(e.playerId, game.id, cardsUpdate, player.score)
                        }

                        val foldUpdate = game.fold.let {
                            val fold = it.toMutableMap()
                            fold[e.playerId] = e.card
                            fold
                        }
                        skullKingGames[e.gameId] = ReadSkullKing(game.id, game.players, game.roundNb, foldUpdate)
                    }
                }
                is FoldWinnerSettled -> {
                    val gamePlayers = players
                        .filter { it.value.gameId == e.gameId }

                    gamePlayers
                        .forEach { (readPlayerId, player) ->
                            if (e.winner != player.id) Unit
                            else {
                                skullKingGames[player.gameId]?.let { game ->
                                    player.score[game.roundNb]?.let { (announced, done) ->
                                        player.score[game.roundNb] = Pair(announced, done + 1)
                                        players[readPlayerId] = player
                                    }
                                }

                                Unit
                            }
                        }
                }
                is NewRoundStarted -> {
                    val gamePlayers = players.values.filter { it.gameId == e.gameId }
                    skullKingGames[e.gameId]?.let { game ->
                        skullKingGames[e.gameId] = ReadSkullKing(e.gameId, gamePlayers.map { it.id }, e.nextRoundNb)

                        e.players.forEach { player ->
                            players[buildPlayerId(game.id, player.id)]?.let {
                                players[buildPlayerId(game.id, player.id)] = ReadPlayer(
                                    player.id,
                                    game.id,
                                    player.cards,
                                    it.score
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

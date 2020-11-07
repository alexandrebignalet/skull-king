package org.skull.king.command

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.skull.king.application.createActor
import org.skull.king.command.SkullKing.Companion.FIRST_ROUND_NB
import org.skull.king.command.SkullKing.Companion.MAX_PLAYERS
import org.skull.king.command.SkullKing.Companion.MIN_PLAYERS
import org.skull.king.command.SkullKing.Companion.NEXT_FIRST_PLAYER_INDEX
import org.skull.king.eventStore.CardPlayed
import org.skull.king.eventStore.Event
import org.skull.king.eventStore.EventStore
import org.skull.king.eventStore.FoldWinnerSettled
import org.skull.king.eventStore.NewRoundStarted
import org.skull.king.eventStore.PlayerAnnounced
import org.skull.king.eventStore.SkullKingEvent
import org.skull.king.eventStore.Started
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid
import org.skull.king.functional.Validated

typealias CmdResult = Validated<DomainError, List<Event>>
typealias EsScope = EventStore.() -> CmdResult

data class CommandMsg(val command: Command, val response: CompletableDeferred<CmdResult>) // a command with a result


class CommandHandler(val eventStore: EventStore) {

    //if we need we can have multiple instances
    val sendChannel = createActor<CommandMsg> { executeCommand(it) }

    private fun executeCommand(msg: CommandMsg) {

        val res = processPoly(msg.command)(eventStore)

        runBlocking {
            //we want to reply after sending the event to the store
            if (res is Valid) {
                eventStore.sendChannel.send(res.value)
            }
            msg.response.complete(res)
        }
    }

    private fun processPoly(c: Command): EsScope {

        println("Processing $c")

        return when (c) {
            is StartSkullKing -> execute(c)
            is AnnounceWinningCardsFoldCount -> execute(c)
            is PlayCard -> execute(c)
        }
    }

    fun handle(cmd: Command): CompletableDeferred<CmdResult> =
        runBlocking {
            //use launch to execute commands in parallel slightly out of order
            CommandMsg(cmd, CompletableDeferred()).let {
                sendChannel.send(it)
                it.response
            }
        }

}


private fun List<SkullKingEvent>.fold(): SkullKing {
    return this.fold(emptySkullKing) { i: SkullKing, e: SkullKingEvent -> i.compose(e) }
}


private fun execute(c: StartSkullKing): EsScope = lambda@{
    if (c.players.size !in MIN_PLAYERS..MAX_PLAYERS)
        return@lambda Invalid(
            SkullKingConfigurationError("SkullKing game must be played at least with 2 or at most with 5 people! $c", c)
        )

    val game = getEvents<SkullKingEvent>(c.gameId).fold()
    if (game == emptySkullKing) {
        val playersOrdered = game.distributeCards(c.players, FIRST_ROUND_NB, c.gameId)

        Valid(listOf(Started(c.gameId, playersOrdered)))
    } else
        Invalid(SkullKingError("SkullKing game already existing! $game", game))
}

private fun execute(c: AnnounceWinningCardsFoldCount): EsScope = {

    when (val game = getEvents<SkullKingEvent>(c.gameId).fold()) {
        is emptySkullKing -> Invalid(SkullKingNotStartedError("Game ${c.gameId} not STARTED !", c))
        is NewRound -> when {
            game.hasAlreadyAnnounced(c.playerId) -> Invalid(
                PlayerAlreadyAnnouncedError("Player ${c.playerId} already announced", c)
            )
            game.has(c.playerId) -> Valid(listOf(PlayerAnnounced(c.gameId, c.playerId, c.count, game.roundNb)))
            else -> Invalid(PlayerNotInGameError("Player ${c.playerId} not in game", c))
        }
        is ReadySkullKing -> Invalid(SkullKingAlreadyReadyError("Game ${c.gameId} already ready !", c))
    }
}

private fun execute(c: PlayCard): EsScope = {
    val game = getEvents<SkullKingEvent>(c.gameId).fold()
    (c.card as? ColoredCard)?.let {
        if (it.color == CardColor.BLUE && it.value == 2) {
            print("stop")
        }
    }
    when (game) {
        is NewRound -> Invalid(
            SkullKingNotReadyError(
                "All players must announce before starting to play cards",
                c
            )
        )
        is ReadySkullKing -> when {
            !game.has(c.playerId) -> Invalid(
                PlayerNotInGameError(
                    "Player ${c.playerId} not in game",
                    c
                )
            )
            game.doesPlayerHaveCard(c.playerId, c.card) -> {
                val cardPlayed = CardPlayed(
                    game.id,
                    c.playerId,
                    c.card
                )

                when {
                    game.isCardPlayNotAllowed(c.playerId, c.card) -> Invalid(CardNotAllowedError(c))
                    game.isLastFoldPlay() -> {
                        val events = listOf(
                            cardPlayed,
                            FoldWinnerSettled(game.id, settleFoldWinner(game.currentFold))
                        )

                        when {
                            game.isLastFoldOfRound() -> {
                                val nextRoundNb = game.roundNb + 1
                                Valid(
                                    events + NewRoundStarted(
                                        game.id,
                                        nextRoundNb,
                                        game.distributeCards(
                                            game.players.map { it.id },
                                            foldCount = nextRoundNb,
                                            firstPlayerIndex = NEXT_FIRST_PLAYER_INDEX
                                        )
                                    )
                                )
                            }
                            else -> Valid(events)
                        }
                    }
                    else -> Valid(listOf(cardPlayed))
                }
            }
            else -> Invalid(
                PlayerDoNotHaveCardError(
                    "Player ${c.playerId} do not have card ${c.card}",
                    c
                )
            )
        }
        else -> Invalid(SkullKingNotStartedError("Game ${c.gameId} not STARTED !", c))
    }
}

// MOCK first fold player always wins
private fun settleFoldWinner(fold: Map<PlayerId, Card>): PlayerId {
    return fold.keys.first()
}

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
import org.skull.king.eventStore.GameFinished
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
            SkullKingConfigurationError("SkullKing game must be played at least with 2 or at most with 6 people! $c", c)
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
        is skullKingOver -> Invalid(SkullKingOverError(c))
    }
}

private fun execute(c: PlayCard): EsScope = {
    when (val game = getEvents<SkullKingEvent>(c.gameId).fold()) {
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
                            game.isNextFoldLastFoldOfRound() -> {
                                val nextRoundNb = game.roundNb + 1

                                when {
                                    game.isOver() -> Valid(events + GameFinished(game.id))
                                    else -> Valid(
                                        events + NewRoundStarted(
                                            game.id,
                                            nextRoundNb,
                                            game.distributeCards(
                                                game.players.map { it.id },
                                                nextRoundNb,
                                                firstPlayerIndex = NEXT_FIRST_PLAYER_INDEX
                                            )
                                        )
                                    )
                                }
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
        emptySkullKing -> Invalid(SkullKingNotStartedError("Game ${c.gameId} not STARTED !", c))
        skullKingOver -> Invalid(SkullKingOverError(c))
    }
}

typealias Fold = List<PlayerCard>

data class PlayerCard(val playerId: PlayerId, val card: Card)

fun settleFoldWinner(foldPlayed: Map<PlayerId, Card>): PlayerId {
    val fold = foldPlayed.map { PlayerCard(it.key, it.value) }

    val skullKing = fold.skullKing()
    val pirates = fold.pirates()
    val mermaids = fold.mermaids()
    val highestBlackCard = fold.highestOf(CardColor.BLACK)

    val winningCard: PlayerCard = when {
        skullKing.isNotEmpty() -> when {
            mermaids.isNotEmpty() -> mermaids.first()
            else -> skullKing.first()
        }
        pirates.isNotEmpty() -> pirates.first()
        mermaids.isNotEmpty() -> mermaids.first()
        highestBlackCard != null -> highestBlackCard
        fold.onlyEscapes() -> fold.first()
        else -> requireNotNull(
            fold.highestOf(requireNotNull(fold.colorAsked()))
        )
    }

    return winningCard.playerId
}

private fun Fold.skullKing() = filter { it.card is SpecialCard && it.card.type == SpecialCardType.SKULL_KING }
private fun Fold.mermaids() = filter { it.card is SpecialCard && it.card.type == SpecialCardType.MERMAID }
private fun Fold.pirates() = filter {
    (it.card is SpecialCard && it.card.type == SpecialCardType.PIRATE)
            || (it.card is ScaryMary && it.card.usage == ScaryMaryUsage.PIRATE)
}

private fun Fold.onlyEscapes() = all {
    (it.card is SpecialCard && it.card.type == SpecialCardType.ESCAPE)
            || (it.card is ScaryMary && it.card.usage == ScaryMaryUsage.ESCAPE)
}

private fun Fold.highestOf(colorAsked: CardColor) =
    filter { it.card is ColoredCard && it.card.color == colorAsked }.maxBy { (it.card as ColoredCard).value }

private fun Fold.colorAsked() =
    firstOrNull { it.card is ColoredCard }?.let { (_, card) -> (card as ColoredCard).color }

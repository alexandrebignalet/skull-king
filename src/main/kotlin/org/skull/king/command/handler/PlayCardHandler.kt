package org.skull.king.command.handler

import org.skull.king.command.PlayCard
import org.skull.king.command.domain.NewRound
import org.skull.king.command.domain.ReadySkullKing
import org.skull.king.command.domain.ScaryMary
import org.skull.king.command.domain.ScaryMaryUsage
import org.skull.king.command.domain.emptySkullKing
import org.skull.king.command.domain.skullKingOver
import org.skull.king.command.error.CardNotAllowedError
import org.skull.king.command.error.NotYourTurnError
import org.skull.king.command.error.PlayerDoNotHaveCardError
import org.skull.king.command.error.PlayerNotInGameError
import org.skull.king.command.error.ScaryMaryUsageError
import org.skull.king.command.error.SkullKingNotReadyError
import org.skull.king.command.error.SkullKingNotStartedError
import org.skull.king.command.error.SkullKingOverError
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.event.CardPlayed
import org.skull.king.repository.SkullkingEventSourcedRepositoryInMemory

class PlayCardHandler(private val repository: SkullkingEventSourcedRepositoryInMemory) :
    CommandHandler<PlayCard, String> {

    override fun execute(command: PlayCard): Pair<String, Sequence<Event>> =
        when (val game = repository[command.gameId]) {
            is NewRound -> throw SkullKingNotReadyError(
                "All players must announce before starting to play cards",
                command
            )
            is ReadySkullKing -> when {
                !game.has(command.playerId) -> throw
                PlayerNotInGameError(
                    "Player ${command.playerId} not in game",
                    command
                )
                !game.isPlayerTurn(command.playerId) -> throw NotYourTurnError(command)
                game.doesPlayerHaveCard(command.playerId, command.card) -> {
                    val cardPlayed = CardPlayed(
                        game.getId(),
                        command.playerId,
                        command.card,
                        game.isLastFoldPlay()
                    )

                    when {
                        command.card is ScaryMary && command.card.usage == ScaryMaryUsage.NOT_SET -> throw ScaryMaryUsageError(
                            command
                        )
                        game.isCardPlayNotAllowed(command.playerId, command.card) -> throw CardNotAllowedError(command)
                        else -> Pair(game.getId(), sequenceOf(cardPlayed))
                    }
                }
                else -> throw
                PlayerDoNotHaveCardError(
                    "Player ${command.playerId} do not have card ${command.card}",
                    command
                )
            }
            emptySkullKing -> throw SkullKingNotStartedError("Game ${command.gameId} not STARTED !", command)
            skullKingOver -> throw SkullKingOverError(command)
        }
}

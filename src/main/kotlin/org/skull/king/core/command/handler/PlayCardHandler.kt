package org.skull.king.core.command.handler

import org.skull.king.core.command.PlayCard
import org.skull.king.core.command.domain.NewRound
import org.skull.king.core.command.domain.ReadySkullKing
import org.skull.king.core.command.domain.ScaryMary
import org.skull.king.core.command.domain.ScaryMaryUsage
import org.skull.king.core.command.domain.emptySkullKing
import org.skull.king.core.command.domain.skullKingOver
import org.skull.king.core.command.error.CardNotAllowedError
import org.skull.king.core.command.error.NotYourTurnError
import org.skull.king.core.command.error.PlayerDoNotHaveCardError
import org.skull.king.core.command.error.PlayerNotInGameError
import org.skull.king.core.command.error.ScaryMaryUsageError
import org.skull.king.core.command.error.SkullKingNotReadyError
import org.skull.king.core.command.error.SkullKingNotStartedError
import org.skull.king.core.command.error.SkullKingOverError
import org.skull.king.core.event.CardPlayed
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepositoryInMemory

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

package org.skull.king.domain.core.command.handler

import org.skull.king.domain.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.domain.core.command.domain.NewRound
import org.skull.king.domain.core.command.domain.ReadySkullKing
import org.skull.king.domain.core.command.domain.emptySkullKing
import org.skull.king.domain.core.command.domain.skullKingOver
import org.skull.king.domain.core.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.domain.core.command.error.PlayerNotInGameError
import org.skull.king.domain.core.command.error.SkullKingNotStartedError
import org.skull.king.domain.core.command.error.SkullKingOverError
import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.infrastructure.cqrs.command.CommandHandler
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository

class AnnounceHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<AnnounceWinningCardsFoldCount, String> {

    override fun execute(command: AnnounceWinningCardsFoldCount): Pair<String, Sequence<Event>> {
        return when (val game = repository[command.gameId]) {
            is emptySkullKing -> throw SkullKingNotStartedError("Game ${command.gameId} not STARTED !", command)
            is NewRound -> when {
                game.hasAlreadyAnnounced(command.playerId) -> throw PlayerAlreadyAnnouncedError(
                    "Player ${command.playerId} already announced",
                    command
                )
                game.has(command.playerId) -> Pair(
                    game.getId(),
                    sequenceOf(
                        PlayerAnnounced(
                            command.gameId,
                            command.playerId,
                            command.count,
                            game.roundNb,
                            game.isMissingOneLastAnnounce()
                        )
                    )
                )
                else -> throw PlayerNotInGameError("Player ${command.playerId} not in game", command)
            }
            is ReadySkullKing -> throw PlayerAlreadyAnnouncedError(
                "Player ${command.playerId} already announced",
                command
            )
            is skullKingOver -> throw SkullKingOverError(command)
        }
    }
}

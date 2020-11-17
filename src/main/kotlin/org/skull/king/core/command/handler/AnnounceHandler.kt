package org.skull.king.core.command.handler

import org.skull.king.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.core.command.domain.NewRound
import org.skull.king.core.command.domain.ReadySkullKing
import org.skull.king.core.command.domain.emptySkullKing
import org.skull.king.core.command.domain.skullKingOver
import org.skull.king.core.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.core.command.error.PlayerNotInGameError
import org.skull.king.core.command.error.SkullKingAlreadyReadyError
import org.skull.king.core.command.error.SkullKingNotStartedError
import org.skull.king.core.command.error.SkullKingOverError
import org.skull.king.core.event.PlayerAnnounced
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepositoryInMemory

class AnnounceHandler(private val repository: SkullkingEventSourcedRepositoryInMemory) :
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
                    sequenceOf(PlayerAnnounced(command.gameId, command.playerId, command.count, game.roundNb))
                )
                else -> throw PlayerNotInGameError("Player ${command.playerId} not in game", command)
            }
            is ReadySkullKing -> throw SkullKingAlreadyReadyError("Game ${command.gameId} already ready !", command)
            is skullKingOver -> throw SkullKingOverError(command)
        }
    }
}

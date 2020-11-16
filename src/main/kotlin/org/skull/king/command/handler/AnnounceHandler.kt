package org.skull.king.command.handler

import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.domain.NewRound
import org.skull.king.command.domain.ReadySkullKing
import org.skull.king.command.domain.emptySkullKing
import org.skull.king.command.domain.skullKingOver
import org.skull.king.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.command.error.PlayerNotInGameError
import org.skull.king.command.error.SkullKingAlreadyReadyError
import org.skull.king.command.error.SkullKingNotStartedError
import org.skull.king.command.error.SkullKingOverError
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.event.PlayerAnnounced
import org.skull.king.repository.SkullkingEventSourcedRepositoryInMemory

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

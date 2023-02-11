package org.skull.king.domain.core.command.handler

import org.skull.king.domain.core.command.domain.state.AnnounceState
import org.skull.king.domain.core.command.domain.state.OverState
import org.skull.king.domain.core.command.domain.state.RoundState
import org.skull.king.domain.core.command.domain.state.StartState
import org.skull.king.domain.core.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.domain.core.command.error.PlayerNotInGameError
import org.skull.king.domain.core.command.error.SkullKingNotStartedError
import org.skull.king.domain.core.command.error.SkullKingOverError
import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.framework.command.Command
import org.skull.king.infrastructure.framework.command.CommandHandler
import org.skull.king.infrastructure.framework.ddd.event.Event

data class AnnounceWinningCardsFoldCount(val gameId: String, val playerId: String, val count: Int) : Command<String>

class AnnounceHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<AnnounceWinningCardsFoldCount, String> {

    override fun execute(command: AnnounceWinningCardsFoldCount): Pair<String, Sequence<Event>> {
        return when (val game = repository[command.gameId]) {
            is StartState -> throw SkullKingNotStartedError(game)
            is AnnounceState -> when {
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
                            game.isMissingOneLastAnnounce(),
                            game.version
                        )
                    )
                )

                else -> throw PlayerNotInGameError(command.playerId, game)
            }

            is RoundState -> throw PlayerAlreadyAnnouncedError(
                "Player ${command.playerId} already announced",
                command
            )

            is OverState -> throw SkullKingOverError(game)
        }
    }
}

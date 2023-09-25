package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.command.Command
import org.skull.king.application.infrastructure.framework.command.CommandHandler
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import org.skull.king.core.domain.*
import org.skull.king.core.infrastructure.SkullkingEventSourcedRepository

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

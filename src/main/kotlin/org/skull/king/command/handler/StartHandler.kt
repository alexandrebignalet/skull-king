package org.skull.king.command.handler

import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.SkullKing
import org.skull.king.command.domain.emptySkullKing
import org.skull.king.command.error.SkullKingConfigurationError
import org.skull.king.command.error.SkullKingError
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.event.Started
import org.skull.king.repository.SkullkingEventSourcedRepositoryInMemory

class StartHandler(private val repository: SkullkingEventSourcedRepositoryInMemory) :
    CommandHandler<StartSkullKing, String> {

    override fun execute(command: StartSkullKing): Pair<String, Sequence<Event>> {
        if (command.players.size !in SkullKing.MIN_PLAYERS..SkullKing.MAX_PLAYERS)
            throw SkullKingConfigurationError(
                "SkullKing game must be played at least with 2 or at most with 6 people! $command",
                command
            )

        val game = repository[command.gameId]
        return if (game == emptySkullKing) {
            val playersOrdered = game.distributeCards(command.players, SkullKing.FIRST_ROUND_NB, command.gameId)

            Pair(game.getId(), sequenceOf(Started(command.gameId, playersOrdered)))
        } else throw SkullKingError("SkullKing game already existing! $game", game)
    }
}

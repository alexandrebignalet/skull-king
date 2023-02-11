package org.skull.king.domain.core.command.handler

import org.skull.king.domain.core.command.domain.Card
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.framework.command.Command
import org.skull.king.infrastructure.framework.command.CommandHandler
import org.skull.king.infrastructure.framework.ddd.event.Event

data class PlayCard(val gameId: String, val playerId: String, val card: Card) : Command<String>

class PlayCardHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<PlayCard, String> {

    override fun execute(command: PlayCard): Pair<String, Sequence<Event>> =
        repository[command.gameId]
            .playCard(command.playerId, command.card)
            .let {
                Pair(command.gameId, sequenceOf(it))
            }
}

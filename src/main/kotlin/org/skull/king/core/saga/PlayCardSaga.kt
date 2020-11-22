package org.skull.king.core.saga

import org.skull.king.core.command.domain.Card
import org.skull.king.cqrs.saga.Saga

data class PlayCardSaga(
    val gameId: String,
    val playerId: String,
    val card: Card
) : Saga<String>
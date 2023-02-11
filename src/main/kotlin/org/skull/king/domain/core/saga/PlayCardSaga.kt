package org.skull.king.domain.core.saga

import org.skull.king.domain.core.command.domain.Card
import org.skull.king.infrastructure.framework.saga.Saga

data class PlayCardSaga(
    val gameId: String,
    val playerId: String,
    val card: Card
) : Saga<String>

package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.saga.Saga
import org.skull.king.core.domain.Card

data class PlayCardSaga(
    val gameId: String,
    val playerId: String,
    val card: Card
) : Saga<String>

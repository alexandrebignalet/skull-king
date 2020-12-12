package org.skull.king.domain.core.saga

import org.skull.king.infrastructure.cqrs.saga.Saga

data class AnnounceWinningCardsFoldCountSaga(val gameId: String, val playerId: String, val count: Int) : Saga<String>

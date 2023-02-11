package org.skull.king.domain.core.saga

import org.skull.king.infrastructure.framework.saga.Saga

data class AnnounceWinningCardsFoldCountSaga(val gameId: String, val playerId: String, val count: Int) : Saga<String>

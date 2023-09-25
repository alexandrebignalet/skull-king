package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.saga.Saga

data class AnnounceWinningCardsFoldCountSaga(val gameId: String, val playerId: String, val count: Int) : Saga<String>

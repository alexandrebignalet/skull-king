package org.skull.king.infrastructure.saga

import org.skull.king.domain.core.saga.AnnounceWinningCardsFoldCountSagaHandler
import org.skull.king.infrastructure.cqrs.saga.Saga
import org.skull.king.infrastructure.cqrs.saga.SagaHandler
import org.skull.king.infrastructure.event.ConcurrentEventsException
import org.slf4j.LoggerFactory

abstract class RetryableSagaHandler<TResult, TSaga : Saga<TResult>> : SagaHandler<TResult, TSaga> {

    companion object {
        val LOGGER = LoggerFactory.getLogger(AnnounceWinningCardsFoldCountSagaHandler::class.java)
        private const val timeout: Long = 10
        private const val maxRetries: Long = 10
    }

    protected fun <T> exponentialBackoff(retry: Int = 1, block: () -> T): T {
        return try {
            block()
        } catch (exception: ConcurrentEventsException) {
            if (retry > maxRetries) throw exception

            LOGGER.warn("retrying $retry/$maxRetries; $timeout")
            Thread.sleep(timeout)
            exponentialBackoff(retry + 1, block)
        }
    }
}

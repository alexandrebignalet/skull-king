package org.skull.king.application.infrastructure.saga

import org.skull.king.application.infrastructure.event.ConcurrentEventsException
import org.skull.king.application.infrastructure.framework.saga.Saga
import org.skull.king.application.infrastructure.framework.saga.SagaHandler
import org.skull.king.core.usecases.AnnounceWinningCardsFoldCountSagaHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class RetryableSagaHandler<TResult, TSaga : Saga<TResult>> : SagaHandler<TResult, TSaga> {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AnnounceWinningCardsFoldCountSagaHandler::class.java)
        private const val timeout: Long = 10
        private const val maxRetries: Long = 10
    }

    protected fun <T> exponentialBackoff(retry: Int = 1, block: () -> T): T {
        return try {
            block()
        } catch (exception: ConcurrentEventsException) {
            if (retry > maxRetries) throw exception

            LOGGER.error("retrying $retry/$maxRetries; $timeout")
            Thread.sleep(timeout)
            exponentialBackoff(retry + 1, block)
        }
    }
}

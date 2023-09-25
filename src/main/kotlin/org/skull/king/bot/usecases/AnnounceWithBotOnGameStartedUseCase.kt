package org.skull.king.bot.usecases

import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.core.domain.Started


class AnnounceWithBotOnGameStartedUseCase(
) : EventCaptor<Started> {
    override fun execute(event: Started) {
    }


}
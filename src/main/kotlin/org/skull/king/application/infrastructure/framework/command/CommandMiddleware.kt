package org.skull.king.application.infrastructure.framework.command

import org.skull.king.application.infrastructure.framework.ddd.event.Event
import java.util.function.Supplier


interface CommandMiddleware {
    fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ): Pair<T, Sequence<Event>>
}

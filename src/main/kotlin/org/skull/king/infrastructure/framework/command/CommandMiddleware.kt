package org.skull.king.infrastructure.framework.command

import java.util.function.Supplier
import org.skull.king.infrastructure.framework.ddd.event.Event


interface CommandMiddleware {
    fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ): Pair<T, Sequence<Event>>
}

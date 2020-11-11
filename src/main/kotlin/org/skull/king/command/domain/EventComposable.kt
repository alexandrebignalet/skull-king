package org.skull.king.command.domain

import org.skull.king.event.Event

interface EventComposable<T : Event> {
    fun compose(e: T): EventComposable<T>
}

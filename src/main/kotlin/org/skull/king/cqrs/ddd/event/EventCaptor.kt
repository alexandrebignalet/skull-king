package org.skull.king.cqrs.ddd.event

import com.google.common.reflect.TypeToken

interface EventCaptor<TEvent : Event> {
    fun execute(event: TEvent)

    fun eventType(): Class<TEvent> = TypeToken.of(this::class.java)
        .resolveType(EventCaptor::class.java.typeParameters[0]).rawType as Class<TEvent>
}

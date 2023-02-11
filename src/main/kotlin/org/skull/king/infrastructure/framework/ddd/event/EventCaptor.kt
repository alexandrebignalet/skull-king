package org.skull.king.infrastructure.framework.ddd.event

import com.google.common.reflect.TypeToken

interface EventCaptor<TEvent : Event> {
    fun execute(event: TEvent)

    @Suppress("UNCHECKED_CAST")
    fun eventType(): Class<TEvent> = TypeToken.of(this::class.java)
        .resolveType(EventCaptor::class.java.typeParameters[0]).rawType as Class<TEvent>
}

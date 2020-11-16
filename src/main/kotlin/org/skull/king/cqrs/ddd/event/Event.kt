package org.skull.king.cqrs.ddd.event

import java.time.Instant

abstract class Event {
    val timestamp: Long = Instant.now().toEpochMilli()

    abstract fun targetId(): Any

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * timestamp.hashCode()
    }
}

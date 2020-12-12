package org.skull.king.infrastructure.event

class ConcurrentEventsException(aggregateId: String, throwable: Throwable) :
    RuntimeException("Refuse saving concurrent events on $aggregateId", throwable)

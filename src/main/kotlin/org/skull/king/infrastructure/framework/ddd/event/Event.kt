package org.skull.king.infrastructure.framework.ddd.event

interface Event {
    val version: Int
    val timestamp: Long
    val aggregateId: String
    val aggregateType: String
    val type: String
}

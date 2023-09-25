package org.skull.king.application.infrastructure.framework.ddd.event

interface Event {
    val version: Int
    val timestamp: Long
    val aggregateId: String
    val aggregateType: String
    val type: String
}

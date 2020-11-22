package org.skull.king.cqrs.ddd.event

interface Event {
    val version: Int
    val timestamp: Long
    val aggregateId: String
    val aggregateType: String
    val type: String
}

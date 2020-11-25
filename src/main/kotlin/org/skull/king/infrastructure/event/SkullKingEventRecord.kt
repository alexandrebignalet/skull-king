package org.skull.king.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.infrastructure.cqrs.ddd.event.Event

@JsonDeserialize(using = SkullKingEventRecordJsonDeserializer::class)
data class SkullKingEventRecord(
    override val aggregateId: String,
    override val aggregateType: String,
    override val type: String,
    override val version: Int,
    override val timestamp: Long,
    val data: SkullKingEvent
) : Event {
    companion object {
        fun of(event: Event) = SkullKingEventRecord(
            event.aggregateId,
            event.aggregateType,
            event.type,
            event.version,
            event.timestamp,
            event as SkullKingEvent
        )

        fun buildPersistenceId(id: String) = "${SkullKingEvent.SKULLKING_AGGREGATE_TYPE}!_!$id"
    }

    fun persistenceId() = buildPersistenceId(aggregateId)

    private val _data get() :SkullKingEvent = data

    fun fireMap(objectMapper: ObjectMapper) = mapOf(
        "aggregate_id" to aggregateId,
        "aggregate_type" to aggregateType,
        "event_type" to type,
        "version" to version,
        "timestamp" to timestamp,
        "data" to objectMapper.writeValueAsString(data)
    )
}

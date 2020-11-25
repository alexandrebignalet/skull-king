package org.skull.king.infrastructure.event

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.utils.JsonObjectMapper

class SkullKingEventRecordJsonDeserializer : JsonDeserializer<SkullKingEventRecord>() {
    companion object {
        private val objectMapper = JsonObjectMapper.getObjectMapper()
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): SkullKingEventRecord {
        val oc = p.codec
        val node: JsonNode = oc.readTree(p)

        val aggregateId = node.get("aggregate_id").asText()
        val aggregateType = node.get("aggregate_type").asText()
        val type = node.get("event_type").asText()
        val version = node.get("version").asInt()
        val timestamp = node.get("timestamp").asLong()
        val data = objectMapper.readValue<SkullKingEvent>(node.get("data").asText())

        return SkullKingEventRecord(aggregateId, aggregateType, type, version, timestamp, data)
    }
}

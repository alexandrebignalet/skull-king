package org.skull.king.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.skull.king.domain.core.query.Play

class FoldDeserializer : JsonDeserializer<List<Play>>() {
    companion object {
        private val objectMapper = JsonObjectMapper.getObjectMapper()
    }

    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): List<Play> {
        val node: JsonNode = jp.codec.readTree(jp)
        return objectMapper.readValue<Map<String, Play>>(node.toString()).values.toList()
    }

}

package org.skull.king.application.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.skull.king.core.domain.ReadCard

class CardsDeserializer : JsonDeserializer<List<ReadCard>>() {
    companion object {
        private val objectMapper = JsonObjectMapper.getObjectMapper()
    }

    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): List<ReadCard> {
        val node: JsonNode = jp.codec.readTree(jp)
        return objectMapper.readValue<Map<String, ReadCard>>(node.toString()).values.toList()
    }

}

package org.skull.king.application.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JsonObjectMapper {
    fun getObjectMapper(objectMapper: ObjectMapper? = null) = configure(objectMapper ?: ObjectMapper())

    private fun configure(objectMapper: ObjectMapper): ObjectMapper {
        return objectMapper
            .registerKotlinModule()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }
}

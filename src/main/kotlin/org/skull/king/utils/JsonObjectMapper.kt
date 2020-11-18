package org.skull.king.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JsonObjectMapper {
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    fun getObjectMapper() = objectMapper
}

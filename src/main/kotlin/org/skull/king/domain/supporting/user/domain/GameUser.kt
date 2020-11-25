package org.skull.king.domain.supporting.user.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.skull.king.utils.FirebaseSetDeserializer

data class GameUser(
    val id: String,
    @JsonDeserialize(using = FirebaseSetDeserializer::class)
    val rooms: Set<String> = setOf()
)

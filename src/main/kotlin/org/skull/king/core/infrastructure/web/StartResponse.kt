package org.skull.king.core.infrastructure.web

import com.fasterxml.jackson.annotation.JsonProperty

data class StartResponse(@JsonProperty("game_id") val gameId: String)
